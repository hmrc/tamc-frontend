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

import java.text.SimpleDateFormat
import java.util.Date
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import config.ApplicationConfig
import connectors.ApplicationAuditConnector
import connectors.MarriageAllowanceConnector
import errors.BadFetchRequest
import errors.CacheMissingUpdateRecord
import errors.CacheUpdateRequestNotSent
import errors.CannotUpdateRelationship
import errors.CitizenNotFound
import errors.TransferorNotFound
import events.UpdateRelationshipCacheFailureEvent
import events.UpdateRelationshipFailureEvent
import events.UpdateRelationshipSuccessEvent
import models.CitizenName
import models.EndReasonCode
import models.EndRelationshipReason
import models.LoggedInUserInfo
import models.NotificationRecord
import models.RecipientInformation
import models.RecipientInformation
import models.RelationshipInformation
import models.RelationshipRecord
import models.RelationshipRecordList
import models.RelationshipRecordStatusWrapper
import models.RelationshipRecordWrapper
import models.ResponseStatus
import models.Role
import models.TransferorInformation
import models.UpdateRelationshipCacheData
import models.UpdateRelationshipConfirmationModel
import models.UpdateRelationshipNotificationRequest
import models.UpdateRelationshipRequest
import models.UpdateRelationshipRequestHolder
import models.UpdateRelationshipResponse
import models.UserRecord
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.AuditEvent
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.time.TaxYearResolver
import errors.RecipientNotFound

object UpdateRelationshipService extends UpdateRelationshipService {
  override val marriageAllowanceConnector = MarriageAllowanceConnector
  override val customAuditConnector = ApplicationAuditConnector
  override val cachingService = CachingService
  override val timeService = TimeService
}

trait UpdateRelationshipService {

  val marriageAllowanceConnector: MarriageAllowanceConnector
  val customAuditConnector: AuditConnector
  val cachingService: CachingService
  val timeService: TimeService

  private def handleAudit(event: AuditEvent)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    Future {
      customAuditConnector.sendEvent(event)
    }

