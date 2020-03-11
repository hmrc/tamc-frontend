/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{ApplicationAuditConnector, MarriageAllowanceConnector}
import errors.ErrorResponseStatus._
import errors.{RecipientNotFound, _}
import events.{UpdateRelationshipFailureEvent, UpdateRelationshipSuccessEvent}
import models._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.i18n.Messages
import play.api.libs.json.Json
import services.TimeService._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.time
import utils.LanguageUtils
import viewModels.FinishedUpdateViewModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object UpdateRelationshipService extends UpdateRelationshipService {
  override val marriageAllowanceConnector = MarriageAllowanceConnector
  override val customAuditConnector = ApplicationAuditConnector
  override val cachingService = CachingService
}

trait UpdateRelationshipService {

  val marriageAllowanceConnector: MarriageAllowanceConnector
  val customAuditConnector: AuditConnector
  val cachingService: CachingService

  private val parseDate = parseDateWithFormat(_: String)

  def retrieveRelationshipRecords(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] = {
    marriageAllowanceConnector.listRelationship(nino) map (RelationshipRecords(_))
  }

  def saveRelationshipRecords(relationshipRecords: RelationshipRecords)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] = {

    //TODO we can probably remove this as regardless of what this is returning we return whatever we pass in
    def checkCreateActionLock(trrecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
      cachingService.unlockCreateRelationship().map { _ => trrecord }

    // TODO is this really required
    val transferorRec = UserRecord(Some(relationshipRecords.loggedInUserInfo))
    val checkCreateActionLockFuture = checkCreateActionLock(transferorRec)
    val saveTransferorRecordFuture = cachingService.saveTransferorRecord(transferorRec)

    val cacheRelationshipRecordFuture = cachingService.cacheValue(ApplicationConfig.CACHE_RELATIONSHIP_RECORDS, relationshipRecords)


    for {
      _ <-cacheRelationshipRecordFuture
      _ <- checkCreateActionLockFuture
      _ <- saveTransferorRecordFuture
    } yield relationshipRecords
  }

  def getCheckClaimOrCancelDecision(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cachingService.fetchAndGetEntry[String](ApplicationConfig.CACHE_CHECK_CLAIM_OR_CANCEL)
  }

  def getMakeChangesDecision(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EndMarriageAllowanceReason]] = {
    cachingService.fetchAndGetEntry[EndMarriageAllowanceReason](ApplicationConfig.CACHE_MAKE_CHANGES_DECISION)
  }

  def saveMakeChangeDecision(makeChangeDecision: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndMarriageAllowanceReason] = {
    val endReason = EndMarriageAllowanceReason.toCaseObject(makeChangeDecision)
    cachingService.cacheValue(ApplicationConfig.CACHE_MAKE_CHANGES_DECISION, endReason)
  }

  def getDivorceDate(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[LocalDate]] = {
    cachingService.fetchAndGetEntry[LocalDate](ApplicationConfig.CACHE_DIVORCE_DATE)
  }

  def getEmailAddress(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[String]] = {
    cachingService.fetchAndGetEntry[String](ApplicationConfig.CACHE_EMAIL_ADDRESS)
  }

  private def getEmailAddressForConfirmation(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    cachingService.fetchAndGetEntry[String](ApplicationConfig.CACHE_EMAIL_ADDRESS).map(_.getOrElse(throw new RuntimeException("Email not found in cache")))
  }

  def getInformationForConfirmation(implicit messages: Messages, hc: HeaderCarrier): Future[FinishedUpdateViewModel] = {
    for {
      role <- getRelationshipRecords.map(_.primaryRecord.role)
      email <- getEmailAddressForConfirmation.map(EmailAddress(_))
    } yield FinishedUpdateViewModel(EmailAddress(email), role)
  }

  def saveEmailAddress(emailAddress: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    cachingService.cacheValue[String](ApplicationConfig.CACHE_EMAIL_ADDRESS, emailAddress)
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
    cachingService.cacheValue[LocalDate](ApplicationConfig.CACHE_DIVORCE_DATE, dateOfDivorce)


  def saveCheckClaimOrCancelDecision(checkClaimOrCancelDecision: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cachingService.cacheValue[String](ApplicationConfig.CACHE_CHECK_CLAIM_OR_CANCEL, checkClaimOrCancelDecision)

  def updateRelationship(nino: Nino)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[Unit] = {

    for {
      //TODO would be nice to get a better cache method
      updateRelationshipCacheData <- cachingService.getUpdateRelationshipCachedDataTemp
      updateRelationshipData = UpdateRelationshipRequestHolder(updateRelationshipCacheData, LanguageUtils.isWelsh(messages))
      postUpdateData <- sendUpdateRelationship(nino, updateRelationshipData)
      _ <- auditUpdateRelationship(postUpdateData)
    } yield Future.successful(Unit)
  }

  def getConfirmationUpdateAnswers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationUpdateAnswers] = {
    cachingService.getConfirmationAnswers.map(ConfirmationUpdateAnswers(_))
  }

  def getMAEndingDatesForCancelation: MarriageAllowanceEndingDates = {
    val marriageAllowanceEndDate = EndDateMACeasedCalculator.calculateEndDate
    val personalAllowanceEffectiveDate = EndDateMACeasedCalculator.calculatePaEffectiveDate

    MarriageAllowanceEndingDates(marriageAllowanceEndDate, personalAllowanceEffectiveDate)
  }

  def getMAEndingDatesForDivorce(role: Role, divorceDate: LocalDate): MarriageAllowanceEndingDates = {

    val marriageAllowanceEndDate = EndDateDivorceCalculator.calculateEndDate(role, divorceDate)
    val personalAllowanceEffectiveDate = EndDateDivorceCalculator.calculatePersonalAllowanceEffectiveDate(marriageAllowanceEndDate)

    MarriageAllowanceEndingDates(marriageAllowanceEndDate, personalAllowanceEffectiveDate)

  }

  def saveMarriageAllowanceEndingDates(maEndingDates: MarriageAllowanceEndingDates)(implicit hc: HeaderCarrier): Future[MarriageAllowanceEndingDates] = {
    cachingService.cacheValue[MarriageAllowanceEndingDates](ApplicationConfig.CACHE_MA_ENDING_DATES, maEndingDates)
  }

  def getRelationshipRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] =
    cachingService.getRelationshipRecords.map(_.getOrElse(throw CacheMissingRelationshipRecords()))

  def removeCache(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    cachingService.remove().map(_ => Unit)
  }

  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    Future {
      customAuditConnector.sendEvent(event)
    }

  private def sendUpdateRelationship(transferorNino: Nino,
                                     data: UpdateRelationshipRequestHolder)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipRequestHolder] =
    marriageAllowanceConnector.updateRelationship(transferorNino, data) map {
      httpResponse =>
        Json.fromJson[UpdateRelationshipResponse](httpResponse.json).get match {
          case UpdateRelationshipResponse(ResponseStatus("OK")) => data
          case UpdateRelationshipResponse(ResponseStatus(CANNOT_UPDATE_RELATIONSHIP)) => throw CannotUpdateRelationship()
          case UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST)) => throw RecipientNotFound()
        }
    } recover {
      case error =>
        handleAudit(UpdateRelationshipFailureEvent(data, error))
        throw error
    }

  //TODO is this required
  private def validateUpdateRelationshipFinishedData(cacheData: Option[UpdateRelationshipCacheData]
                                                    )(implicit hc: HeaderCarrier,
                                                      ec: ExecutionContext): Future[(NotificationRecord, EndRelationshipReason)] =
    Future {
      cacheData match {
        case Some(UpdateRelationshipCacheData(_, _, _, _, Some(notification), Some(reason), _)) => (notification, reason)
        case _ => throw new CacheUpdateRequestNotSent()
      }
    }

  private def auditUpdateRelationship(updateData: UpdateRelationshipRequestHolder
                                     )(implicit hc: HeaderCarrier,
                                       ec: ExecutionContext): Future[Unit] = {
    handleAudit(UpdateRelationshipSuccessEvent(updateData))
  }
}
