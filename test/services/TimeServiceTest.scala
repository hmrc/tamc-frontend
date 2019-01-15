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
import uk.gov.hmrc.time.TaxYearResolver
import org.mockito.Mockito._

class TimeServiceTest extends ControllerBaseSpec {

  def service: TimeService = new TimeService {
    override val taxYearResolver: TaxYearResolver = mock[TaxYearResolver]
    when(taxYearResolver.endOfCurrentTaxYear).thenReturn(LocalDate.now.plusDays(1))
    when(taxYearResolver.startOfCurrentTaxYear).thenReturn(LocalDate.now())
    when(taxYearResolver.currentTaxYear).thenReturn(2018)
    when(taxYearResolver.startOfNextTaxYear).thenReturn(LocalDate.now().plusDays(5))
  }

  "getEffectiveUntilDate" should {
    "return end of current tax year" when{
      "End reason code is CANCEL" in {
        val timeService = service
        val result = timeService.getEffectiveUntilDate(EndRelationshipReason("CANCEL"))
        result shouldBe Some(LocalDate.now.plusDays(1))
        verify(timeService.taxYearResolver, times(1)).endOfCurrentTaxYear
      }
    }

    "return end tax year for divorce date" when {
      "End reason code is DIVORCE_CY" in {
        val timeService = service
        val date = LocalDate.now().plusDays(2)
        val data = EndRelationshipReason("DIVORCE_CY", Some(LocalDate.now()))
        when(timeService.taxYearResolver.taxYearFor(data.dateOfDivorce.get))
          .thenReturn(2018)
        when(timeService.taxYearResolver.endOfTaxYear(2018)).thenReturn(date)
        val result = timeService.getEffectiveUntilDate(data)
        result shouldBe Some(LocalDate.now().plusDays(2))
        verify(timeService.taxYearResolver, times(1)).taxYearFor(LocalDate.now())
      }
    }

    "return None" when {
      "End reason code is DIVORCE_PY" in {
        val timeService = service
        val data = EndRelationshipReason("DIVORCE_PY")
        val result = timeService.getEffectiveUntilDate(data)
        result shouldBe None
        verifyZeroInteractions(service.taxYearResolver)
      }
    }
  }

  "getEffectiveDate" should {
    "return startOfNextTaxYear" when {
      "End reason code is CANCEL" in {
        val timeService = service
        val data = EndRelationshipReason("CANCEL")
        timeService.getEffectiveDate(data) shouldBe LocalDate.now().plusDays(5)
        verify(timeService.taxYearResolver, times(1)).startOfNextTaxYear
      }
    }

    "return end of tax year of divorce date" when {
      "End reason code is DIVORCE_CY" in {
        val timeService = service
        val date = LocalDate.now().plusDays(2)
        val data = EndRelationshipReason("DIVORCE_CY", Some(LocalDate.now()))
        when(timeService.taxYearResolver.taxYearFor(data.dateOfDivorce.get))
          .thenReturn(2018)
        when(timeService.taxYearResolver.endOfTaxYear(2018)).thenReturn(date)
        timeService.getEffectiveDate(data) shouldBe date.plusDays(1)
        verify(timeService.taxYearResolver, times(1)).taxYearFor(LocalDate.now())
      }
    }

    "return start of tax year of divorce date" when {
      "End reason code is DIVORCE_PY" in {
        val timeService = service
        val data = EndRelationshipReason("DIVORCE_PY", Some(LocalDate.now().minusDays(1)))
        when(timeService.taxYearResolver.taxYearFor(data.dateOfDivorce.get))
          .thenReturn(2017)
        when(timeService.taxYearResolver.startOfTaxYear(2017))
          .thenReturn(LocalDate.now().minusDays(2))
        timeService.getEffectiveDate(data) shouldBe LocalDate.now().minusDays(2)
      }
    }
  }

  "getPreviousYearDate" should {
    "return previous year" in {
      service.getPreviousYearDate() shouldBe LocalDate.now().minusYears(1)
    }
  }

  "parseDateWtihFormat" should {
    "parse date with format of yyyyMMdd" in {
      val format = "yyyyMMdd"
      val date = "20170822"
      service.parseDateWtihFormat(date, format) shouldBe new LocalDate(2017, 8, 22)
    }
  }
}
