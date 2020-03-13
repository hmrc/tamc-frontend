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

import errors.{CitizenNotFound, MultipleActiveRecordError, NoPrimaryRecordError}
import org.joda.time.DateTime
import services.TimeService
import uk.gov.hmrc.play.test.UnitSpec

class RelationshipRecordsTest extends UnitSpec {

  val dateFormat = "yyyyMMdd"
  val activeRecipientRelationshipRecord: RelationshipRecord = RelationshipRecord(
    Recipient.asString(),
    creationTimestamp = "56787",
    participant1StartDate = "20130101",
    relationshipEndReason = Some(DesRelationshipEndReason.Default),
    participant1EndDate = None,
    otherParticipantInstanceIdentifier = "1",
    otherParticipantUpdateTimestamp = "TimeStamp")

  val activeTransferorRelationshipRecord2: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant = Transferor.asString())
  val activeRelationshipEndDate1: String = new DateTime().plusDays(10).toString(dateFormat)
  val activeTransferorRelationshipRecord3: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(activeRelationshipEndDate1))


  val inactiveRelationshipEndDate1: String = new DateTime().minusDays(1).toString(dateFormat)
  val inactiveRelationshipEndDate2: String = new DateTime().minusDays(10).toString(dateFormat)
  val inactiveRelationshipEndDate3: String = new DateTime().minusDays(1000).toString(dateFormat)

  val inactiveRecipientRelationshipRecord1: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveRecipientRelationshipRecord2: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveRecipientRelationshipRecord3: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  val inactiveTransferorRelationshipRecord1: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveTransferorRelationshipRecord2: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveTransferorRelationshipRecord3: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))


  val loggedInUserInfo = LoggedInUserInfo(1, "TimeStamp", name = Some(CitizenName(Some("Test"), Some("User"))))
  val nonPrimaryRelationships = Seq(inactiveRecipientRelationshipRecord2, inactiveTransferorRelationshipRecord1)
  val primaryRelationship = activeRecipientRelationshipRecord

  "hasMarriageAllowanceBeenCancelled" should {
    "return true if endDate is present in primary record" in {
      val relationship = RelationshipRecords(primaryRelationship.copy(participant1EndDate = Some("20200101")),
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.hasMarriageAllowanceBeenCancelled shouldBe true
    }

    "return false if endDate is not present in primary record" in {
      val relationship = RelationshipRecords(primaryRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.hasMarriageAllowanceBeenCancelled shouldBe false
    }
  }

  "recipientInformation" should {
    "return Recipient Information from primaryRecord if role is Transferor" in {
      val expectedInstanceIdentifier = activeTransferorRelationshipRecord3.otherParticipantInstanceIdentifier
      val expectedTimestamp = activeTransferorRelationshipRecord3.otherParticipantUpdateTimestamp
      val expectedRecipientInformation = RecipientInformation(expectedInstanceIdentifier, expectedTimestamp)

      val relationship = RelationshipRecords(primaryRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.recipientInformation shouldBe expectedRecipientInformation
    }

    "return Recipient Information from loggedInUser if role is Recipient" in {
      val expectedInstanceIdentifier = loggedInUserInfo.cid.toString
      val expectedTimestamp = loggedInUserInfo.timestamp
      val expectedRecipientInformation = RecipientInformation(expectedInstanceIdentifier, expectedTimestamp)

      val relationship = RelationshipRecords(primaryRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.recipientInformation shouldBe expectedRecipientInformation
    }
  }

  "transferorInformation" should {
    "return Transferor Information from primaryRecord if role is Recipient" in {
      val expectedTimestamp = activeRecipientRelationshipRecord.otherParticipantUpdateTimestamp
      val expectedTransferorInformation = TransferorInformation(expectedTimestamp)

      val relationship = RelationshipRecords(primaryRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.transferorInformation shouldBe expectedTransferorInformation
    }

    "return Transferor Information from loggedInUser if role is Transferor" in {
      val expectedTimestamp = loggedInUserInfo.timestamp
      val expectedTransferorInformation = TransferorInformation(expectedTimestamp)
      val relationship = RelationshipRecords(primaryRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.transferorInformation shouldBe expectedTransferorInformation
    }
  }

  "apply" should {
    "populate RelationshipRecord" in {
      val relationshipRecordList = RelationshipRecordList(
        Seq(activeRecipientRelationshipRecord, inactiveRecipientRelationshipRecord2, inactiveTransferorRelationshipRecord1),
        Some(loggedInUserInfo))

      val relationship = RelationshipRecords(relationshipRecordList)

      relationship shouldBe RelationshipRecords(primaryRelationship, nonPrimaryRelationships, loggedInUserInfo)
    }

    "populate RelationshipRecord when non-primary records aren't present" in {
      val relationshipRecordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord),
        Some(loggedInUserInfo))
      val nonPrimaryRelationship = Seq()

      val relationship = RelationshipRecords(relationshipRecordList)

      relationship shouldBe RelationshipRecords(primaryRelationship, nonPrimaryRelationship, loggedInUserInfo)
    }

    "return a NoPrimaryError when no primary Record is found" in {
      val relationshipRecordList = RelationshipRecordList(Seq(inactiveRecipientRelationshipRecord2, inactiveTransferorRelationshipRecord2),
        Some(loggedInUserInfo))

      val relationship = intercept[NoPrimaryRecordError](RelationshipRecords(relationshipRecordList))

      relationship shouldBe NoPrimaryRecordError()
    }

    "return a MultipleActiveRecordError when multiple primary Records are found" in {
      val relationshipRecordList = RelationshipRecordList(Seq(activeTransferorRelationshipRecord2, activeRecipientRelationshipRecord),
        Some(loggedInUserInfo))

      val relationship = intercept[MultipleActiveRecordError](RelationshipRecords(relationshipRecordList))

      relationship shouldBe MultipleActiveRecordError()
    }

    "return a CitizenNotFound error when non logged in user found" in {
      val relationshipRecordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord))

      val relationship = intercept[CitizenNotFound](RelationshipRecords(relationshipRecordList))

      relationship shouldBe CitizenNotFound()
    }
  }
}
