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

import java.text.SimpleDateFormat

import connectors.{ApplicationAuditConnector, MarriageAllowanceConnector}
import models._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import services.TimeService._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object ListRelationshipService extends ListRelationshipService {
  override val marriageAllowanceConnector = MarriageAllowanceConnector
  override val customAuditConnector = ApplicationAuditConnector
  override val cachingService = CachingService
  override val applicationService: ApplicationService = ApplicationService
}


trait ListRelationshipService {

  val marriageAllowanceConnector: MarriageAllowanceConnector
  val customAuditConnector: AuditConnector
  val cachingService: CachingService
  val applicationService: ApplicationService

  private def handleAudit(event: DataEvent)(implicit headerCarrier: HeaderCarrier): Future[Unit] =
    Future {
      customAuditConnector.sendEvent(event)
    }

  def listRelationship(transferorNino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[(RelationshipRecordList, Boolean)] =
    for {
      relationshipRecordWrapper <- marriageAllowanceConnector.listRelationship(transferorNino)
      activeRelationship <- cacheActiveRelationship(relationshipRecordWrapper.activeRelationship)
      historicRelationships <- cachingService.saveHistoricRelationships(relationshipRecordWrapper.historicRelationships)
      transformedHistoricRelationships <- transformHistoricRelationships(historicRelationships)
      loggedInUserInfo = relationshipRecordWrapper.userRecord
      savedLoggedInUserInfo <- cachingService.saveLoggedInUserInfo(loggedInUserInfo.get)
      transferorRec = UserRecord(relationshipRecordWrapper.userRecord)
      checkedRecord <- checkCreateActionLock(transferorRec)
      savedTransferorRecord <- cachingService.saveTransferorRecord(transferorRec)
    } yield (new RelationshipRecordList(activeRelationship, transformedHistoricRelationships, loggedInUserInfo),
      applicationService.canApplyForPreviousYears(historicRelationships, activeRelationship))

  private def checkCreateActionLock(trrecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cachingService.unlockCreateRelationship().map { _ => trrecord }

  def transformHistoricRelationships(historicRelationships: Option[Seq[RelationshipRecord]]): Future[Option[List[RelationshipRecord]]] =
    Future {
      var list = List[RelationshipRecord]()
      if (historicRelationships.isDefined) {
        for (record <- historicRelationships.get) {
          var relationshipRecord = RelationshipRecord(
            record.participant,
            record.creationTimestamp,
            transformDate(record.participant1StartDate).get,
            record.relationshipEndReason,
            transformDate(record.participant1EndDate.getOrElse("")),
            record.otherParticipantInstanceIdentifier,
            record.otherParticipantUpdateTimestamp)
          list = relationshipRecord :: list
        }
      }
      if (list.nonEmpty) Some(list.reverse) else None
    }

  def transformDate(date: String): Option[String] = {
    date match {
      case "" => None
      case d =>
        val formatIncomming = new SimpleDateFormat("yyyyMMdd")
        val formatOutgoing = new SimpleDateFormat("dd-MM-yyyy")
        val dateFormated = formatOutgoing.format(formatIncomming.parse(d))
        Some(dateFormated.toString)
    }
  }

  def transformDateAgain(date: String): Option[LocalDate] = {
    date match {
      case "" => None
      case d => Some(DateTimeFormat.forPattern("yyyyMMdd").parseLocalDate(d));
    }
  }

  private def cacheActiveRelationship(activeRelationship: Option[RelationshipRecord]
                                     )(implicit hc: HeaderCarrier,
                                       ec: ExecutionContext): Future[Option[RelationshipRecord]] = {
    activeRelationship match{
      case None =>  Future(None)
      case Some(active) =>
        cachingService.saveActiveRelationshipRecord(active)
        Future {Some(active)}
    }
  }
}
