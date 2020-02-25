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
import events.{UpdateRelationshipCacheFailureEvent, UpdateRelationshipFailureEvent, UpdateRelationshipSuccessEvent}
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

    def cacheOptionalData[T](data: Option[T], f: T => Future[T])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[T]] = {
      data match {
        case Some(dataToCache) => f(dataToCache) map (Some(_))
        case _ => Future.successful(None)
      }
    }

    //TODO could potentially be made val
    def cacheActiveRelationship(activeRelationship: Option[RelationshipRecord])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RelationshipRecord]] = {
      cacheOptionalData(activeRelationship, cachingService.saveActiveRelationshipRecord(_: RelationshipRecord))
    }

    //TODO could potentially be made val
    def cacheLoggedInUserInfo(loggedInUserInfo: Option[LoggedInUserInfo])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[LoggedInUserInfo]] = {
      cacheOptionalData(loggedInUserInfo, cachingService.saveLoggedInUserInfo(_: LoggedInUserInfo))
    }

    def checkCreateActionLock(trrecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
      cachingService.unlockCreateRelationship().map { _ => trrecord }

    // TODO tidy this
    val transferorRec = UserRecord(relationshipRecords.loggedInUserInfo)
    val cacheActiveRelationshipFuture = cacheActiveRelationship(relationshipRecords.activeRelationship)
    val saveHistoricRelationshipFuture = cachingService.saveHistoricRelationships(relationshipRecords.historicRelationships)
    val cacheLoggedInUserInfoFuture = cacheLoggedInUserInfo(relationshipRecords.loggedInUserInfo)
    val checkCreateActionLockFuture = checkCreateActionLock(transferorRec)
    val saveTransferorRecordFuture = cachingService.saveTransferorRecord(transferorRec)

    for {
      _ <- cacheActiveRelationshipFuture
      _ <- saveHistoricRelationshipFuture
      _ <- cacheLoggedInUserInfoFuture
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

  def getEmailAddressForConfirmation(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    cachingService.fetchAndGetEntry[String](ApplicationConfig.CACHE_EMAIL_ADDRESS).map(_.getOrElse(throw new RuntimeException("Email not found in cache")))
  }

  def saveEmailAddress(emailAddress: EmailAddress)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {
    cachingService.cacheValue[String](ApplicationConfig.CACHE_EMAIL_ADDRESS, emailAddress.value)
  }

  //TODO tuple or type
  def getDivorceExplanationData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(Role, LocalDate)] = {

    val relationshipRecordsFuture = getRelationshipRecords
    val divorceDateFuture = getDivorceDate

    for {
      relationshipRecords <- relationshipRecordsFuture
      divorceDate <- divorceDateFuture
    } yield {

      //TODO is this OK?
      divorceDate.fold(throw new RuntimeException("divorce date missing from cache")) {
        (relationshipRecords.role, _)
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

//  def getRelationship(sessionData: UpdateRelationshipCacheData): RelationshipRecord = {
//    sessionData match {
//      case UpdateRelationshipCacheData(_, _, _, historic, _, Some(EndRelationshipReason(EndReasonCode.REJECT, _, Some(timestamp))), _) => {
//        historic.get.filter { relation => relation.creationTimestamp == timestamp && relation.participant == RoleOld.RECIPIENT }.head
//      }
//      case _ => {
//        sessionData.activeRelationshipRecord.get
//      }
//    }
//  }

  def getUpdateNotification(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[NotificationRecord]] =
    cachingService.getUpdateRelationshipCachedData map {
      case Some(
      UpdateRelationshipCacheData(
      Some(LoggedInUserInfo(_, _, _, _)),
      _,
      active,
      historic,
      notificationRecord, _, _)) if active.isDefined || historic.isDefined => notificationRecord
      case _ => throw CacheMissingUpdateRecord()
    }

  def getConfirmationUpdateDataTemp(implicit hc: HeaderCarrier,
                                    ec: ExecutionContext): Future[(UpdateRelationshipConfirmationModel, Option[UpdateRelationshipCacheData])] =
    for {
      updateRelationshipCache <- cachingService.getUpdateRelationshipCachedData
      validatedUpdateRelationship <- validateupdateRelationshipCompleteCache(updateRelationshipCache)
      requiredData <- transformUpdateRelationshipCache(validatedUpdateRelationship)
    } yield (requiredData, updateRelationshipCache)


  //TODO service layer should know what to pass back
  def getConfirmationUpdateAnswers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationUpdateAnswers] = {
    cachingService.getConfirmationAnswers.map(ConfirmationUpdateAnswers(_))
  }

    def getCancelDates: (LocalDate, LocalDate) = (EndDateMACeasedCalculator.calculateEndDate, EndDateMACeasedCalculator.calculatePaEffectiveDate)

    def getDatesForDivorce(role: Role, divorceDate: LocalDate): (LocalDate, LocalDate) = {
      val marriageAllowanceEndDate = EndDateDivorceCalculator.calculateEndDate(role, divorceDate)
      val personalAllowanceEffectiveDate = EndDateDivorceCalculator.calculatePersonalAllowanceEffectiveDate(marriageAllowanceEndDate)

      (marriageAllowanceEndDate, personalAllowanceEffectiveDate)

    }

  def getUpdateRelationshipCacheDataForDateOfDivorce(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    for {
      updateRelationshipCache <- cachingService.getUpdateRelationshipCachedData
      validatedUpdateRelationship <- validateupdateRelationshipCompleteCache(updateRelationshipCache)
    } yield updateRelationshipCache

  def getUpdateRelationshipCacheForReject(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    cachingService.getUpdateRelationshipCachedData

  def getupdateRelationshipFinishedData(transferorNino: Nino
                                       )(implicit hc: HeaderCarrier,
                                         ec: ExecutionContext): Future[(NotificationRecord, EndRelationshipReason)] =
    for {
      cacheData <- cachingService.getUpdateRelationshipCachedData
      notificationAndEmail <- validateUpdateRelationshipFinishedData(cacheData)
    } yield notificationAndEmail

  def saveEndRelationshipReason(endRealtionshipReason: EndRelationshipReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndRelationshipReason] =
    cachingService.savRelationshipEndReasonRecord(endRealtionshipReason)

  def isValidDivorceDate(dod: Option[LocalDate])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    for {
      cacheData <- cachingService.getUpdateRelationshipCachedData
    } yield isValidDivorceDate(dod, cacheData)

  def getRelationshipRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecords] =
    cachingService.getRelationshipRecords

  def getEndDate(endRelationshipReason: EndRelationshipReason,
                 selectedRelationship: RelationshipRecord): LocalDate =
    endRelationshipReason match {
      case EndRelationshipReason(EndReasonCode.DIVORCE_PY, _, _) => getPreviousYearDate
      case EndRelationshipReason(EndReasonCode.DIVORCE_CY, _, _) =>
        if (time.TaxYear.current.contains(endRelationshipReason.dateOfDivorce.get)) getCurrentDate
        else time.TaxYear.taxYearFor(endRelationshipReason.dateOfDivorce.get).finishes
      case EndRelationshipReason(EndReasonCode.CANCEL, _, _) => getCurrentDate
      case EndRelationshipReason(EndReasonCode.REJECT, _, _) => time.TaxYear.taxYearFor(parseDate(selectedRelationship.participant1StartDate)).starts
    }

  def getRelationEndDate(selectedRelationship: RelationshipRecord): LocalDate =
    LocalDate.parse(selectedRelationship.participant1EndDate.get, DateTimeFormat.forPattern("yyyyMMdd"))


  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    Future {
      customAuditConnector.sendEvent(event)
    }

  private def transformDateAgain(date: String): Option[LocalDate] = {
//    date match {
//      case "" => None
//      case date => Some(DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(date));
//    }
    ???
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


  private def validateupdateRelationshipCompleteCache(cacheData: Option[UpdateRelationshipCacheData]
                                                     )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipCacheData] =
    cacheData match {
      case Some(
      UpdateRelationshipCacheData(
      Some(LoggedInUserInfo(_, _, _, Some(CitizenName(_, _)))),
      _,
      active,
      historic,
      Some(notification: NotificationRecord),
      Some(_), _)) if active.isDefined || historic.isDefined => Future.successful(cacheData.get)
      case _ => throw CacheMissingUpdateRecord()
    }


  private def isValidDivorceDate(dod: Option[LocalDate], cacheData: Option[UpdateRelationshipCacheData]): Boolean =
    (dod, cacheData) match {
      case (Some(dayOfDivorce), Some(UpdateRelationshipCacheData(_, _, Some(RelationshipRecord(_, _, startDate, _, _, _, _)), _, _, _, _))) =>
        transformDateAgain(startDate).exists {
          !_.isAfter(dayOfDivorce)
        }
      case _ =>
        false
    }


  private def transformUpdateRelationshipCache(updateRelationshipCacheData: UpdateRelationshipCacheData
                                              )(implicit hc: HeaderCarrier,
                                                ec: ExecutionContext): Future[UpdateRelationshipConfirmationModel] =
    Future {
      UpdateRelationshipConfirmationModel(
        fullName = updateRelationshipCacheData.loggedInUserInfo.get.name,
        email = updateRelationshipCacheData.notification.get.transferor_email,
        endRelationshipReason = updateRelationshipCacheData.relationshipEndReasonRecord.get,
        historicRelationships = updateRelationshipCacheData.historicRelationships,
        role = updateRelationshipCacheData.roleRecord)
    }




  private def getEndReasonCode(endReasonCode: EndRelationshipReason): String = {
    endReasonCode.endReason match {
      case EndReasonCode.CANCEL => "Cancelled by Transferor"
      case EndReasonCode.DIVORCE_CY => "Divorce/Separation"
      case EndReasonCode.DIVORCE_PY => "Divorce/Separation"
      case EndReasonCode.REJECT => "Rejected by Recipient"
    }
  }
}
