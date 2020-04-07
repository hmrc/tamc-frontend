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

import org.joda.time.LocalDate
import uk.gov.hmrc.time.TaxYear
import utils.BaseTest

import scala.collection.immutable

class TimeServiceTest extends BaseTest {

  def timeService: TimeService = TimeService

  private val currentYear = timeService.getCurrentDate.getYear
  private val years: immutable.Seq[Int] = (currentYear - 2 to currentYear + 3).toList

  "getTaxYearForDate" should {
    "return current year before 6th April" in {
      val expectedYear = 2016
      val year = 2017
      val month = 4
      val day = 5
      val expected = new LocalDate(year, month, day)
      timeService.getTaxYearForDate(expected) shouldBe expectedYear
    }

    "return current year after 6th April" in {
      val expectedYear = 2017
      val year = 2017
      val month = 4
      val day = 6
      val expected = new LocalDate(year, month, day)
      timeService.getTaxYearForDate(expected) shouldBe expectedYear
    }
  }

  "parseDateWithFormat" should {

    "parse date with default format" in {
      val year = 2017
      val month = 10
      val day = 22
      val expected = new LocalDate(year, month, day)

      val date: String = "" + year + month + day

      timeService.parseDateWithFormat(date.trim) shouldBe expected
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

        timeService.parseDateWithFormat(date.trim, format.trim) shouldBe expected
      }
    }
  }

  "isFutureDate" should {

    "future date is today" in {
      val data = LocalDate.now()
      timeService.isFutureDate(data) shouldBe false
    }

    "future date is yesterday" in {
      val data = LocalDate.now().minusDays(1)
      timeService.isFutureDate(data) shouldBe false
    }

    "future date is tomorrow" in {
      val data = LocalDate.now().plusDays(1)
      timeService.isFutureDate(data) shouldBe true
    }

  }

  "getCurrentTaxYear" should {

    "get current tax year" in {
      val data = TaxYear.current.startYear
      timeService.getCurrentTaxYear shouldBe data
    }

  }

  "getStartDateForTaxYear" should {

    for (year <- years) {
      s"start date of tax year is $year April 6th" in {
        val expected = new LocalDate(year, 4, 6)
        timeService.getStartDateForTaxYear(year) shouldBe expected
      }
    }
  }

  "getValidYearsApplyMAPreviousYears" should {

    "return empty list if none is passed" in {
      timeService.getValidYearsApplyMAPreviousYears(None) should have size(0)
    }

    "return empty list if empty list is passed" in {
      timeService.getValidYearsApplyMAPreviousYears(Some(List[models.TaxYear]())) should have size(0)
    }

    "return empty list if years < than minim allowed is passed" in {
      val list = List(models.TaxYear(year = 2002))
      timeService.getValidYearsApplyMAPreviousYears(Some(list)) should have size(0)
    }

    "return valid list if years > than minim allowed is passed" in {
      val year = timeService.getCurrentDate.getYear
      val from = year - 10
      val to = year + 5
      val list: List[models.TaxYear] = (from to to).map(year => {
        models.TaxYear(year)
      }).toList
      timeService.getValidYearsApplyMAPreviousYears(Some(list)) should have size(10)
    }
  }

}
