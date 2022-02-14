/*
 * Copyright 2022 HM Revenue & Customs
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
import utils.UnitSpec

import java.time.LocalDate

class RelationshipRecordTest extends UnitSpec {

  lazy val currentYear: Int = LocalDate.now().getYear
  lazy val nextYear: Int = currentYear + 1
  lazy val pastYear: Int = currentYear - 1
  lazy val futureDateTime: String = "" + nextYear + "0101"
  lazy val pastDateTime: String = "" + pastYear + "0101"
  val dateFormat = "yyyyMMdd"

  lazy val relationshipActiveRecordWithNoEndDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.value,
      "56787",
      "20130101",
      Some(DesRelationshipEndReason.Default),
      None,
      "",
      "")

  lazy val relationshipActiveRecordWithFutureValidDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.value,
      "56787",
      "20130101",
      Some(DesRelationshipEndReason.Default),
      Some(futureDateTime),
      "",
      "")

  lazy val relationshipActiveRecordWithPastValidDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.value,
      "56787",
      "20130101",
      Some(DesRelationshipEndReason.Default),
      Some(pastDateTime),
      "",
      "")

  "isActive" should {
    "return true" when {

      "there is no relationship end date" in {
        relationshipActiveRecordWithNoEndDate.isActive shouldBe true
      }

      "relationship end date is a future date" in {
        relationshipActiveRecordWithFutureValidDate.isActive shouldBe true
      }

    }

    "return false" when {

      "relationship end date is a past date" in {
        relationshipActiveRecordWithPastValidDate.isActive shouldBe false
      }

      "relationship end date is today" in {
        val relationshipEndDate = new DateTime().toString(dateFormat)
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(participant1EndDate = Some(relationshipEndDate))
        relationshipRecord.isActive shouldBe false
      }

    }
  }

  "overlappingTaxYears" should {
    "return a set of Tax Years" when {
      "participant endDate is in PastYear" in {
        val relationshipRecord = relationshipActiveRecordWithPastValidDate

        relationshipRecord.overlappingTaxYears shouldBe Set(2012, 2013, 2014, 2015, 2016, 2017, 2019, 2018, 2020)
      }

      "participant endDate is in FutureYear" in {
        val relationshipRecord = relationshipActiveRecordWithFutureValidDate

        relationshipRecord.overlappingTaxYears shouldBe Set(2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2021, 2020, 2022)
      }

      "participant startDate and endDate is in same year" in {
        val relationshipEndDate = "20200101"
        val relationshipStartDate = "20200102"
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(participant1EndDate = Some(relationshipEndDate),
          participant1StartDate = relationshipStartDate)

        relationshipRecord.overlappingTaxYears shouldBe Set(2019)
      }

      "particpantEndDate is not set" in {
        val relationshipRecord = relationshipActiveRecordWithNoEndDate

        relationshipRecord.overlappingTaxYears shouldBe Set(2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021)
      }

      "Return a set of years that ends with the start year of the previous tax year when a participant endReason is Divorce" in {
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(relationshipEndReason = Some(DesRelationshipEndReason.Divorce),
          participant1EndDate = Some("20190406"))

        relationshipRecord.overlappingTaxYears shouldBe Set(2012, 2013, 2014, 2015, 2016, 2017, 2018)
      }
    }
  }

}
