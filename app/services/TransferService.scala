/*
 * Copyright 2016 HM Revenue & Customs
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

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import connectors.ApplicationAuditConnector
import connectors.MarriageAllowanceConnector
import errors._
//import events.AuthSuccessEvent
import events.CheckRecipientRelationshipEvent
import events.CheckTransferorRelationshipEvent
import events.CreateRelationshipCacheFailureEvent
import events.CreateRelationshipFailureEvent
import events.CreateRelationshipSuccessEvent
import events.RecipientFailureEvent
import events.TransferorDetailsExistEvent
//import events.TransferorFailureEvent
import events.RelationshipAlreadyCreatedEvent
import models._
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Request
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.AuditEvent
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.SessionKeys
import play.api.Logger
import events.TransferorRelationshipDataInconsistent
import uk.gov.hmrc.time.TaxYearResolver

object TransferService extends TransferService {
  override val marriageAllowanceConnector = MarriageAllowanceConnector
  override val customAuditConnector = ApplicationAuditConnector
  override val cachingService = CachingService
  override val timeService = TimeService
}

trait TransferService {

  val marriageAllowanceConnector: MarriageAllowanceConnector
  val customAuditConnector: AuditConnector
  val cachingService: CachingService
  val timeService: TimeService

  private def handleAudit(event: AuditEvent)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    Future {
      customAuditConnector.sendEvent(event)
    }

  def getEligibleTransferorName(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CitizenName]] = {
    for {
      initialCacheState <- cachingService.getCachedData
      name <- validateTransferorName(initialCacheState)
    } yield (name)
  }

  private def validateTransferorName(cache: Option[CacheData]): Future[Option[CitizenName]] =
    Future {
      cache match {
        case None => throw CacheMissingTransferor()
        case Some(CacheData(_, _, _, Some(true), _)) => throw CacheRelationshipAlreadyCreated()
        case Some(CacheData(None, _, _, _, _)) => throw CacheMissingTransferor()
        case Some(CacheData(Some(UserRecord(_, _, _, name)), _, _, _, _)) => name
      }
    }

  private def checkCreateActionLock(trrecord: UserRecord, initialCacheState: Option[CacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    (trrecord, initialCacheState) match {
      case (UserRecord(_, _, _, _), Some(CacheData(_, _, _, Some(true), _))) =>
        Logger.warn("Backend System returned eligible transferor although transfer is already in place")
        handleAudit(TransferorRelationshipDataInconsistent(trrecord, initialCacheState))
        cachingService.unlockCreateRelationship().map { _ => trrecord }
      case _ =>
        Future { trrecord }
    }

  private def auditTransferor(transferorNino: Nino, trrecord: UserRecord, transferor: TransferorEligibilityHolder)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] = {
    val auditEvent = transferor match {
      case TransferorEligibilityHolder(true, name) => TransferorDetailsExistEvent(name, transferorNino)
      case _                                       => CheckTransferorRelationshipEvent(trrecord)
    }
    handleAudit(auditEvent)
    Future { trrecord }
  }

  def isRecipientEligible(transferorNino: Nino, recipientData: RegistrationFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    checkRecipientEligible(transferorNino, recipientData).map(eligible => eligible) recoverWith {
      case error =>
        handleAudit(RecipientFailureEvent(transferorNino, error))
        Future.failed(error)
    }

  private def checkRecipientEligible(transferorNino: Nino, recipientData: RegistrationFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    for {
      cache <- cachingService.getUpdateRelationshipCachedData
      _ <- validateTransferorAgainstRecipient(recipientData, cache)
      (recipientRecord, taxYears) <- getRecipientRelationship(transferorNino, recipientData)
      savedRecipientRecord <- cachingService.saveRecipientRecord(
        recipientRecord,
        recipientData,
        taxYears.getOrElse(List[TaxYear]()))
    } yield (true)

  private def validateTransferorAgainstRecipient(recipientData: RegistrationFormInput, cache: Option[UpdateRelationshipCacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    (recipientData, cache) match {
      case (RegistrationFormInput(_, _, _, _, dom), Some(UpdateRelationshipCacheData(_, activeRelationshipRecord, historicRelationships, _, _, _)))
        if UpdateRelationshipService.canApplyForPreviousYears(historicRelationships, activeRelationshipRecord, timeService.getTaxYearForDate(dom)) =>
          Future { cache }
      case (RegistrationFormInput(_, _, _, _, dom), Some(UpdateRelationshipCacheData(_, activeRelationshipRecord, historicRelationships, _, _, _))) =>
        Future.failed(new NoTaxYearsForTransferor())
      case _ => throw CacheMissingTransferor()
    }

  def createRelationship(transferorNino: Nino, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    doCreateRelationship(transferorNino, journey) recover {
      case error =>
        handleAudit(CreateRelationshipCacheFailureEvent(error))
        throw error
    }

  def getFinishedData(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    for {
      cacheData <- cachingService.getCachedData
      notification <- validateFinishedData(cacheData)
    } yield (notification)

  private def validateFinishedData(cacheData: Option[CacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    Future {
      cacheData match {
        case Some(CacheData(_, _, Some(notification), Some(true), _)) => notification
        case _ => throw new CacheCreateRequestNotSent()
      }
    }

  private def doCreateRelationship(transferorNino: Nino, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    for {
      cacheData <- cachingService.getCachedData
      validated <- validateCompleteCache(cacheData)
      postCreateData <- sendCreateRelationship(transferorNino, validated, journey)
      _ <- lockCreateRelationship()
      _ <- auditCreateRelationship(postCreateData, journey)
    } yield (validated.notification.get)

  private def lockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    cachingService.lockCreateRelationship
  }

  private def auditCreateRelationship(cacheData: CacheData, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    handleAudit(CreateRelationshipSuccessEvent(cacheData, journey))
  }

  def getTransferorNotification(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[NotificationRecord]] =
    cachingService.getCachedData map {
      case Some(CacheData(_, _, _, Some(true), _)) => throw CacheRelationshipAlreadyCreated()
      case Some(
        CacheData(
          Some(UserRecord(_, _, _, _)),
          Some(RecipientRecord(UserRecord(_, _, _, _), _, _)),
          notificationRecord, _, _)) => notificationRecord
      case None                              => throw CacheMissingTransferor()
      case Some(CacheData(None, _, _, _, _)) => throw CacheMissingTransferor()
      case Some(CacheData(_, None, _, _, _)) => throw CacheMissingRecipient()
    }

  def upsertTransferorNotification(notificationRecord: NotificationRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext, user: AuthContext): Future[NotificationRecord] =
    cachingService.saveNotificationRecord(notificationRecord)

  def getConfirmationData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationModel] =
    for {
      cache <- cachingService.getCachedData
      validated <- validateCompleteCache(cache)
      confirmData <- transformCache(validated)
    } yield (confirmData)

  private def validateCompleteCache(cacheData: Option[CacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheData] =
    {
      cacheData match {
        case Some(CacheData(_, _, _, Some(true), _)) => {
          handleAudit(RelationshipAlreadyCreatedEvent(cacheData.get))
          throw CacheRelationshipAlreadyCreated()
        }
        case Some(
          CacheData(
            Some(UserRecord(_, _, _, _)),
            Some(RecipientRecord(UserRecord(_, _, _, _), _, _)),
            Some(notification: NotificationRecord),
            _,
            Some(selectedTaxYears))) if (selectedTaxYears.size > 0) => Future.successful(cacheData.get)
        case None => throw CacheMissingTransferor()
        case Some(CacheData(None, _, _, _, _)) => throw CacheMissingTransferor()
        case Some(CacheData(_, None, _, _, _)) => throw CacheMissingRecipient()
        case Some(CacheData(_, _, None, _, _)) => throw CacheMissingEmail()
        case Some(CacheData(_, _, _, _, None)) => throw NoTaxYearsSelected()
        case Some(CacheData(_, _, _, _, Some(selectedTaxYears))) if (selectedTaxYears.size == 0) => throw NoTaxYearsSelected()
      }
    }

  private def transformCache(cacheData: CacheData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationModel] =
    Future {
      ConfirmationModel(
        transferorFullName = cacheData.transferor.get.name,
        transferorEmail = cacheData.notification.get.transferor_email,
        recipientFirstName = cacheData.recipient.get.data.name,
        recipientLastName = cacheData.recipient.get.data.lastName,
        recipientNino = cacheData.recipient.get.data.nino,
        availableYears = getAvailableTaxYears(cacheData.selectedYears.get))
    }

  private def getAvailableTaxYears(selectedYears: List[Int]): List[TaxYear] =
    selectedYears.map { x =>
      TaxYear(x, isCurrentTaxYear(x))
    }.sortWith((m1, m2) => m1.year > m2.year)

  private def isCurrentTaxYear(year: Int): Option[Boolean] =
    if (TaxYearResolver.currentTaxYear == year)
      Some(true)
    else
      None

  private def getRecipientRelationship(transferorNino: Nino, recipientData: RegistrationFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(UserRecord, Option[List[TaxYear]])] =
    marriageAllowanceConnector.getRecipientRelationship(transferorNino, recipientData) map {
      httpResponse =>
        Json.fromJson[GetRelationshipResponse](httpResponse.json).get match {
          case GetRelationshipResponse(Some(recipientRecord), availableYears, ResponseStatus("OK")) => (recipientRecord, availableYears)
          case GetRelationshipResponse(None, _, ResponseStatus("TAMC:ERROR:RECIPIENT-NOT-FOUND"))   => throw RecipientNotFound()
        }
    }

  def deleteSelectionAndGetCurrentAndExtraYearEligibility(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(Boolean, List[TaxYear], RecipientRecord)] =
    for {
      _ <- cachingService.saveSelectedYears(List[Int]())
      res <- getCurrentAndExtraYearEligibility
    } yield (res)

  def getCurrentAndExtraYearEligibility(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(Boolean, List[TaxYear], RecipientRecord)] =
    for {
      recipient <- getEligibleYears
    } yield (
      !recipient.aivailableTaxYears.filter { _.year == timeService.getCurrentTaxYear }.isEmpty,
      recipient.aivailableTaxYears.filter { _.year != timeService.getCurrentTaxYear },
      recipient)

  def getEligibleYears(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RecipientRecord] =
    for {
      cache <- cachingService.getCachedData
    } yield (cache.get.recipient.get)

  private def transform(sessionData: CacheData): CreateRelationshipRequestHolder = {
    val transferor = sessionData.transferor.get
    val recipient = sessionData.recipient.get.record
    val formData = sessionData.recipient.get.data
    val email = sessionData.notification.get.transferor_email
    val createRelationshipreq = CreateRelationshipRequest(
      transferor_cid = transferor.cid,
      transferor_timestamp = transferor.timestamp,
      recipient_cid = recipient.cid,
      recipient_timestamp = recipient.timestamp,
      taxYears = sessionData.selectedYears.get.sortWith(_ < _))
    val sendNotificationData = CreateRelationshipNotificationRequest(full_name = "UNKNOWN", email = email)
    CreateRelationshipRequestHolder(request = createRelationshipreq, notification = sendNotificationData)
  }

  private def sendCreateRelationship(transferorNino: Nino, data: CacheData, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[CacheData] =
    marriageAllowanceConnector.createRelationship(transferorNino, transform(data), journey) map {
      httpResponse =>
        Json.fromJson[CreateRelationshipResponse](httpResponse.json).get match {
          case CreateRelationshipResponse(ResponseStatus("OK"))                                    => data
          case CreateRelationshipResponse(ResponseStatus("TAMC:ERROR:CANNOT-CREATE-RELATIONSHIP")) => throw CannotCreateRelationship()
        }
    } recover {
      case error =>
        handleAudit(CreateRelationshipFailureEvent(data, journey, error))
        throw error
    }

  def saveSelectedYears(recipient: RecipientRecord, selectedYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    cachingService.saveSelectedYears(selectedYears)

  def updateSelectedYears(recipient: RecipientRecord, extraYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    for {
      cache <- cachingService.getCachedData
      updatedYears <- updateSelectedYears(cache.get.selectedYears, extraYears)
      validatedSelectedYears <- validateSelectedYears(recipient.aivailableTaxYears, updatedYears)
      savedYears <- cachingService.saveSelectedYears(validatedSelectedYears)
    } yield (savedYears)

  def validateSelectedYears(aivailableTaxYears: List[TaxYear], selectedYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    if (selectedYears.isEmpty) {
      throw new NoTaxYearsSelected
    } else if (selectedYears.forall { aivailableTaxYears.map(_.year).contains(_) }) {
      Future { selectedYears }
    } else {
      Future.failed(new IllegalArgumentException(s"${selectedYears} is not a subset of ${aivailableTaxYears}"))
    }

  private def updateSelectedYears(cy: Option[List[Int]], extraYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    Future {
      cy.getOrElse(List[Int]()).union(extraYears).distinct
    }
}
