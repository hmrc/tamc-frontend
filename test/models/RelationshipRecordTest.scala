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

import java.time.format.DateTimeFormatter

import config.ApplicationConfig
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.{BaseTest, SystemLocalDate}
import java.time.{LocalDate, LocalDateTime}

class RelationshipRecordTest extends BaseTest with GuiceOneAppPerSuite {

  lazy val currentYear: Int = LocalDate.now().getYear
  lazy val futureDateTime: String = s"${currentTaxYear.finishYear}0504"
  lazy val pastDateTime: String = s"${currentTaxYear.startYear}0604"
  lazy val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
  lazy val firstTaxYearOfMarriage: Int = 2012

  lazy val applicationConfig : ApplicationConfig = instanceOf[ApplicationConfig]
  lazy val currentTaxYear: uk.gov.hmrc.time.TaxYear = applicationConfig.currentTaxYear()
  lazy val localDate: LocalDate = instanceOf[SystemLocalDate].now()

  lazy val relationshipActiveRecordWithNoEndDate: RelationshipRecord =
    RelationshipRecord(
      Recipient.value,
      "56787",
      s"${currentTaxYear.startYear-8}0101",
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
        relationshipActiveRecordWithNoEndDate.isActive(localDate) shouldBe true
      }

      "relationship end date is a future date" in {
        relationshipActiveRecordWithFutureValidDate.isActive(localDate) shouldBe true
      }

    }

    "return false" when {

      "relationship end date is a past date" in {
        relationshipActiveRecordWithPastValidDate.copy(participant1EndDate =
          Some(s"${currentTaxYear.startYear}0504")).isActive(localDate) shouldBe false
      }

      "relationship end date is today" in {
        val relationshipEndDate = dateFormat.format(LocalDateTime.now())
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(participant1EndDate = Some(relationshipEndDate))
        relationshipRecord.isActive(localDate) shouldBe false
      }

    }
  }

  "overlappingTaxYears" should {
    "return a set of Tax Years" when {
      "participant endDate is in PastYear" in {
        val relationshipRecord = relationshipActiveRecordWithPastValidDate

        relationshipRecord.overlappingTaxYears(currentTaxYear.startYear) shouldBe (firstTaxYearOfMarriage to currentTaxYear.startYear).toSet
      }

      "participant endDate is in FutureYear" in {
        val relationshipRecord = relationshipActiveRecordWithFutureValidDate

        relationshipRecord.overlappingTaxYears(currentTaxYear.startYear) shouldBe (firstTaxYearOfMarriage to currentTaxYear.finishYear).toSet
      }

      "participant startDate and endDate is in same year" in {
        val relationshipEndDate = "20200101"
        val relationshipStartDate = "20200102"
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(participant1EndDate = Some(relationshipEndDate),
          participant1StartDate = relationshipStartDate)

        relationshipRecord.overlappingTaxYears(currentTaxYear.startYear) shouldBe Set(2019)
      }

      "particpantEndDate is not set" in {
        val relationshipRecord = relationshipActiveRecordWithNoEndDate

        val range: Seq[Int] = currentTaxYear.startYear-9 to currentTaxYear.startYear

        relationshipRecord.overlappingTaxYears(currentTaxYear.startYear) shouldBe range.toSet
      }

      "Return a set of years that ends with the start year of the previous tax year when a participant endReason is Divorce" in {
        val relationshipRecord = relationshipActiveRecordWithNoEndDate.copy(relationshipEndReason = Some(DesRelationshipEndReason.Divorce),
          participant1EndDate = Some(s"${currentTaxYear.startYear-3}0406"))

        val range: Seq[Int] = currentTaxYear.startYear-9 to currentTaxYear.startYear-4

        relationshipRecord.overlappingTaxYears(currentTaxYear.startYear) shouldBe range.toSet
      }
    }
  }

}
