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

import org.joda.time.DateTime
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.TimeService
import uk.gov.hmrc.play.test.UnitSpec

class RelationshipRecordsTest extends UnitSpec with GuiceOneAppPerSuite {

  //active
  private val activeRecipientRelationshipRecord = RelationshipRecord(
    Recipient.asString(),
    creationTimestamp = "56787",
    participant1StartDate = "20130101",
    relationshipEndReason = Some(RelationshipEndReason.Default),
    participant1EndDate = None,
    otherParticipantInstanceIdentifier = "",
    otherParticipantUpdateTimestamp = "")
  private val activeTransferorRelationshipRecord2 = activeRecipientRelationshipRecord.copy(participant = Transferor.asString())

  //inactive
  private val inactiveRelationshipEndDate1 = new DateTime().minusDays(1).toString(TimeService.defaultDateFormat)
  private val inactiveRelationshipEndDate2 = new DateTime().minusDays(10).toString(TimeService.defaultDateFormat)
  private val inactiveRelationshipEndDate3 = new DateTime().minusDays(1000).toString(TimeService.defaultDateFormat)

  private val inactiveRecipientRelationshipRecord1 = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  private val inactiveRecipientRelationshipRecord2 = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  private val inactiveRecipientRelationshipRecord3 = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  private val inactiveTransferorRelationshipRecord1 = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  private val inactiveTransferorRelationshipRecord2 = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  private val inactiveTransferorRelationshipRecord3 = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  "recordStatus" should {
    "is active" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeRecipientRelationshipRecord,
        activeTransferorRelationshipRecord2
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.recordStatus == Active
    }

    "is active historic" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        inactiveRecipientRelationshipRecord1,
        inactiveRecipientRelationshipRecord2,
        inactiveRecipientRelationshipRecord3
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.recordStatus == ActiveHistoric
    }

    "is historic" in {
      val relationshipRecordList = RelationshipRecordList(Seq())
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.recordStatus == Historic
    }
  }

  "role" should {
    "is active Recipient" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeRecipientRelationshipRecord
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role == Recipient
    }
    "is active Transferor" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeTransferorRelationshipRecord2
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role == Transferor
    }

    "is historic Recipient" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        inactiveRecipientRelationshipRecord1,
        inactiveRecipientRelationshipRecord2,
        inactiveRecipientRelationshipRecord3
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role == Recipient
    }

    "is historic Transferor" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        inactiveTransferorRelationshipRecord1,
        inactiveTransferorRelationshipRecord2,
        inactiveTransferorRelationshipRecord3
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role == Transferor
    }

    //TODO to test properly
    "failed to get role no active and no historic records and no user info" in {
      intercept[Exception] {
        new RelationshipRecords(None, None, None).role
      }.getMessage == "IDK?!"
    }

  }


}
