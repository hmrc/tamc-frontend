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

package utils

import models.{Active, ActiveHistoric, CitizenName, Historic, LoggedInUserInfo, Recipient, RecordStatus, RelationshipRecord, RelationshipRecords, Role}
import org.joda.time.LocalDate
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.test.UnitSpec
import viewModels.{HistorySummaryButton, HistorySummaryViewModel}

trait TamcViewModelTest extends UnitSpec with I18nSupport with GuiceOneAppPerSuite {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  def buildHistorySummaryViewModel(participant: Role, relation: RecordStatus): HistorySummaryViewModel = {
    val activeRelationshipMock = getActiveRelationShip(participant, relation)
    val historicRelationShipMock = getHistoricRelationShip(participant, relation)
    val cid = 1122L
    val timeStamp = new LocalDate().toString
    val hasAllowance = None
    val citizenName = CitizenName(Some("Test"), Some("User"))
    val loggedInUserInfo = Some(LoggedInUserInfo(cid, timeStamp, hasAllowance, Some(citizenName)))
    val relationshipRecords = new RelationshipRecords(activeRelationshipMock, historicRelationShipMock, loggedInUserInfo)

    HistorySummaryViewModel(relationshipRecords)
  }

  private def getActiveRelationShip(participant: Role, relation: RecordStatus): Option[RelationshipRecord] = {
    if (relation == Active)
      Some(RelationshipRecord(participant.asString(), "", "19960327", None, None, "", ""))
    else
      None
  }

  private def getHistoricRelationShip(participant: Role, relation: RecordStatus): Option[Seq[RelationshipRecord]] = {
    if (relation == Historic)
      Some(Seq(RelationshipRecord(participant.asString(), "", "19960327", None, None, "", "")))
    else
      None
  }

  def createButtonForHistorySummaryView(relation: RecordStatus): HistorySummaryButton = {
    relation match {
      case Active => HistorySummaryButton("checkOrUpdateMarriageAllowance", messagesApi("pages.history.active.button"),
        controllers.routes.UpdateRelationshipController.decision().url)
      case Historic => HistorySummaryButton("checkMarriageAllowance", messagesApi("pages.history.historic.button"),
        controllers.routes.UpdateRelationshipController.claims().url)
      case ActiveHistoric =>
        //TODO
        ???
    }
  }
}
