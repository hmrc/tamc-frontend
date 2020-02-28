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

import models.DesRelationshipEndReason._
import models.DesRelationshipEndReason.{Divorce => Separation}
import play.api.i18n.Messages
import utils.TamcViewModelTest
import viewModels.HistoricRow
import views.helpers.TextGenerator

class HistoricRowTest extends TamcViewModelTest {

  val noneEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = None)

  val deathEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Death))
  val divorceEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Separation))
  val invPartEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(InvalidParticipant))
  val cancelledEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Cancelled))
  val rejectedEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Rejected))
  val hmrcEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Hmrc))
  val closedEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Closed))
  val mergerEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Merger))
  val retroEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(Retrospective))
  val systemEndReason: RelationshipRecord = activeRecipientRelationshipRecord.copy(relationshipEndReason = Some(System))

  val recordInterval = "2013 to present"

  "historic row" should {
    val rows = Seq(
      (noneEndReason, recordInterval),
      (activeRecipientRelationshipRecord, recordInterval),
      (deathEndReason, recordInterval),
      (divorceEndReason, recordInterval),
      (invPartEndReason, recordInterval),
      (cancelledEndReason, recordInterval),
      (rejectedEndReason,recordInterval),
      (hmrcEndReason, recordInterval),
      (closedEndReason, recordInterval),
      (mergerEndReason, recordInterval),
      (retroEndReason, recordInterval),
      (systemEndReason, recordInterval)
    ).foreach {
        row => {
          s"be historic row[$row] from rows" in {
            val expectedDate = row._2
            val expectedReason = getReason(row._1)
            val nonPrimaryRow = HistoricRow(row._1)
            nonPrimaryRow.historicDateInterval shouldBe expectedDate
            nonPrimaryRow.historicStatus shouldBe expectedReason
          }
        }
    }
  }

  private def getReason(record: RelationshipRecord)(implicit messages: Messages): String = {
    val endReason = record.relationshipEndReason

    endReason match {
      case Some(x) =>
        val cause = x.value
        val messageKey = s"coc.end-reason.$cause"
        messages(messageKey)
      case _ => ""
    }
  }

}
