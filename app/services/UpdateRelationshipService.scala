/*
 * Copyright 2019 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.{ApplicationAuditConnector, MarriageAllowanceConnector}
import errors.ErrorResponseStatus._
import errors.{RecipientNotFound, _}
import events.{UpdateRelationshipCacheFailureEvent, UpdateRelationshipFailureEvent, UpdateRelationshipSuccessEvent}
import models._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.i18n.{Lang, Messages}
import play.api.libs.json.Json
import services.TimeService._
import uk.gov.hmrc.domain.Nino
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
  override val timeService = TimeService
}

trait UpdateRelationshipService {

  val marriageAllowanceConnector: MarriageAllowanceConnector
  val customAuditConnector: AuditConnector
  val cachingService: CachingService
  val timeService: TimeService
  private val parseDate = parseDateWithFormat(_: String, format = "yyyyMMdd")

  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    Future {
      customAuditConnector.sendEvent(event)
    }

  def listRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(RelationshipRecordList, Boolean)] =
    for {
      relationshipRecordWrapper <- marriageAllowanceConnector.listRelationship(transferorNino)
      activeRelationship <- cacheActiveRelationship(relationshipRecordWrapper.activeRelationship)
      historicRelationships <- getHistoricRelationships(relationshipRecordWrapper)
      transformedHistoricRelationships <- transformHistoricRelationships(historicRelationships)
      loggedInUserInfo <- getLoggedInUserInfo(relationshipRecordWrapper)
      savedLoggedInUserInfo <- cachingService.saveLoggedInUserInfo(loggedInUserInfo.get)
      transferorRec <- transformRecord(relationshipRecordWrapper.userRecord, activeRelationship)
      checkedRecord <- checkCreateActionLock(transferorRec)
      savedTransferorRecord <- cachingService.saveTransferorRecord(transferorRec)
    } yield (new RelationshipRecordList(activeRelationship, transformedHistoricRelationships, loggedInUserInfo), canApplyForPreviousYears(historicRelationships, activeRelationship))

  private def canApplyForPreviousYears(
                                        historicRelationships: Option[Seq[RelationshipRecord]],
                                        activeRelationship: Option[RelationshipRecord],
                                        startingFromTaxYear: Int = ApplicationConfig.TAMC_BEGINNING_YEAR): Boolean = {
    val startYear = Math.max(startingFromTaxYear, ApplicationConfig.TAMC_BEGINNING_YEAR)
    val availableYears: Set[Int] = (startYear until timeService.getCurrentTaxYear).toSet
    val unavailableYears: Set[Int] = taxYearsThatAreUnavailableForUpdate(historicRelationships, activeRelationship)
    (availableYears -- unavailableYears).nonEmpty
  }

  private def canApplyForCurrentYears(
                                       historicRelationships: Option[Seq[RelationshipRecord]],
                                       activeRelationship: Option[RelationshipRecord]): Boolean =
    !taxYearsThatAreUnavailableForUpdate(historicRelationships, activeRelationship).contains(timeService.getCurrentTaxYear)

  def canApplyForMarriageAllowance(
                                    historicRelationships: Option[Seq[RelationshipRecord]],
                                    activeRelationship: Option[RelationshipRecord],
                                    startingFromTaxYear: Int = ApplicationConfig.TAMC_BEGINNING_YEAR): Boolean =
    canApplyForPreviousYears(historicRelationships, activeRelationship, startingFromTaxYear) ||
      canApplyForCurrentYears(historicRelationships, activeRelationship)

  private def taxYearsThatAreUnavailableForUpdate(historicRelationships: Option[Seq[RelationshipRecord]], activeRelationship: Option[RelationshipRecord]): Set[Int] = {
    val historicYears: Set[Set[Int]] = historicRelationships.getOrElse(Seq[RelationshipRecord]()).toSet.filter {
      relationship =>
        val unavailableReasonCodes = List(
          Some(RelationshipEndReason.Divorce),
          Some(RelationshipEndReason.Cancelled),
          Some(RelationshipEndReason.Merger),
          Some(RelationshipEndReason.Retrospective)
        )
        unavailableReasonCodes contains relationship.relationshipEndReason
    }.map {
      relationship => taxYearsOverlappingWithRelationship(relationship)
    }

    val activeYears: Set[Int] = activeRelationship.map { relationship => taxYearsOverlappingWithRelationship(relationship) }.getOrElse(Set[Int]())
    val allYears: Set[Set[Int]] = historicYears.+(activeYears)
    allYears.flatten
  }

  private def taxYearsOverlappingWithRelationship(relationship: RelationshipRecord): Set[Int] = {
    val taxYearOfRelationshipStart = timeService.getTaxYearForDate(parseDate(relationship.participant1StartDate))
    val taxYearOfRelationshipEnd = relationship.participant1EndDate.fold(timeService.getCurrentTaxYear)(
      participant1EndDateAsString => {
        val participant1EndDate = parseDate(participant1EndDateAsString)
        val taxYearOfParticipant1EndDate = timeService.getTaxYearForDate(participant1EndDate)
        val isParticipant1EndDateOnTheFirstDayOfTaxYear: Boolean = participant1EndDate == timeService.getStartDateForTaxYear(taxYearOfParticipant1EndDate)

        relationship.relationshipEndReason match {
          case Some(RelationshipEndReason.Divorce) if isParticipant1EndDateOnTheFirstDayOfTaxYear => taxYearOfParticipant1EndDate - 1
          case _ => taxYearOfParticipant1EndDate
        }
      })

    (taxYearOfRelationshipStart to taxYearOfRelationshipEnd).toSet
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

  def transformDate(date: String): Option[String] = {
    date match {
      case "" => None
      case date =>
        val formatIncomming = new SimpleDateFormat("yyyyMMdd")
        val formatOutgoing = new SimpleDateFormat("dd-MM-yyyy")
        val dateFormated = formatOutgoing.format(formatIncomming.parse(date))
        Some(dateFormated.toString())
    }
  }

  def transformDateAgain(date: String): Option[LocalDate] = {
    date match {
      case "" => None
      case date => Some(DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(date));
    }
  }

  def transformRecord(rec: Option[LoggedInUserInfo], activeRel: Option[RelationshipRecord]): Future[UserRecord] =
    Future.successful {
      rec.fold(throw TransferorNotFound())(rec => UserRecord(
        cid = rec.cid,
        timestamp = rec.timestamp,
        has_allowance = None,
        name = rec.name))
    }

  private def cacheActiveRelationship(activeRelationship: Option[RelationshipRecord])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RelationshipRecord]] = {
    activeRelationship match{
      case None =>  Future(None)
      case Some(active) =>
        cachingService.saveActiveRelationshipRecord(active)
        Future {Some(active)}
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
    Future {
      relationshipRecordWrapper.userRecord
    }

  private def doUpdateRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[NotificationRecord] =
    for {
      updateRelationshipCacheData <- cachingService.getUpdateRelationshipCachedData
      validated <- validateupdateRelationshipCompleteCache(updateRelationshipCacheData)
      postUpdateData <- sendUpdateRelationship(transferorNino, validated)
      _ <- auditUpdateRelationship(postUpdateData)
    } yield (validated.notification.get)

  private def lockUpdateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cachingService.lockUpdateRelationship()

  private def transformUpdateData(sessionData: UpdateRelationshipCacheData, messages: Messages): UpdateRelationshipRequestHolder = {
    val loggedInUser = sessionData.loggedInUserInfo.get
    val relationshipRecord = sessionData.relationshipEndReasonRecord.get
    val endReason = getEndReasonCode(relationshipRecord)

    val selectedRelationship = getRelationship(sessionData)
    val role = selectedRelationship.participant
    val relationCreationTimestamp = selectedRelationship.creationTimestamp
    val effectiveDate = getEndDate(relationshipRecord, selectedRelationship)
    val endDate = effectiveDate.toString("yyyyMMdd")

    val isRetrospective = relationshipRecord.endReason match {
      case EndReasonCode.REJECT =>
        selectedRelationship.participant1EndDate != None && selectedRelationship.participant1EndDate.nonEmpty &&
          !selectedRelationship.participant1EndDate.get.equals("") && (effectiveDate.getYear != TimeService.getCurrentTaxYear)
      case _ => false
    }

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
    val sendNotificationData = UpdateRelationshipNotificationRequest(full_name = "UNKNOWN", email = sessionData.notification.get.transferor_email, role = role, welsh = LanguageUtils.isWelsh(messages), isRetrospective = isRetrospective)
    UpdateRelationshipRequestHolder(request = updateRelationshipReq, notification = sendNotificationData)
  }

  def getRelationship(sessionData: UpdateRelationshipCacheData) = {
    sessionData match {
      case UpdateRelationshipCacheData(_, _, _, historic, _, Some(EndRelationshipReason(EndReasonCode.REJECT, _, Some(timestamp))), _) => {
        historic.get.filter { relation => relation.creationTimestamp == timestamp && relation.participant == Role.RECIPIENT }.head
      }
      case _ => {
        sessionData.activeRelationshipRecord.get
      }
    }
  }

  private def sendUpdateRelationship(transferorNino: Nino, data: UpdateRelationshipCacheData)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[UpdateRelationshipCacheData] =
    marriageAllowanceConnector.updateRelationship(transferorNino, transformUpdateData(data, messages)) map {
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

  private def validateUpdateRelationshipFinishedData(cacheData: Option[UpdateRelationshipCacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(NotificationRecord, EndRelationshipReason)] =
    Future {
      cacheData match {
        case Some(UpdateRelationshipCacheData(_, _, _, _, Some(notification), Some(reason), _)) => (notification, reason)
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
      _,
      active,
      historic,
      notificationRecord, _, _)) if (active.isDefined || historic.isDefined) => notificationRecord
      case _ => throw CacheMissingUpdateRecord()
    }

  def getConfirmationUpdateData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(UpdateRelationshipConfirmationModel, Option[UpdateRelationshipCacheData])] =
    for {
      updateRelationshipCache <- cachingService.getUpdateRelationshipCachedData
      validatedUpdateRelationship <- validateupdateRelationshipCompleteCache(updateRelationshipCache)
      requiredData <- transformUpdateRelationshipCache(validatedUpdateRelationship)
    } yield (requiredData, updateRelationshipCache)

  def getUpdateRelationshipCacheDataForDateOfDivorce(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    for {
      updateRelationshipCache <- cachingService.getUpdateRelationshipCachedData
      validatedUpdateRelationship <- validateupdateRelationshipCompleteCache(updateRelationshipCache)
    } yield (updateRelationshipCache)

  def getUpdateRelationshipCacheForReject(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    cachingService.getUpdateRelationshipCachedData

  private def validateupdateRelationshipCompleteCache(cacheData: Option[UpdateRelationshipCacheData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipCacheData] =
    cacheData match {
      case Some(
      UpdateRelationshipCacheData(
      Some(LoggedInUserInfo(_, _, _, Some(CitizenName(_, _)))),
      _,
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

  def saveEndRelationshipReason(endRealtionshipReason: EndRelationshipReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndRelationshipReason] =
    cachingService.savRelationshipEndReasonRecord(endRealtionshipReason)

  def isValidDivorceDate(dod: Option[LocalDate])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    for {
      cacheData <- cachingService.getUpdateRelationshipCachedData
    } yield (isValidDivorceDate(dod, cacheData))

  private def isValidDivorceDate(dod: Option[LocalDate], cacheData: Option[UpdateRelationshipCacheData]): Boolean =
    (dod, cacheData) match {
      case (Some(dayOfDivorce), Some(UpdateRelationshipCacheData(_, _, Some(RelationshipRecord(_, _, startDate, _, _, _, _)), _, _, _, _))) =>
        transformDateAgain(startDate).exists {
          !_.isAfter(dayOfDivorce)
        }
      case _ =>
        false
    }

  def updateRelationship(transferorNino: Nino, lang: Lang)(implicit hc: HeaderCarrier, messages: Messages, ec: ExecutionContext): Future[NotificationRecord] =
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
        historicRelationships = updateRelationshipCacheData.historicRelationships,
        role = updateRelationshipCacheData.roleRecord)
    }

  def getEndDate(endRelationshipReason: EndRelationshipReason, selectedRelationship: RelationshipRecord): LocalDate =
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


  private def getEndReasonCode(endReasonCode: EndRelationshipReason): String = {
    endReasonCode.endReason match {
      case EndReasonCode.CANCEL => "Cancelled by Transferor"
      case EndReasonCode.DIVORCE_CY => "Divorce/Separation"
      case EndReasonCode.DIVORCE_PY => "Divorce/Separation"
      case EndReasonCode.REJECT => "Rejected by Recipient"
    }
  }
}
