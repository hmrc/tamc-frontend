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
import config.ApplicationConfig
import connectors.MarriageAllowanceConnector
import errors.ErrorResponseStatus._
import errors._
import events._
import models._
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.SystemTaxYear
import views.helpers.LanguageUtilsImpl
import services.CacheService._


import scala.concurrent.{ExecutionContext, Future}

class TransferService @Inject()(
                                 marriageAllowanceConnector: MarriageAllowanceConnector,
                                 auditConnector: AuditConnector,
                                 cachingService: CachingService,
                                 applicationService: ApplicationService,
                                 timeService: TimeService,
                                 languageUtilsImpl: LanguageUtilsImpl,
                                 taxYear: SystemTaxYear,
                                 appConfig: ApplicationConfig
) extends Logging {

  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      auditConnector.sendEvent(event)
    }

  def isRecipientEligible(transferorNino: Nino, recipientData: RegistrationFormInput)
                         (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkRecipientEligible(transferorNino, recipientData).map(eligible => eligible) recoverWith {
      case error =>
        handleAudit(RecipientFailureEvent(transferorNino, error))
        Future.failed(error)
    }

  private def checkRecipientEligible(transferorNino: Nino, recipientData: RegistrationFormInput)
                                    (implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    for {
      cache <- getCachedDataForEligibilityCheck
      _ <- validateTransferorAgainstRecipient(recipientData, cache)
      (userRecord, taxYears) <- getRecipientRelationship(transferorNino, recipientData)
      validYears = timeService.getValidYearsApplyMAPreviousYears(taxYears)
      _ <- cachingService.put[RecipientRecord](CACHE_RECIPIENT_RECORD, RecipientRecord(record = userRecord, data = recipientData, availableTaxYears = validYears))
    } yield true


  def getCachedDataForEligibilityCheck(implicit
                                       request: Request[_]
                                      ): Future[Option[EligibilityCheckCacheData]] =
    cachingService.get[EligibilityCheckCacheData](USER_ANSWERS_ELIGIBILITY_CHECK)

  private def validateTransferorAgainstRecipient(recipientData: RegistrationFormInput, cache: Option[EligibilityCheckCacheData])
  : Future[Option[EligibilityCheckCacheData]] =
    (recipientData, cache) match {
      case (RegistrationFormInput(_, _, _, _, dom), Some(EligibilityCheckCacheData(_, _, activeRelationshipRecord, historicRelationships, _, _, _)))
        if applicationService.canApplyForMarriageAllowance(historicRelationships, activeRelationshipRecord, timeService.getTaxYearForDate(dom)) =>
        Future.successful(cache)
      case (_, Some(_)) => throw NoTaxYearsForTransferor()
      case _ => throw CacheMissingTransferor()
    }

  def createRelationship(transferorNino: Nino)(implicit request: Request[_], hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[NotificationRecord] = {
    doCreateRelationship(transferorNino)(request, hc, messages, ec) recover {
      case error =>
        handleAudit(CreateRelationshipCacheFailureEvent(error))
        throw error
    }
  }

  def getFinishedData(transferorNino: Nino)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    for {
      userAnswersCachedData <- getUserAnswersCachedData
      notification <- validateFinishedData(userAnswersCachedData)
    } yield notification

  def getUserAnswersCachedData(implicit
                               request: Request[_]
                              ): Future[Option[UserAnswersCacheData]] =
    cachingService.get[UserAnswersCacheData](USER_ANSWERS_CACHE)

  private def validateFinishedData(cacheData: Option[UserAnswersCacheData])(implicit ec: ExecutionContext): Future[NotificationRecord] =
    Future {
      cacheData match {
        case Some(UserAnswersCacheData(_, _, Some(notification), Some(true), _, _, _)) => notification
        case _ => throw CacheCreateRequestNotSent()
      }
    }

  private def doCreateRelationship(transferorNino: Nino)(implicit request: Request[_], hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[NotificationRecord] = {
    for {
      userAnswersCachedData <- getUserAnswersCachedData
      validated <- validateCompleteCache(userAnswersCachedData)
      postCreateData <- sendCreateRelationship(transferorNino, validated)(hc, messages, ec)
      _ <- lockCreateRelationship()
      _ <- auditCreateRelationship(postCreateData)
    } yield validated.notification.get
  }

  private def lockCreateRelationship()(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    logger.info("lockCreateRelationship has been called.")
    cachingService.put[Boolean](CACHE_LOCKED_CREATE, true)
  }


  def getRecipientDetailsFormData()(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[RecipientDetailsFormInput] =
    for {
      userAnswersCachedData <- getUserAnswersCachedData
      registrationData <- validateRegistrationData(userAnswersCachedData)
    } yield registrationData

  private def validateRegistrationData(cacheData: Option[UserAnswersCacheData])(implicit ec: ExecutionContext): Future[RecipientDetailsFormInput] =
    Future {
      cacheData match {
        case Some(UserAnswersCacheData(_, _, _, _, _, Some(registrationData), _)) => registrationData
        case Some(UserAnswersCacheData(_, None, _, _, _, _, _)) => throw CacheMissingRecipient()
        case _ => throw BadFetchRequest()
      }
    }

  private def auditCreateRelationship(cacheData: UserAnswersCacheData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    handleAudit(CreateRelationshipSuccessEvent(cacheData))
  }

  def upsertTransferorNotification(notificationRecord: NotificationRecord)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] = {
    logger.info("upsertTransferorNotification has been called.")
    cachingService.put[NotificationRecord](CACHE_NOTIFICATION_RECORD, notificationRecord)
  }

  def getConfirmationData(nino: Nino)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationModel] = {
    logger.info("getConfirmationData has been called.")
    for {
      cache <- getUserAnswersCachedDataWithTransferorRecord(nino)
      validated <- validateCompleteCache(cache)
      confirmData <- transformCache(validated)
    } yield confirmData
  }

  def getUserAnswersCachedDataWithTransferorRecord(nino: Nino)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserAnswersCacheData]] = {
    getUserAnswersCachedData.flatMap({
      case maybeData if maybeData.forall(_.transferor.isDefined)=> Future.successful(maybeData)
      case _ => updateCacheWithTransferorRecord(nino).flatMap(_ => getUserAnswersCachedData)
    })
  }

  private def updateCacheWithTransferorRecord(nino: Nino)(implicit request: Request[_], hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] = {
    getTransferorRecord(nino)
      .flatMap(cachingService.put[UserRecord](CACHE_TRANSFEROR_RECORD, _))
      .recover({case _ => throw TransferorNotFound()})
      .map(_ => ())
  }

  private def getTransferorRecord(nino: Nino)(implicit request: Request[_], hc: HeaderCarrier, executionContext: ExecutionContext): Future[UserRecord] = marriageAllowanceConnector
    .listRelationship(nino)
    .map(_.userRecord)
    .map(_.getOrElse(throw TransferorNotFound()))
    .map({ loggedInUser =>
      UserRecord(
        cid = loggedInUser.cid,
        timestamp = loggedInUser.timestamp,
        has_allowance = None,
        name = loggedInUser.name)
    })

  private def validateCompleteCache(cacheData: Option[UserAnswersCacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserAnswersCacheData] = {
    logger.info("validateCompleteCache has been called.")
    cacheData match {
      case Some(UserAnswersCacheData(_, _, _, Some(true), _, _, _)) => {
        handleAudit(RelationshipAlreadyCreatedEvent(cacheData.get))
        throw CacheRelationshipAlreadyCreated()
      }
      case Some(
      UserAnswersCacheData(
      Some(_),
      Some(RecipientRecord(UserRecord(_, _, _, _), _, _)),
      Some(notification: NotificationRecord),
      _,
      Some(selectedTaxYears), _, _)) if (selectedTaxYears.size > 0) => Future.successful(cacheData.get)
      case None => throw CacheMissingTransferor()
      case Some(UserAnswersCacheData(None, _, _, _, _, _, _)) => throw CacheMissingTransferor()
      case Some(UserAnswersCacheData(_, None, _, _, _, _, _)) => throw CacheMissingRecipient()
      case Some(UserAnswersCacheData(_, _, None, _, _, _, _)) => throw CacheMissingEmail()
      case Some(UserAnswersCacheData(_, _, _, _, None, _, _)) => throw NoTaxYearsSelected()
      case Some(UserAnswersCacheData(_, _, _, _, Some(selectedTaxYears), _, _)) if (selectedTaxYears.isEmpty) => throw NoTaxYearsSelected()
      case _ => throw BadFetchRequest()
    }
  }

  private def transformCache(cacheData: UserAnswersCacheData)(implicit ec: ExecutionContext): Future[ConfirmationModel] =
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

  def deleteSelectionAndGetCurrentAndPreviousYearsEligibility(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[CurrentAndPreviousYearsEligibility] =
    for {
      _ <- saveSelectedYears(List[Int]())
      res <- getCurrentAndPreviousYearsEligibility
    } yield res

  def getCurrentAndPreviousYearsEligibility(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[CurrentAndPreviousYearsEligibility] =
    cachingService.get[RecipientRecord](CACHE_RECIPIENT_RECORD) map {
      _.fold(throw CacheMissingRecipient())(CurrentAndPreviousYearsEligibility(_, taxYear))
    }

  private def transform(sessionData: UserAnswersCacheData, messages: Messages): CreateRelationshipRequestHolder = {
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

  private def sendCreateRelationship(transferorNino: Nino, data: UserAnswersCacheData)(implicit hc: HeaderCarrier,
                                                                                       messages: Messages, ec: ExecutionContext): Future[UserAnswersCacheData] = {

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

  def saveSelectedYears(selectedYears: List[Int])(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    cachingService.put[List[Int]](CACHE_SELECTED_YEARS, selectedYears)

  def updateSelectedYears(availableTaxYears: List[TaxYear], extraYear: Int, yearAvailableForSelection: Option[Int])(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] = {
    updateSelectedYears(availableTaxYears, List(extraYear).filter(_ > 0), yearAvailableForSelection)
  }

  def updateSelectedYears(availableTaxYears: List[TaxYear], extraYears: List[Int], yearAvailableForSelection: Option[Int])(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    for {
      userAnswersCachedData <- getUserAnswersCachedData
      updatedYears <- updateSelectedYears(userAnswersCachedData.get.selectedYears, extraYears)
      validatedSelectedYears <- validateSelectedYears(availableTaxYears, updatedYears, yearAvailableForSelection)
      savedYears <- saveSelectedYears(validatedSelectedYears)
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
