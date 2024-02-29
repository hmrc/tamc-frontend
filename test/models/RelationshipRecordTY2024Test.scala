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

import utils.BaseTest

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, LocalTime}

class RelationshipRecordTY2024Test extends BaseTest {

  val testYear: Int = 2024

  val testTaxYear: uk.gov.hmrc.time.TaxYear = uk.gov.hmrc.time.TaxYear(testYear)
  val testDate: LocalDate = LocalDate.of(testYear, 6, 6)
  val testDateTime = LocalDateTime.of(testDate, LocalTime.of(1, 1, 1))

  val nextYear: Int = testYear + 1
  val pastYear: Int = testYear - 1
  val futureDateTime: String = "" + nextYear + "0101"
  val pastDateTime: String = "" + pastYear + "0101"
  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

  val relationshipActiveRecordWithNoEndDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.value,
      "56787",
      s"${testTaxYear.startYear - 8}0101",
      Some(DesRelationshipEndReason.Default),
      None,
      "",
      "")

  val relationshipActiveRecordWithFutureValidDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.value,
      "56787",
      "20130101",
      Some(DesRelationshipEndReason.Default),
      Some(futureDateTime),
      "",
      "")

  val relationshipActiveRecordWithPastValidDate: RelationshipRecord =
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
        relationshipActiveRecordWithNoEndDate.isActive(testDate) shouldBe true
      }

      "relationship end date is a future date" in {
        relationshipActiveRecordWithFutureValidDate.isActive(testDate) shouldBe true
      }

    }

    "return false" when {

      "relationship end date is a past date" in {
        relationshipActiveRecordWithPastValidDate.isActive(testDate) shouldBe false
      }

      "relationship end date is today" in {
        val relationshipEndDate = dateFormat.format(testDateTime)
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(
          participant1EndDate = Some(relationshipEndDate)
        )

        relationshipRecord.isActive(testDate) shouldBe false
      }

    }
  }

  "overlappingTaxYears" should {
    "return a set of Tax Years" when {
      "participant endDate is in PastYear" in {
        val relationshipRecord = relationshipActiveRecordWithPastValidDate

        relationshipRecord.overlappingTaxYears(testTaxYear.startYear) shouldBe Set(2014, 2020, 2017, 2015, 2016, 2022, 2012, 2013, 2019, 2021, 2018)
      }

      "participant endDate is in FutureYear" in {
        val relationshipRecord = relationshipActiveRecordWithFutureValidDate

        relationshipRecord.overlappingTaxYears(testTaxYear.startYear) shouldBe Set(2014, 2020, 2024, 2017, 2015, 2023, 2016, 2022, 2012, 2013, 2019, 2021, 2018)
      }

      "participant startDate and endDate is in same year" in {
        val relationshipEndDate = "20200101"
        val relationshipStartDate = "20200102"
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(
          participant1EndDate = Some(relationshipEndDate),
          participant1StartDate = relationshipStartDate
        )

        relationshipRecord.overlappingTaxYears(testTaxYear.startYear) shouldBe Set(2019)
      }

      "particpantEndDate is not set" in {
        val relationshipRecord = relationshipActiveRecordWithNoEndDate

        val range: Seq[Int] = testTaxYear.startYear - 9 to testTaxYear.startYear

        relationshipRecord.overlappingTaxYears(testTaxYear.startYear) shouldBe range.toSet
      }

      "Return a set of years that ends with the start year of the previous tax year when a participant endReason is Divorce" in {
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(
          relationshipEndReason = Some(DesRelationshipEndReason.Divorce),
          participant1EndDate = Some(s"${testTaxYear.startYear - 3}0406")
        )

        val range: Seq[Int] = testTaxYear.startYear - 9 to testTaxYear.startYear - 4

        relationshipRecord.overlappingTaxYears(testTaxYear.startYear) shouldBe range.toSet
      }
    }
  }
}
