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
import play.api.i18n.Messages
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
) {

  def retrieveRelationshipRecords(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] =
    marriageAllowanceConnector.listRelationship(nino) map (RelationshipRecords(_, localDate.now()))


  def saveRelationshipRecords(relationshipRecords: RelationshipRecords)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] = {

    def checkCreateActionLock(trrecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
      cachingService.unlockCreateRelationship().map { _ => trrecord }

    val transferorRec = UserRecord(Some(relationshipRecords.loggedInUserInfo))
    val checkCreateActionLockFuture = checkCreateActionLock(transferorRec)
    val saveTransferorRecordFuture = cachingService.saveTransferorRecord(transferorRec)
    val cacheRelationshipRecordFuture = cachingService.cacheValue(appConfig.CACHE_RELATIONSHIP_RECORDS, relationshipRecords)

    for {
      _ <- cacheRelationshipRecordFuture
      _ <- checkCreateActionLockFuture
      _ <- saveTransferorRecordFuture
    } yield relationshipRecords
  }

  def getCheckClaimOrCancelDecision(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cachingService.fetchAndGetEntry[String](appConfig.CACHE_CHECK_CLAIM_OR_CANCEL)
  }

  def getMakeChangesDecision(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cachingService.fetchAndGetEntry[String](appConfig.CACHE_MAKE_CHANGES_DECISION)
  }

  def saveMakeChangeDecision(makeChangeDecision: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    cachingService.cacheValue(appConfig.CACHE_MAKE_CHANGES_DECISION, makeChangeDecision)
  }

  def getDivorceDate(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[LocalDate]] = {
    cachingService.fetchAndGetEntry[LocalDate](appConfig.CACHE_DIVORCE_DATE)
  }

  def getEmailAddress(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EmailAddress]] = {
    cachingService.fetchAndGetEntry[EmailAddress](appConfig.CACHE_EMAIL_ADDRESS)
  }

  def getEmailAddressForConfirmation(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailAddress] = {
    cachingService.fetchAndGetEntry[EmailAddress](appConfig.CACHE_EMAIL_ADDRESS).map(_.getOrElse(throw CacheMissingEmail()))
  }

  def saveEmailAddress(emailAddress: EmailAddress)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailAddress] = {
    cachingService.cacheValue[EmailAddress](appConfig.CACHE_EMAIL_ADDRESS, emailAddress)
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

  def saveDivorceDate(dateOfDivorce: LocalDate)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LocalDate] =
    cachingService.cacheValue[LocalDate](appConfig.CACHE_DIVORCE_DATE, dateOfDivorce)


  def saveCheckClaimOrCancelDecision(checkClaimOrCancelDecision: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cachingService.cacheValue[String](appConfig.CACHE_CHECK_CLAIM_OR_CANCEL, checkClaimOrCancelDecision)

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
      updateRelationshipCacheData <- cachingService.getUpdateRelationshipCachedData
      updateRelationshipData = UpdateRelationshipData(updateRelationshipCacheData)
      updateRelationshipRequest = updateRelationshipRequestHolder(updateRelationshipData)
      postUpdateData <- sendUpdateRelationship(nino, updateRelationshipRequest)
      _ <- auditUpdateRelationship(postUpdateData)
    } yield postUpdateData
  }

  def getConfirmationUpdateAnswers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationUpdateAnswers] = {
    cachingService.getConfirmationAnswers.map(ConfirmationUpdateAnswers(_))
  }

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

  def saveMarriageAllowanceEndingDates(maEndingDates: MarriageAllowanceEndingDates)(implicit hc: HeaderCarrier, ec: ExecutionContext):
  Future[MarriageAllowanceEndingDates] =
    cachingService.cacheValue[MarriageAllowanceEndingDates](appConfig.CACHE_MA_ENDING_DATES, maEndingDates)


  def getRelationshipRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] =
    cachingService.getRelationshipRecords.map(_.getOrElse(throw CacheMissingRelationshipRecords()))

  def removeCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = cachingService.remove()

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
