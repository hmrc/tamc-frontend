/*
 * Copyright 2021 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import java.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.TaxYear

class EndDateForMACeasedTest extends UnitSpec with GuiceOneAppPerSuite{

  val endDateForMACeased = app.injector.instanceOf[EndDateForMACeased]
  val currentTaxYear = TaxYear.current

  def startOfNextGivenTaxYear(taxYear: LocalDate) = TaxYear.taxYearFor(taxYear).next.starts

  "EndDateMACeasedCalculatorTest" should {

    "return a marriage allowance end date" in {
      endDateForMACeased.endDate shouldBe currentTaxYear.finishes
    }

    "return a personal allowance effective date" in {
      endDateForMACeased.personalAllowanceEffectiveDate shouldBe currentTaxYear.next.starts
    }
  }
}
