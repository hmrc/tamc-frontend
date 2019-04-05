/*
 * Copyright 2019 HM Revenue & Customs
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

class TimeServiceTest extends ControllerBaseSpec {

  def service: TimeService = TimeService

  "getEffectiveUntilDate" should {
    "return end of current tax year" when{
      "End reason code is CANCEL" in {
        val timeService = service
        val result = timeService.getEffectiveUntilDate(EndRelationshipReason("CANCEL"))
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

  "parseDateWtihFormat" should {
    "parse date with format of yyyyMMdd" in {
      val format = "yyyyMMdd"
      val date = "20170822"
      service.parseDateWithFormat(date, format) shouldBe new LocalDate(2017, 8, 22)
    }
  }
}
