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

package services

import models.{Recipient, Transferor}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.time.TaxYear
import utils.UnitSpec

import java.time.LocalDate

class EndDateDivorceCalculatorTest extends UnitSpec with GuiceOneAppPerSuite {

  val endDateDivorceCalculator = app.injector.instanceOf[EndDateDivorceCalculator]
  val recipientEndDateCalculator = endDateDivorceCalculator.calculateEndDate(Recipient, _: LocalDate)
  val transferorEndDateCalculator = endDateDivorceCalculator.calculateEndDate(Transferor, _:LocalDate)
  val currentTaxYear = TaxYear.current
  val currentTaxYearDate = currentTaxYear.starts
  val previousTaxYearDate = currentTaxYear.previous.finishes

  def startOfNextGivenTaxYear(taxYear: LocalDate) = TaxYear.taxYearFor(taxYear).next.starts

  "EndDateDivorceCalculatorTest" should {
    "return the correct marriageAllowance end date" when {
      "a Recipient cancels marriage allowance" when {
        "the divorce date is in the current tax year" in {

          val marriageAllowanceEndingDate = recipientEndDateCalculator(currentTaxYearDate)
          marriageAllowanceEndingDate shouldBe currentTaxYear.finishes
        }

        "the divorce date is in the previous tax year" in {

          val marriageAllowanceEndingDate = recipientEndDateCalculator(previousTaxYearDate)
          marriageAllowanceEndingDate shouldBe currentTaxYear.previous.finishes
        }
      }

      "a Transferor cancels marriage allowance" when {
        "the divorce date is in the current tax year" in {

          val marriageAllowanceEndingDate = transferorEndDateCalculator(currentTaxYearDate)
          marriageAllowanceEndingDate shouldBe currentTaxYear.previous.finishes
        }

        "the divorce date is in the previous tax year" in {

          val marriageAllowanceEndingDate = transferorEndDateCalculator(previousTaxYearDate)
          marriageAllowanceEndingDate shouldBe TaxYear.taxYearFor(previousTaxYearDate).finishes
        }
      }
    }

    "return the correct marriageAllowance personal allowance effective date" when {
      "the marriage allowance end date is in the current tax year" in {

        val marriageAllowanceEndingDate = endDateDivorceCalculator.calculatePersonalAllowanceEffectiveDate(currentTaxYearDate)
        marriageAllowanceEndingDate shouldBe startOfNextGivenTaxYear(currentTaxYearDate)
      }

      "the marriage allowance end date is in the previous tax year" in {

        val marriageAllowanceEndingDate =endDateDivorceCalculator.calculatePersonalAllowanceEffectiveDate(previousTaxYearDate)
        marriageAllowanceEndingDate shouldBe startOfNextGivenTaxYear(previousTaxYearDate)
      }
    }
  }
}