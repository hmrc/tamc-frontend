/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import com.google.inject.Inject
import connectors.MarriageAllowanceConnector
import errors.ErrorResponseStatus._
import errors._
import events._
import models._
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.SystemTaxYear
import views.helpers.LanguageUtilsImpl

import scala.concurrent.{ExecutionContext, Future}


class TransferService @Inject()(
  marriageAllowanceConnector: MarriageAllowanceConnector,
  auditConnector: AuditConnector,
  cachingService: CachingService,
  applicationService: ApplicationService,
  timeService: TimeService,
  languageUtilsImpl: LanguageUtilsImpl,
  taxYear: SystemTaxYear
) extends Logging {

  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      auditConnector.sendEvent(event)
    }

  def isRecipientEligible(transferorNino: Nino, recipientData: RegistrationFormInput)
                         (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkRecipientEligible(transferorNino, recipientData).map(eligible => eligible) recoverWith {
      case error =>
        handleAudit(RecipientFailureEvent(transferorNino, error))
        Future.failed(error)
    }

  private def checkRecipientEligible(transferorNino: Nino, recipientData: RegistrationFormInput)
                                    (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    for {
      cache <- cachingService.getCachedDataForEligibilityCheck
      _ <- validateTransferorAgainstRecipient(recipientData, cache)
      (recipientRecord, taxYears) <- getRecipientRelationship(transferorNino, recipientData)
      validYears = timeService.getValidYearsApplyMAPreviousYears(taxYears)
      _ <- cachingService.saveRecipientRecord(recipientRecord, recipientData, validYears)
    } yield true

  private def validateTransferorAgainstRecipient(recipientData: RegistrationFormInput, cache: Option[EligibilityCheckCacheData])
  : Future[Option[EligibilityCheckCacheData]] =
    (recipientData, cache) match {
      case (RegistrationFormInput(_, _, _, _, dom), Some(EligibilityCheckCacheData(_, _, activeRelationshipRecord, historicRelationships, _, _, _)))
        if applicationService.canApplyForMarriageAllowance(historicRelationships, activeRelationshipRecord, timeService.getTaxYearForDate(dom)) =>
        Future.successful(cache)
      case (_, Some(_)) => throw NoTaxYearsForTransferor()
      case _ => throw CacheMissingTransferor()
    }

  def createRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[NotificationRecord] = {
    doCreateRelationship(transferorNino)(hc, messages, ec) recover {
      case error =>
        handleAudit(CreateRelationshipCacheFailureEvent(error))
        throw error
    }
  }

  def getFinishedData(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    for {
      cacheData <- cachingService.getCachedData
      notification <- validateFinishedData(cacheData)
    } yield notification

  private def validateFinishedData(cacheData: Option[CacheData])(implicit ec: ExecutionContext): Future[NotificationRecord] =
    Future {
      cacheData match {
        case Some(CacheData(_, _, Some(notification), Some(true), _, _, _)) => notification
        case _ => throw CacheCreateRequestNotSent()
      }
    }

  private def doCreateRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[NotificationRecord] = {
    for {
      cacheData <- cachingService.getCachedData
      validated <- validateCompleteCache(cacheData)
      postCreateData <- sendCreateRelationship(transferorNino, validated)(hc, messages, ec)
      _ <- lockCreateRelationship()
      _ <- auditCreateRelationship(postCreateData)
    } yield validated.notification.get
  }

  private def lockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    logger.info("lockCreateRelationship has been called.")
    cachingService.lockCreateRelationship()
  }


  def getRecipientDetailsFormData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RecipientDetailsFormInput] =
    for {
      cacheData <- cachingService.getCachedData
      registrationData <- validateRegistrationData(cacheData)
    } yield registrationData

  private def validateRegistrationData(cacheData: Option[CacheData])(implicit ec: ExecutionContext): Future[RecipientDetailsFormInput] =
    Future {
      cacheData match {
        case Some(CacheData(_, _, _, _, _, Some(registrationData), _)) => registrationData
        case Some(CacheData(_, None, _, _, _, _, _)) => throw CacheMissingRecipient()
        case _ => throw BadFetchRequest()
      }
    }

  private def auditCreateRelationship(cacheData: CacheData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    handleAudit(CreateRelationshipSuccessEvent(cacheData))
  }

  def upsertTransferorNotification(notificationRecord: NotificationRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] = {
    logger.info("upsertTransferorNotification has been called.")
    cachingService.saveNotificationRecord(notificationRecord)
  }

  def getConfirmationData(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationModel] = {
    logger.info("getConfirmationData has been called.")
    for {
      cache <- cachingService.getCachedData(nino)
      validated <- validateCompleteCache(cache)
      confirmData <- transformCache(validated)
    } yield confirmData
  }

  private def validateCompleteCache(cacheData: Option[CacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheData] = {
    logger.info("validateCompleteCache has been called.")
    cacheData match {
      case Some(CacheData(_, _, _, Some(true), _, _, _)) => {
        handleAudit(RelationshipAlreadyCreatedEvent(cacheData.get))
        throw CacheRelationshipAlreadyCreated()
      }
      case Some(
      CacheData(
      Some(_),
      Some(RecipientRecord(UserRecord(_, _, _, _), _, _)),
      Some(notification: NotificationRecord),
      _,
      Some(selectedTaxYears), _, _)) if (selectedTaxYears.size > 0) => Future.successful(cacheData.get)
      case None => throw CacheMissingTransferor()
      case Some(CacheData(None, _, _, _, _, _, _)) => throw CacheMissingTransferor()
      case Some(CacheData(_, None, _, _, _, _, _)) => throw CacheMissingRecipient()
      case Some(CacheData(_, _, None, _, _, _, _)) => throw CacheMissingEmail()
      case Some(CacheData(_, _, _, _, None, _, _)) => throw NoTaxYearsSelected()
      case Some(CacheData(_, _, _, _, Some(selectedTaxYears), _, _)) if (selectedTaxYears.isEmpty) => throw NoTaxYearsSelected()
      case _ => throw BadFetchRequest()
    }
  }

  private def transformCache(cacheData: CacheData)(implicit ec: ExecutionContext): Future[ConfirmationModel] =
    Future {
      ConfirmationModel(
        transferorFullName = cacheData.transferor.flatMap(_.name),
        transferorEmail = cacheData.notification.get.transferor_email,
        recipientFirstName = cacheData.recipient.get.data.name,
        recipientLastName = cacheData.recipient.get.data.lastName,
        recipientNino = cacheData.recipient.get.data.nino,
        availableYears = getAvailableTaxYears(cacheData.selectedYears.get),
        dateOfMarriage = cacheData.dateOfMarriage.get)
    }

  private def getAvailableTaxYears(selectedYears: List[Int]): List[TaxYear] =
    selectedYears.map { x =>
      TaxYear(x, isCurrentTaxYear(x))
    }.sortWith((m1, m2) => m1.year > m2.year)

  private def isCurrentTaxYear(year: Int): Option[Boolean] =
    if (taxYear.current().startYear == year)
      Some(true)
    else
      None

  private def getRecipientRelationship(transferorNino: Nino, recipientData: RegistrationFormInput)
                                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(UserRecord, Option[List[TaxYear]])] =
    marriageAllowanceConnector
      .getRecipientRelationship(transferorNino, recipientData)
      .flatMap {
        case Right(getRelationshipResponse) =>
          getRelationshipResponse match {
            case GetRelationshipResponse(Some(recipientRecord), availableYears, ResponseStatus("OK")) =>
              Future.successful((recipientRecord, availableYears))
            case _ => Future.failed(RecipientNotFound())
          }
        case Left(error) =>
          error.status.status_code match {
            case TRANSFEROR_DECEASED => Future.failed(TransferorDeceased())
            case RECIPIENT_NOT_FOUND => Future.failed(RecipientNotFound())
            case _ => Future.failed(OtherError(error))
          }
      }

  def deleteSelectionAndGetCurrentAndPreviousYearsEligibility(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CurrentAndPreviousYearsEligibility] =
    for {
      _ <- cachingService.saveSelectedYears(List[Int]())
      res <- getCurrentAndPreviousYearsEligibility
    } yield res

  def getCurrentAndPreviousYearsEligibility(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CurrentAndPreviousYearsEligibility] =
    cachingService.getRecipientRecord map {
      _.fold(throw CacheMissingRecipient())(CurrentAndPreviousYearsEligibility(_, taxYear))
    }

  private def transform(sessionData: CacheData, messages: Messages): CreateRelationshipRequestHolder = {
    val transferor: UserRecord = sessionData.transferor.get
    val recipient = sessionData.recipient.get.record
    val email = sessionData.notification.get.transferor_email
    val createRelationshipreq = CreateRelationshipRequest(
      transferor_cid = transferor.cid,
      transferor_timestamp = transferor.timestamp,
      recipient_cid = recipient.cid,
      recipient_timestamp = recipient.timestamp,
      taxYears = sessionData.selectedYears.get.sortWith(_ < _))
    val sendNotificationData = CreateRelationshipNotificationRequest(full_name = "UNKNOWN", email = email, welsh = languageUtilsImpl.isWelsh(messages))
    CreateRelationshipRequestHolder(request = createRelationshipreq, notification = sendNotificationData)
  }

  private def sendCreateRelationship(transferorNino: Nino, data: CacheData)(implicit hc: HeaderCarrier,
                                                                            messages: Messages, ec: ExecutionContext): Future[CacheData] = {

    marriageAllowanceConnector.createRelationship(transferorNino, transform(data, messages)) map {
      case Right(createRelationshipResponse) =>
        createRelationshipResponse match {
          case Some(CreateRelationshipResponse(ResponseStatus("OK"))) => data
          case _ => throw new UnsupportedOperationException("Unable to send create relationship request")
        }
      case Left(error) =>
        error.status.status_code match {
          case CANNOT_CREATE_RELATIONSHIP => throw CannotCreateRelationship()
          case RELATION_MIGHT_BE_CREATED => throw RelationshipMightBeCreated()
          case RECIPIENT_DECEASED => throw RecipientDeceased()
          case _ => throw new UnsupportedOperationException("Unable to send create relationship request")
        }
    } recover {
      case error =>
        handleAudit(CreateRelationshipFailureEvent(data, error))
        throw error
    }
  }

  def saveSelectedYears(selectedYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    cachingService.saveSelectedYears(selectedYears)

  def updateSelectedYears(availableTaxYears: List[TaxYear], extraYear: Int, yearAvailableForSelection: Option[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] = {
    updateSelectedYears(availableTaxYears, List(extraYear).filter(_ > 0), yearAvailableForSelection)
  }

  def updateSelectedYears(availableTaxYears: List[TaxYear], extraYears: List[Int], yearAvailableForSelection: Option[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    for {
      cache <- cachingService.getCachedData
      updatedYears <- updateSelectedYears(cache.get.selectedYears, extraYears)
      validatedSelectedYears <- validateSelectedYears(availableTaxYears, updatedYears, yearAvailableForSelection)
      savedYears <- cachingService.saveSelectedYears(validatedSelectedYears)
    } yield savedYears

  def validateSelectedYears(availableTaxYears: List[TaxYear], selectedYears: List[Int], yearAvailableForSelection: Option[Int])(implicit ec: ExecutionContext): Future[List[Int]] =
    if (selectedYears.isEmpty && yearAvailableForSelection.isDefined && availableTaxYears.nonEmpty && yearAvailableForSelection.get == availableTaxYears.last.year) {
      throw new NoTaxYearsSelected
    } else if (selectedYears.isEmpty) {
      Future {
        selectedYears
      }
    }

    else if (selectedYears.forall {
      availableTaxYears.map(_.year).contains(_)
    }) {
      Future {
        selectedYears
      }
    } else {
      Future.failed(new IllegalArgumentException(s"$selectedYears is not a subset of $availableTaxYears"))
    }

  private def updateSelectedYears(cy: Option[List[Int]], extraYears: List[Int])(implicit ec: ExecutionContext): Future[List[Int]] =
    Future {
      cy.getOrElse(List[Int]()).concat(extraYears).distinct
    }
}