  def listRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(RelationshipRecordList, Boolean)] =
    for {
      relationshipRecordWrapper <- fetchListRelationship(transferorNino)
      activeRelationship <- getActiveRelationship(relationshipRecordWrapper)
      historicRelationships <- getHistoricRelationships(relationshipRecordWrapper)
      transformedHistoricRelationships <- transformHistoricRelationships(historicRelationships)
      loggedInUserInfo <- getLoggedInUserInfo(relationshipRecordWrapper)
      savedLoggedInUserInfo <- cachingService.saveLoggedInUserInfo(loggedInUserInfo.get)
      transferorRec <- transformRecord(relationshipRecordWrapper.userRecord, activeRelationship)
      checkedRecord <- checkCreateActionLock(transferorRec)
      savedTransferorRecord <- cachingService.saveTransferorRecord(transferorRec)
    } yield (new RelationshipRecordList(activeRelationship, transformedHistoricRelationships, loggedInUserInfo), canApplyForPreviousYears(historicRelationships, activeRelationship))

  def canApplyForPreviousYears(
      historicRelationships: Option[Seq[RelationshipRecord]], 
      activeRelationship: Option[RelationshipRecord],
      startingFromTaxYear: Int = ApplicationConfig.TAMC_BEGINNING_YEAR): Boolean = {
    val startYear = Math.max(startingFromTaxYear, ApplicationConfig.TAMC_BEGINNING_YEAR)
    val availableYears: Set[Int] = (startYear to timeService.getCurrentTaxYear).toSet
    val unavailableYears: Set[Int] = getUnavailableYears(historicRelationships, activeRelationship)

    (availableYears -- unavailableYears).size > 0
  }

  def getUnavailableYears(historicRelationships: Option[Seq[RelationshipRecord]], activeRelationship: Option[RelationshipRecord]): Set[Int] = {
    val historicYears: Set[Set[Int]] = historicRelationships.
      getOrElse(Seq[RelationshipRecord]()).toSet.
      filter {
        relationship => List(Some("DIVORCE"), Some("CANCELLED"), Some("MERGER"), Some("RETROSPECTIVE")) contains (relationship.relationshipEndReason)
      }.
      map {
        relationship => findOccupiedYears(relationship)
      }

    val activeYears: Set[Int] = activeRelationship.map { relationship => findOccupiedYears(relationship) }.getOrElse(Set[Int]())

    val allYears: Set[Set[Int]] = historicYears.+(activeYears)

    allYears.flatten
  }

  def findOccupiedYears(relationship: RelationshipRecord): Set[Int] = {
    val relStartDate = timeService.getTaxYearForDate(parseRelationshipStartDate(relationship.participant1StartDate))
    val relEndDate = relationship.participant1EndDate.fold(timeService.getCurrentTaxYear)(
      year => {
        val date = parseRelationshipStartDate(year)
        val taxYear = timeService.getTaxYearForDate(date)

        if (relationship.relationshipEndReason == Some("DIVORCE") && date == timeService.getStartDateForTaxYear(taxYear)) {
          taxYear - 1
        } else {
          taxYear
        }
      })

    (relStartDate to relEndDate).toSet
  }

  private def checkCreateActionLock(trrecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cachingService.unlockCreateRelationship().map { _ => trrecord }

  def transformHistoricRelationships(historicRelationships: Option[Seq[RelationshipRecord]]): Future[Option[List[RelationshipRecord]]] =
    Future {
      var list = List[RelationshipRecord]()
      if (historicRelationships != None) {
        for (record <- historicRelationships.get) {
          var relationshipRecord = RelationshipRecord(record.participant, record.creationTimestamp, transformDate(record.participant1StartDate).get, record.relationshipEndReason, transformDate(record.participant1EndDate.getOrElse("")), record.otherParticipantInstanceIdentifier, record.otherParticipantUpdateTimestamp)
          list = relationshipRecord :: list
        }
      }
      if (list.size > 0) Some(list.reverse) else None
    }

  def transformDate(date: String): Option[String] =
    {
      date match {
        case "" => None
        case date =>
          val formatIncomming = new SimpleDateFormat("yyyyMMdd")
          val formatOutgoing = new SimpleDateFormat("dd-MM-yyyy")
          val dateFormated = formatOutgoing.format(formatIncomming.parse(date))
          Some(dateFormated.toString())
      }
    }

  def transformDateAgain(date: String): Option[LocalDate] =
    {
      date match {
        case ""   => None
        case date => Some(DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(date));
      }
    }

  def transformRecord(rec: Option[LoggedInUserInfo], activeRel: Option[RelationshipRecord]): Future[UserRecord] =
    Future {
      rec.fold(throw TransferorNotFound())(rec => UserRecord(
        cid = rec.cid,
        timestamp = rec.timestamp,
        has_allowance = None,
        name = rec.name))
    }

  private def fetchListRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecordWrapper] =
    marriageAllowanceConnector.listRelationship(transferorNino) map {
      case RelationshipRecordStatusWrapper(relationshipRecordWrapper, ResponseStatus("OK"))      => relationshipRecordWrapper
      case RelationshipRecordStatusWrapper(_, ResponseStatus("TAMC:ERROR:TRANSFEROR-NOT-FOUND")) => throw TransferorNotFound()
      case RelationshipRecordStatusWrapper(_, ResponseStatus("TAMC:ERROR:CITIZEN-NOT-FOUND"))    => throw CitizenNotFound()
      case RelationshipRecordStatusWrapper(_, ResponseStatus("TAMC:ERROR:BAD-REQUEST"))          => throw BadFetchRequest()
    }

  private def getActiveRelationship(relationshipRecordWrapper: RelationshipRecordWrapper)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RelationshipRecord]] = {
    val relationships = relationshipRecordWrapper.relationships
    if (relationships.size > 0 && (relationships.head.participant1EndDate == None || isFutureDate(relationships.head.participant1EndDate))) {
      cachingService.saveActiveRelationshipRecord(relationships.head)
      Future { Some(relationships.head) }
    } else {
      Future { None }
    }
  }

  private def isFutureDate(date: Option[String]): Boolean = {
    val format = new java.text.SimpleDateFormat("yyyyMMdd")
    val time = format.parse(date.get).getTime
    time > System.currentTimeMillis()
  }

  private def getHistoricRelationships(relationshipRecordWrapper: RelationshipRecordWrapper)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[RelationshipRecord]]] = {
    val relationships = relationshipRecordWrapper.relationships

    val historic: Option[Seq[RelationshipRecord]] = if (relationships.size > 1 && relationships.head.participant1EndDate == None) {
      Some(relationships.tail)
    } else if (relationships.size > 0 && relationships.head.participant1EndDate != None) {
      Some(relationships)
    } else None

    cachingService.saveHistoricRelationships(historic)
  }

  private def getLoggedInUserInfo(relationshipRecordWrapper: RelationshipRecordWrapper)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[LoggedInUserInfo]] =
    Future { relationshipRecordWrapper.userRecord }

  private def doUpdateRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    for {
      updateRelationshipCacheData <- cachingService.getUpdateRelationshipCachedData
      validated <- validateupdateRelationshipCompleteCache(updateRelationshipCacheData)
      postUpdateData <- sendUpdateRelationship(transferorNino, validated)
      _ <- auditUpdateRelationship(postUpdateData)
    } yield (validated.notification.get)

  private def lockUpdateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cachingService.lockUpdateRelationship()

  private def transformUpdateData(sessionData: UpdateRelationshipCacheData): UpdateRelationshipRequestHolder = {
    val loggedInUser = sessionData.loggedInUserInfo.get
    val relationshipRecord = sessionData.relationshipEndReasonRecord.get
    val endReason = getEndReasonCode(relationshipRecord)

    val selectedRelationship = getRelationship(sessionData)
    val role = selectedRelationship.participant
    val relationCreationTimestamp = selectedRelationship.creationTimestamp
    val endDate = getEndDate(relationshipRecord, selectedRelationship).toString("yyyyMMdd")
    val participiants = role match {
      case Role.TRANSFEROR =>
        (RecipientInformation(instanceIdentifier = selectedRelationship.otherParticipantInstanceIdentifier, updateTimestamp = selectedRelationship.otherParticipantUpdateTimestamp),
          TransferorInformation(updateTimestamp = loggedInUser.timestamp))
      case Role.RECIPIENT =>
        (RecipientInformation(instanceIdentifier = loggedInUser.cid.toString(), updateTimestamp = loggedInUser.timestamp),
          TransferorInformation(updateTimestamp = selectedRelationship.otherParticipantUpdateTimestamp))
    }

    val relationship = RelationshipInformation(creationTimestamp = relationCreationTimestamp, relationshipEndReason = endReason, actualEndDate = endDate)
    val updateRelationshipReq = UpdateRelationshipRequest(participant1 = participiants._1, participant2 = participiants._2, relationship = relationship)
    val sendNotificationData = UpdateRelationshipNotificationRequest(full_name = "UNKNOWN", email = sessionData.notification.get.transferor_email, role = role)
    UpdateRelationshipRequestHolder(request = updateRelationshipReq, notification = sendNotificationData)
  }

  def getRelationship(sessionData: UpdateRelationshipCacheData) = {
    sessionData match {
      case UpdateRelationshipCacheData(_, _, historic, _, Some(EndRelationshipReason(EndReasonCode.REJECT, _, Some(timestamp))), _) => {
        historic.get.filter { relation => relation.creationTimestamp == timestamp && relation.participant == Role.RECIPIENT }.head
      }
      case _ => {
        sessionData.activeRelationshipRecord.get
      }
    }
  }

  private def sendUpdateRelationship(transferorNino: Nino, data: UpdateRelationshipCacheData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipCacheData] =
    marriageAllowanceConnector.updateRelationship(transferorNino, transformUpdateData(data)) map {
      httpResponse =>
        Json.fromJson[UpdateRelationshipResponse](httpResponse.json).get match {
          case UpdateRelationshipResponse(ResponseStatus("OK"))                                    => data
          case UpdateRelationshipResponse(ResponseStatus("TAMC:ERROR:CANNOT-UPDATE-RELATIONSHIP")) => throw CannotUpdateRelationship()
          case UpdateRelationshipResponse(ResponseStatus("TAMC:ERROR:CITIZEN-NOT-FOUND"))          => throw CitizenNotFound()
          case UpdateRelationshipResponse(ResponseStatus("TAMC:ERROR:BAD-REQUEST"))                => throw RecipientNotFound()
        }
    } recover {
      case error =>
        handleAudit(UpdateRelationshipFailureEvent(data, error))
        throw error
    }

  private def validateUpdateRelationshipFinishedData(cacheData: Option[UpdateRelationshipCacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(NotificationRecord, EndRelationshipReason)] =
    Future {
      cacheData match {
        case Some(UpdateRelationshipCacheData(_, _, _, Some(notification), Some(reason), _)) => (notification, reason)
        case _ => throw new CacheUpdateRequestNotSent()
      }
    }

  private def auditUpdateRelationship(cacheData: UpdateRelationshipCacheData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    handleAudit(UpdateRelationshipSuccessEvent(cacheData))
  }

  def getUpdateNotification(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[NotificationRecord]] =
    cachingService.getUpdateRelationshipCachedData map {
      case Some(
        UpdateRelationshipCacheData(
          Some(LoggedInUserInfo(_, _, _, _)),
          active,
          historic,
          notificationRecord, _, _)) if (active.isDefined || historic.isDefined) => notificationRecord
      case _ => throw CacheMissingUpdateRecord()
    }

  def saveSource(source: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, user: AuthContext): Future[String] =
    cachingService.saveSource(source)

  def getConfirmationUpdateData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(UpdateRelationshipConfirmationModel, Option[UpdateRelationshipCacheData])] =
    for {
      updateRelationshipCache <- cachingService.getUpdateRelationshipCachedData
      validatedUpdateRelationship <- validateupdateRelationshipCompleteCache(updateRelationshipCache)
      requiredData <- transformUpdateRelationshipCache(validatedUpdateRelationship)
    } yield (requiredData, updateRelationshipCache)

  def getUpdateRelationshipCacheForReject(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    cachingService.getUpdateRelationshipCachedData

  private def validateupdateRelationshipCompleteCache(cacheData: Option[UpdateRelationshipCacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipCacheData] =
    cacheData match {
      case Some(
        UpdateRelationshipCacheData(
          Some(LoggedInUserInfo(_, _, _, Some(CitizenName(_, _)))),
          active,
          historic,
          Some(notification: NotificationRecord),
          Some(_), _)) if (active.isDefined || historic.isDefined) => Future.successful(cacheData.get)
      case _ => throw CacheMissingUpdateRecord()
    }

  def getupdateRelationshipFinishedData(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(NotificationRecord, EndRelationshipReason)] =
    for {
      cacheData <- cachingService.getUpdateRelationshipCachedData
      notificationAndEmail <- validateUpdateRelationshipFinishedData(cacheData)
    } yield (notificationAndEmail)

  def saveEndRelationshipReason(endRealtionshipReason: EndRelationshipReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndRelationshipReason] = {
    cachingService.savRelationshipEndReasonRecord(endRealtionshipReason)
  }

  def isValidDivorceDate(dod: Option[LocalDate])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    for {
      cacheData <- cachingService.getUpdateRelationshipCachedData
    } yield (isValidDivorceDate(dod, cacheData))

  private def isValidDivorceDate(dod: Option[LocalDate], cacheData: Option[UpdateRelationshipCacheData]): Boolean =
    (dod, cacheData) match {
      case (Some(dayOfDivorce), Some(UpdateRelationshipCacheData(_, Some(RelationshipRecord(_, _, startDate, _, _, _, _)), _, _, _, _))) =>
        transformDateAgain(startDate).exists { !_.isAfter(dayOfDivorce) }
      case _ =>
        false
    }

  def updateRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    doUpdateRelationship(transferorNino) recover {
      case error =>
        handleAudit(UpdateRelationshipCacheFailureEvent(error))
        throw error
    }

  private def transformUpdateRelationshipCache(updateRelationshipCacheData: UpdateRelationshipCacheData)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipConfirmationModel] =
    Future {
      UpdateRelationshipConfirmationModel(
        fullName = updateRelationshipCacheData.loggedInUserInfo.get.name,
        email = updateRelationshipCacheData.notification.get.transferor_email,
        endRelationshipReason = updateRelationshipCacheData.relationshipEndReasonRecord.get,
        historicRelationships = updateRelationshipCacheData.historicRelationships)
    }

  def getEndDate(endRelationshipReason: EndRelationshipReason, selectedRelationship: RelationshipRecord): LocalDate =
    (endRelationshipReason match {
      case EndRelationshipReason(EndReasonCode.DIVORCE_PY, _, _) => LocalDate.now().minusYears(1)
      case EndRelationshipReason(EndReasonCode.DIVORCE_CY, _, _) => if (TaxYearResolver.fallsInThisTaxYear(endRelationshipReason.dateOfDivorce.get)) LocalDate.now() else TaxYearResolver.endOfTaxYear(TaxYearResolver.taxYearFor(endRelationshipReason.dateOfDivorce.get))
      case EndRelationshipReason(EndReasonCode.CANCEL, _, _)     => LocalDate.now()
      case EndRelationshipReason(EndReasonCode.REJECT, _, _)     => TaxYearResolver.startOfTaxYear(TaxYearResolver.taxYearFor(parseRelationshipStartDate(selectedRelationship.participant1StartDate)))
    })

  private def parseRelationshipStartDate(date: String) =
    LocalDate.parse(date, DateTimeFormat.forPattern("yyyyMMdd"))

  private def getEndReasonCode(endReasonCode: EndRelationshipReason): String = {
    endReasonCode.endReason match {
      case EndReasonCode.CANCEL     => "Cancelled by Transferor"
      case EndReasonCode.DIVORCE_CY => "Divorce/Separation"
      case EndReasonCode.DIVORCE_PY => "Divorce/Separation"
      case EndReasonCode.REJECT     => "Rejected by Recipient"
    }
  }
}
