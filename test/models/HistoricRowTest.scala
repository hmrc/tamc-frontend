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

package models

import models.RelationshipEndReason._
import play.api.i18n.Messages
import utils.TamcViewModelTest
import viewModels.HistoricRow
import views.helpers.TextGenerators

class HistoricRowTest extends TamcViewModelTest {

  val noneEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = None)

  val deathEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Death))
  val divorceEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Divorce))
  val invPartEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(InvalidParticipant))
  val cancelledEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Cancelled))
  val rejectedEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Rejected))
  val hmrcEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Hmrc))
  val closedEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Closed))
  val mergerEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Merger))
  val retroEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Retrospective))
  val systemEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(System))

  "historic row" should {
    val rows = Seq(
      //none
      noneEndReason,
      //valid end reason
      activeRecipientRelationshipRecord,
      activeTransferorRelationshipRecord2,
      activeTransferorRelationshipRecord3,
      inactiveRecipientRelationshipRecord1,
      inactiveRecipientRelationshipRecord2,
      inactiveRecipientRelationshipRecord3,
      //all end relationship statuses
      deathEndReason,
      divorceEndReason,
      invPartEndReason,
      cancelledEndReason,
      rejectedEndReason,
      hmrcEndReason,
      closedEndReason,
      mergerEndReason,
      retroEndReason,
      systemEndReason
    )
    var i = 0

    for (row <- rows) {
      i = i + 1
      s"be historic row[$i] from rows" in {
        val expectedDate = TextGenerators.taxDateIntervalString(
          row.participant1StartDate,
          row.participant1EndDate)
        val expectedStatus = getStatus(row)
        val activeRow = HistoricRow(row)
        activeRow.historicDateInterval shouldBe expectedDate
        activeRow.historicStatus shouldBe expectedStatus
      }
    }
  }

  private def getStatus(record: RelationshipRecord)(implicit messages: Messages): String = {
    val endDate = record.relationshipEndReason

    endDate match {
      case Some(x) =>
        val cause = x.value
        val messageKey = s"coc.end-reason.$cause"
        messages(messageKey)
      case _ => ""
    }
  }

}
