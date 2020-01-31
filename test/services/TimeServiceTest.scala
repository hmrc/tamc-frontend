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

package services

import controllers.ControllerBaseSpec
import models.EndRelationshipReason
import org.joda.time.LocalDate
import uk.gov.hmrc.time.TaxYear

import scala.collection.immutable

class TimeServiceTest extends ControllerBaseSpec {

  private val currentYear = service.getCurrentDate.getYear
  private val years: immutable.Seq[Int] = (currentYear - 2 to currentYear + 3).toList

  def service: TimeService = TimeService

  "getEffectiveUntilDate" should {
    "return end of current tax year" when {
      "End reason code is CANCEL" in {
        val timeService = service
        val data = EndRelationshipReason("CANCEL")
        val result = timeService.getEffectiveUntilDate(data)
        result shouldBe Some(TaxYear.current.finishes)
      }
    }

    "return end tax year for divorce date" when {
      "End reason code is DIVORCE_CY" in {
        val timeService = service
        val data = EndRelationshipReason("DIVORCE_CY", Some(LocalDate.now()))
        val result = timeService.getEffectiveUntilDate(data)
        result shouldBe Some(TaxYear.taxYearFor(data.dateOfDivorce.get).finishes)
      }
    }

    "return None" when {
      "End reason code is DIVORCE_PY" in {
        val timeService = service
        val data = EndRelationshipReason("DIVORCE_PY")
        val result = timeService.getEffectiveUntilDate(data)
        result shouldBe None
      }
    }
  }

  "getEffectiveDate" should {
    "return startOfNextTaxYear" when {
      "End reason code is CANCEL" in {
        val timeService = service
        val data = EndRelationshipReason("CANCEL")
        timeService.getEffectiveDate(data) shouldBe TaxYear.current.next.starts
      }
    }

    "return end of tax year of divorce date" when {
      "End reason code is DIVORCE_CY" in {
        val timeService = service
        val date = new LocalDate(2018, 9, 1)
        val data = EndRelationshipReason("DIVORCE_CY", Some(new LocalDate(2019, 1, 1)))
        timeService.getEffectiveDate(data) shouldBe TaxYear.taxYearFor(date).finishes.plusDays(1)
      }
    }

    "return start of tax year of divorce date" when {
      "End reason code is DIVORCE_PY" in {
        val timeService = service
        val data = EndRelationshipReason("DIVORCE_PY", Some(LocalDate.now().minusDays(1)))
        timeService.getEffectiveDate(data) shouldBe TaxYear.taxYearFor(data.dateOfDivorce.get).starts
      }
    }
  }

  "getPreviousYearDate" should {
    "return previous year" in {
      service.getPreviousYearDate shouldBe LocalDate.now().minusYears(1)
    }
  }

  "getTaxYearForDate" should {
    "return current year before 6th April" in {
      val expectedYear = 2016
      val year = 2017
      val month = 4
      val day = 5
      val expected = new LocalDate(year, month, day)
      service.getTaxYearForDate(expected) shouldBe expectedYear
    }

    "return current year after 6th April" in {
      val expectedYear = 2017
      val year = 2017
      val month = 4
      val day = 6
      val expected = new LocalDate(year, month, day)
      service.getTaxYearForDate(expected) shouldBe expectedYear
    }
  }

  "parseDateWithFormat" should {

    "parse date with default format" in {
      val year = 2017
      val month = 10
      val day = 22
      val expected = new LocalDate(year, month, day)

      val date: String = "" + year + month + day

      service.parseDateWithFormat(date.trim) shouldBe expected
    }

    val formats = List[String]("-yyyyMMdd", " yyyyMMdd")
    for (format <- formats) {
      s"parse date with custom format of '$format'" in {
        val year = 2017
        val month = 10
        val day = 22
        val expected = new LocalDate(year, month, day)

        val prefix: String = format.substring(0, 1)
        val date: String = "" + prefix + year + month + day

        service.parseDateWithFormat(date.trim, format.trim) shouldBe expected
      }
    }
  }

  "isFutureDate" should {

    "future date is today" in {
      val timeService = service
      val data = LocalDate.now()
      timeService.isFutureDate(data) shouldBe false
    }

    "future date is yesterday" in {
      val timeService = service
      val data = LocalDate.now().minusDays(1)
      timeService.isFutureDate(data) shouldBe false
    }

    "future date is tomorrow" in {
      val timeService = service
      val data = LocalDate.now().plusDays(1)
      timeService.isFutureDate(data) shouldBe true
    }

  }

  "getCurrentTaxYear" should {

    "get current tax year" in {
      val timeService = service
      val data = TaxYear.current.startYear
      timeService.getCurrentTaxYear shouldBe data
    }

  }

  "getStartDateForTaxYear" should {

    for (year <- years) {
      s"start date of tax year is $year April 6th" in {
        val timeService = service
        val expected = new LocalDate(year, 4, 6)
        timeService.getStartDateForTaxYear(year) shouldBe expected
      }
    }
  }


}
