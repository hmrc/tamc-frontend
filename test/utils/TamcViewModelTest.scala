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

import models._
import org.joda.time.{DateTime, LocalDate}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi}
import services.TimeService
import uk.gov.hmrc.play.test.UnitSpec
import viewModels.{HistorySummaryButton, HistorySummaryViewModel}

trait TamcViewModelTest extends UnitSpec with I18nSupport with GuiceOneAppPerSuite {

  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  //active
  val activeRecipientRelationshipRecord: RelationshipRecord = RelationshipRecord(
    Recipient.asString(),
    creationTimestamp = "56787",
    participant1StartDate = "20130101",
    relationshipEndReason = Some(DesRelationshipEndReason.Default),
    participant1EndDate = None,
    otherParticipantInstanceIdentifier = "1",
    otherParticipantUpdateTimestamp = "TimeStamp")

  val activeTransferorRelationshipRecord2: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant = Transferor.asString())
  val activeRelationshipEndDate1: String = new DateTime().plusDays(10).toString(TimeService.defaultDateFormat)
  val activeTransferorRelationshipRecord3: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(activeRelationshipEndDate1))

  //inactive
  val inactiveRelationshipEndDate1: String = new DateTime().minusDays(1).toString(TimeService.defaultDateFormat)
  val inactiveRelationshipEndDate2: String = new DateTime().minusDays(10).toString(TimeService.defaultDateFormat)
  val inactiveRelationshipEndDate3: String = new DateTime().minusDays(1000).toString(TimeService.defaultDateFormat)

  val inactiveRecipientRelationshipRecord1: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveRecipientRelationshipRecord2: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveRecipientRelationshipRecord3: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  val inactiveTransferorRelationshipRecord1: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveTransferorRelationshipRecord2: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveTransferorRelationshipRecord3: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  def buildHistorySummaryViewModel(participant: Role): HistorySummaryViewModel = {
    val activeRelationshipMock = getActiveRelationShip(participant)
    val cid = 1122L
    val timeStamp = new LocalDate().toString
    val hasAllowance = None
    val citizenName = CitizenName(Some("Test"), Some("User"))
    val loggedInUserInfo = LoggedInUserInfo(cid, timeStamp, hasAllowance, Some(citizenName))
    val relationshipRecords = new RelationshipRecords(activeRelationshipMock, Seq(), loggedInUserInfo)

    HistorySummaryViewModel(relationshipRecords)
  }

  private def getActiveRelationShip(participant: Role): RelationshipRecord = {
      RelationshipRecord(participant.asString(), "", "19960327", None, None, "", "")
  }

  def createButtonForHistorySummaryView: HistorySummaryButton = {
   HistorySummaryButton("checkOrUpdateMarriageAllowance", messagesApi("pages.history.active.button"),
        controllers.routes.UpdateRelationshipController.decision().url)
  }
}
