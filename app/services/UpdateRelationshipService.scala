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
import events.{UpdateRelationshipFailureEvent, UpdateRelationshipSuccessEvent}
import models._
import play.api.Logging
import play.api.i18n.Messages
import play.api.mvc.Request
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayJsonFormats._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import utils.SystemLocalDate
import views.helpers.LanguageUtilsImpl

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class UpdateRelationshipService @Inject()(
  marriageAllowanceConnector: MarriageAllowanceConnector,
  endDateForMACeased: EndDateForMACeased,
  endDateDivorceCalculator: EndDateDivorceCalculator,
  auditConnector: AuditConnector,
  cachingService: CachingService,
  appConfig: ApplicationConfig,
  languageUtilsImpl: LanguageUtilsImpl,
  localDate: SystemLocalDate
) extends Logging {

  def retrieveRelationshipRecords(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] =
    marriageAllowanceConnector.listRelationship(nino) map (RelationshipRecords(_, localDate.now()))


  def saveRelationshipRecords(relationshipRecords: RelationshipRecords)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] = {

    def unlockCreateRelationship()(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
      logger.info("unlockCreateRelationship has been called.")
      cachingService.put[Boolean](appConfig.CACHE_LOCKED_CREATE, false)
    }

    def checkCreateActionLock(trrecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
      unlockCreateRelationship().map { _ => trrecord }

    val transferorRec = UserRecord(Some(relationshipRecords.loggedInUserInfo))
    val checkCreateActionLockFuture = checkCreateActionLock(transferorRec)
    val saveTransferorRecordFuture = cachingService.put[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD, transferorRec)
    val cacheRelationshipRecordFuture = cachingService.put(appConfig.CACHE_RELATIONSHIP_RECORDS, relationshipRecords)

    for {
      _ <- cacheRelationshipRecordFuture
      _ <- checkCreateActionLockFuture
      _ <- saveTransferorRecordFuture
    } yield relationshipRecords
  }

  def getCheckClaimOrCancelDecision(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cachingService.get[String](appConfig.CACHE_CHECK_CLAIM_OR_CANCEL)
  }

  def getMakeChangesDecision(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cachingService.get[String](appConfig.CACHE_MAKE_CHANGES_DECISION)
  }

  def saveMakeChangeDecision(makeChangeDecision: String)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    cachingService.put(appConfig.CACHE_MAKE_CHANGES_DECISION, makeChangeDecision)
  }

  def getDivorceDate(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[LocalDate]] = {
    cachingService.get[LocalDate](appConfig.CACHE_DIVORCE_DATE)
  }

  def getEmailAddress(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EmailAddress]] = {
    cachingService.get[EmailAddress](appConfig.CACHE_EMAIL_ADDRESS)
  }

  def getEmailAddressForConfirmation(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailAddress] = {
    cachingService.get[EmailAddress](appConfig.CACHE_EMAIL_ADDRESS).map(_.getOrElse(throw CacheMissingEmail()))
  }

  def saveEmailAddress(emailAddress: EmailAddress)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[EmailAddress] = {
    cachingService.put[EmailAddress](appConfig.CACHE_EMAIL_ADDRESS, emailAddress)
  }

  def getDataForDivorceExplanation(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(Role, LocalDate)] = {

    val relationshipRecordsFuture = getRelationshipRecords
    val divorceDateFuture = getDivorceDate

    for {
      relationshipRecords <- relationshipRecordsFuture
      divorceDate <- divorceDateFuture
    } yield {
      divorceDate.fold(throw CacheMissingDivorceDate()) {
        (relationshipRecords.primaryRecord.role, _)
      }
    }
  }

  def saveDivorceDate(dateOfDivorce: LocalDate)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[LocalDate] =
    cachingService.put[LocalDate](appConfig.CACHE_DIVORCE_DATE, dateOfDivorce)


  def saveCheckClaimOrCancelDecision(checkClaimOrCancelDecision: String)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cachingService.put[String](appConfig.CACHE_CHECK_CLAIM_OR_CANCEL, checkClaimOrCancelDecision)

  def updateRelationship(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[UpdateRelationshipRequestHolder] = {

    def updateRelationshipRequestHolder(updateRelationshipData: UpdateRelationshipData): UpdateRelationshipRequestHolder = {

      val relationshipRecords = updateRelationshipData.relationshipRecords
      val primaryRecord = relationshipRecords.primaryRecord
      val relationshipInfo = relationshipInformation(primaryRecord.creationTimestamp, updateRelationshipData.endMaReason,
        updateRelationshipData.marriageEndDate)
      val recipient = relationshipRecords.recipientInformation
      val transferor = relationshipRecords.transferorInformation
      val updateRelationshipRequest = UpdateRelationshipRequest(recipient, transferor, relationshipInfo)
      val emailNotificationData = updateRelationshipNotificationRequest(updateRelationshipData.email, primaryRecord.role,
        relationshipRecords.loggedInUserInfo, languageUtilsImpl.isWelsh(messages))

      UpdateRelationshipRequestHolder(updateRelationshipRequest, emailNotificationData)
    }

    def relationshipInformation(creationTimeStamp: String, relationshipEndReason: String, endDate: LocalDate): RelationshipInformation = {

      val endDateFormatted = endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
      val desEnumeration = relationshipEndReason match {
        case "Divorce" => "Divorce/Separation"
        case "Cancel" => "Cancelled by Transferor"
        case _ => throw DesEnumerationNotFound()
      }

      RelationshipInformation(creationTimeStamp, desEnumeration, endDateFormatted)
    }

    def updateRelationshipNotificationRequest(email: String, primaryRole: Role, loggedInUserInfo: LoggedInUserInfo, isWelsh: Boolean):
    UpdateRelationshipNotificationRequest = {
      val role = primaryRole.value
      val name = loggedInUserInfo.name.flatMap(_.fullName).getOrElse("Unknown")
      val emailAddress = EmailAddress(email)

      UpdateRelationshipNotificationRequest(name, emailAddress, role, isWelsh)
    }

    for {
      updateRelationshipCacheData <- getUpdateRelationshipCachedData
      updateRelationshipData = UpdateRelationshipData(updateRelationshipCacheData)
      updateRelationshipRequest = updateRelationshipRequestHolder(updateRelationshipData)
      postUpdateData <- sendUpdateRelationship(nino, updateRelationshipRequest)
      _ <- auditUpdateRelationship(postUpdateData)
    } yield postUpdateData
  }

  def getUpdateRelationshipCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipCacheData] =
    cachingService.getUpdateRelationshipCachedData.map(_.getOrElse(throw CacheMapNoFound()))

  def getConfirmationUpdateAnswers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationUpdateAnswers] =
    cachingService.getConfirmationAnswers.map(_.getOrElse(throw CacheMapNoFound())).map(ConfirmationUpdateAnswers(_))

  def getMAEndingDatesForCancelation: MarriageAllowanceEndingDates = {
    val marriageAllowanceEndDate = endDateForMACeased.endDate
    val personalAllowanceEffectiveDate = endDateForMACeased.personalAllowanceEffectiveDate

    MarriageAllowanceEndingDates(marriageAllowanceEndDate, personalAllowanceEffectiveDate)
  }

  def getMAEndingDatesForDivorce(role: Role, divorceDate: LocalDate): MarriageAllowanceEndingDates = {

    val marriageAllowanceEndDate = endDateDivorceCalculator.calculateEndDate(role, divorceDate)
    val personalAllowanceEffectiveDate = endDateDivorceCalculator.calculatePersonalAllowanceEffectiveDate(marriageAllowanceEndDate)

    MarriageAllowanceEndingDates(marriageAllowanceEndDate, personalAllowanceEffectiveDate)

  }

  def saveMarriageAllowanceEndingDates(maEndingDates: MarriageAllowanceEndingDates)(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext):
  Future[MarriageAllowanceEndingDates] =
    cachingService.put[MarriageAllowanceEndingDates](appConfig.CACHE_MA_ENDING_DATES, maEndingDates)

  def getRelationshipRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] =
    cachingService.get[RelationshipRecords](appConfig.CACHE_RELATIONSHIP_RECORDS).map(_.getOrElse(throw CacheMissingRelationshipRecords()))

  def removeCache(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = cachingService.clear()

  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    Future {
      auditConnector.sendEvent(event)
    }

  private def sendUpdateRelationship(transferorNino: Nino,
                                     data: UpdateRelationshipRequestHolder)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipRequestHolder] =
    marriageAllowanceConnector
      .updateRelationship(transferorNino, data)
      .map {
        case Right(updateRelationshipResponse) =>
          updateRelationshipResponse match {
            case Some(UpdateRelationshipResponse(ResponseStatus("OK"))) => data
            case Some(UpdateRelationshipResponse(ResponseStatus(CANNOT_UPDATE_RELATIONSHIP))) => throw CannotUpdateRelationship()
            case Some(UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST))) => throw RecipientNotFound()
            case _ => throw new UnsupportedOperationException("Unable to send relationship update request")
          }
        case Left(error) =>
          error.status.status_code match {
            case CANNOT_UPDATE_RELATIONSHIP => throw CannotUpdateRelationship()
            case BAD_REQUEST => throw RecipientNotFound()
            case _ => throw new UnsupportedOperationException("Unable to send relationship update request")
          }
      }
      .recover {
        case error =>
          handleAudit(UpdateRelationshipFailureEvent(data, error))
          throw error
      }

  private def auditUpdateRelationship(updateData: UpdateRelationshipRequestHolder
                                     )(implicit hc: HeaderCarrier,
                                       ec: ExecutionContext): Future[Unit] = {
    handleAudit(UpdateRelationshipSuccessEvent(updateData))
  }
}
