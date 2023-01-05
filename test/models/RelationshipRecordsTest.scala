/*
 * Copyright 2023 HM Revenue & Customs
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
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import test_utils.data.RelationshipRecordData._
import utils.{BaseTest, SystemLocalDate}

import java.time.LocalDate

class RelationshipRecordsTest extends BaseTest with GuiceOneAppPerSuite {

  val loggedInUserInfo = LoggedInUserInfo(1, "TimeStamp", name = Some(CitizenName(Some("Test"), Some("User"))))
  val nonPrimaryRelationships = Seq(inactiveRecipientRelationshipRecord2, inactiveTransferorRelationshipRecord1)
  val primaryRecipientRelationship = activeRecipientRelationshipRecord
  val primaryTransferorRelationship = activeTransferorRelationshipRecord2
  lazy val localDate: LocalDate = instanceOf[SystemLocalDate].now()

  "hasMarriageAllowanceBeenCancelled" should {
    "return true if endDate is present in primary record (active record)" in {
      val relationship = RelationshipRecords(primaryRecipientRelationship.copy(participant1EndDate = Some("20200101")),
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.hasMarriageAllowanceBeenCancelled shouldBe true
    }

    "return false if endDate is not present in primary record" in {
      val relationship = RelationshipRecords(primaryRecipientRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.hasMarriageAllowanceBeenCancelled shouldBe false
    }
  }

  "recipientInformation" should {
    "return Recipient Information from primaryRecord if role is Transferor" in {
      val expectedInstanceIdentifier = primaryTransferorRelationship.otherParticipantInstanceIdentifier
      val expectedTimestamp = primaryTransferorRelationship.otherParticipantUpdateTimestamp
      val expectedRecipientInformation = RecipientInformation(expectedInstanceIdentifier, expectedTimestamp)

      val relationship = RelationshipRecords(primaryTransferorRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.recipientInformation shouldBe expectedRecipientInformation
    }

    "return Recipient Information from loggedInUser if role is Recipient" in {
      val expectedInstanceIdentifier = loggedInUserInfo.cid.toString
      val expectedTimestamp = loggedInUserInfo.timestamp
      val expectedRecipientInformation = RecipientInformation(expectedInstanceIdentifier, expectedTimestamp)

      val relationship = RelationshipRecords(primaryRecipientRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.recipientInformation shouldBe expectedRecipientInformation
    }
  }

  "transferorInformation" should {
    "return Transferor Information from primaryRecord if role is Recipient" in {
      val expectedTimestamp = activeRecipientRelationshipRecord.otherParticipantUpdateTimestamp
      val expectedTransferorInformation = TransferorInformation(expectedTimestamp)

      val relationship = RelationshipRecords(primaryRecipientRelationship,
        nonPrimaryRelationships,
        loggedInUserInfo)

      relationship.transferorInformation shouldBe expectedTransferorInformation
    }

    "return Transferor Information from loggedInUser if role is Transferor" in {
      val expectedTimestamp = loggedInUserInfo.timestamp
      val expectedTransferorInformation = TransferorInformation(expectedTimestamp)
      val relationship = RelationshipRecords(primaryTransferorRelationship,
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

      val relationship = RelationshipRecords(relationshipRecordList, localDate)

      relationship shouldBe RelationshipRecords(primaryRecipientRelationship, nonPrimaryRelationships, loggedInUserInfo)
    }

    "populate RelationshipRecord when non-primary records aren't present" in {
      val relationshipRecordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord),
        Some(loggedInUserInfo))
      val nonPrimaryRelationship = Seq()

      val relationship = RelationshipRecords(relationshipRecordList, localDate)

      relationship shouldBe RelationshipRecords(primaryRecipientRelationship, nonPrimaryRelationship, loggedInUserInfo)
    }

    "return a NoPrimaryError when no primary Record is found" in {
      val relationshipRecordList = RelationshipRecordList(Seq(inactiveRecipientRelationshipRecord2, inactiveTransferorRelationshipRecord2),
        Some(loggedInUserInfo))

      val relationship = intercept[NoPrimaryRecordError](RelationshipRecords(relationshipRecordList, localDate))

      relationship shouldBe NoPrimaryRecordError()
    }

    "return a MultipleActiveRecordError when multiple primary Records are found" in {
      val relationshipRecordList = RelationshipRecordList(Seq(activeTransferorRelationshipRecord2, activeRecipientRelationshipRecord),
        Some(loggedInUserInfo))

      val relationship = intercept[MultipleActiveRecordError](RelationshipRecords(relationshipRecordList, localDate))

      relationship shouldBe MultipleActiveRecordError()
    }

    "return a CitizenNotFound error when non logged in user found" in {
      val relationshipRecordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord))

      val relationship = intercept[CitizenNotFound](RelationshipRecords(relationshipRecordList, localDate))

      relationship shouldBe CitizenNotFound()
    }
  }
}
