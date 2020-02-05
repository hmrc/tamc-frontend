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

import java.time.LocalDate

import org.joda.time.DateTime
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import services.TimeService
import uk.gov.hmrc.play.test.UnitSpec

class RelationshipRecordTest extends UnitSpec with GuiceOneAppPerSuite {

  lazy val currentYear: Int = LocalDate.now().getYear
  lazy val nextYear: Int = currentYear + 1
  lazy val pastYear: Int = currentYear - 1
  lazy val futureDateTime: String = "" + nextYear + "0101"
  lazy val pastDateTime: String = "" + pastYear + "0101"

  lazy val relationshipActiveRecordWitNoEndDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.asString(),
      "56787",
      "20130101",
      Some(RelationshipEndReason.Default),
      None,
      "",
      "")

  lazy val relationshipActiveRecordWitFutureInvalidDate: RelationshipRecord =
    RelationshipRecord(
      participant = Recipient.asString(),
      creationTimestamp = "56787",
      participant1StartDate = "20130101",
      relationshipEndReason = Some(RelationshipEndReason.Default),
      participant1EndDate = Some(currentYear.toString),
      otherParticipantInstanceIdentifier = "",
      otherParticipantUpdateTimestamp = "")

  lazy val relationshipActiveRecordWitFutureValidDate: RelationshipRecord =
    RelationshipRecord(
      participant = Recipient.asString(),
      creationTimestamp = "56787",
      participant1StartDate = "20130101",
      relationshipEndReason = Some(RelationshipEndReason.Default),
      participant1EndDate = Some(futureDateTime),
      otherParticipantInstanceIdentifier = "",
      otherParticipantUpdateTimestamp = "")

  lazy val relationshipActiveRecordWitPastValidDate: RelationshipRecord =
    RelationshipRecord(
      participant = Recipient.asString(),
      creationTimestamp = "56787",
      participant1StartDate = "20130101",
      relationshipEndReason = Some(RelationshipEndReason.Default),
      participant1EndDate = Some(pastDateTime),
      otherParticipantInstanceIdentifier = "",
      otherParticipantUpdateTimestamp = "")

  "isActive" should {
    "return true" when {

      "there is no relationship end date" in {
        relationshipActiveRecordWitNoEndDate.isActive shouldBe true
      }

      "relationship end date is a future date" in {
        relationshipActiveRecordWitFutureValidDate.isActive shouldBe true
      }

    }

    "return false" when {

      "relationship end date is a past date" in {
        relationshipActiveRecordWitPastValidDate.isActive shouldBe false
      }

      "relationship end date is invalid format date" in {
        relationshipActiveRecordWitFutureInvalidDate.isActive shouldBe false
      }

      "relationship end date is today" in {
        val relationshipEndDate = new DateTime().toString(TimeService.defaultDateFormat)
        val relationshipRecord = relationshipActiveRecordWitNoEndDate.copy(participant1EndDate = Some(relationshipEndDate))
        relationshipRecord.isActive shouldBe false
      }

    }
  }

  //TODO add test for this method
  "overlappingTaxYears" should {

  }

}
