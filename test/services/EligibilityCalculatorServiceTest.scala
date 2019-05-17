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

import java.text.NumberFormat
import java.util.Locale

import config.ApplicationConfig._
import controllers.ControllerBaseSpec
import models._
import uk.gov.hmrc.time

class EligibilityCalculatorServiceTest extends ControllerBaseSpec {

  val currentTaxYear: Int = time.TaxYear.current.startYear

  private def currencyFormatter(limit: Int): String = {
    val formatter = NumberFormat.getCurrencyInstance(Locale.UK)
    formatter.setMaximumFractionDigits(0)
    formatter.format(limit)
  }

  "MarriageAllowanceCalculator" when {
    "England is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT() + 1
          val lowerEarnerIncome = 9000

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT())))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.check.unlike-benefit-as-couple",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE())))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT() + 1
          val lowerEarnerIncome = 12000

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT())))
        }

        "potential gain is less than £1" in {

          val higherEarnerIncome = PERSONAL_ALLOWANCE() + 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose")
        }
      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE + 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE - 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT()))
        }
      }

      "Inform the user they are eligible for partial Marriage Allowance benefit" when {
        "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT() - 1))
        }

        "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT() - 1))
        }

        "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = PERSONAL_ALLOWANCE() + 5
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(1))
        }

        "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 40
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(242.0))
        }


        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = 14000
          val lowerEarnerIncome = 9000

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(250.0))
        }
      }
    }

    "Scotland is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT_SCOT() + 1
          val lowerEarnerIncome = 9000
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT_SCOT())))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.check.unlike-benefit-as-couple",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE())))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT_SCOT() + 1
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT_SCOT())))
        }

        "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = PERSONAL_ALLOWANCE() + 5
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult("eligibility.feedback.loose")
        }

      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE + 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE - 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT()))
        }
      }

      "Inform the user they are eligible for partial Marriage Allowance benefit" when {
        "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(237.0))
        }

        "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(237.0))
        }

        "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 40
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(229.0))
        }

        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val incomes = List(
            (14000, 9000, 250),
            (14000, 11000, 250),
            (30000, 11000, 250)
          )

          incomes.foreach(income =>
            EligibilityCalculatorService.calculate(income._2, income._1, Scotland) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(income._3))
          )
        }
      }
    }

    "Wales is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT_WALES() + 1
          val lowerEarnerIncome = 9000
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT_WALES())))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.check.unlike-benefit-as-couple",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE())))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT_WALES() + 1
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT_WALES())))
        }
      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE + 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE - 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT()))
        }

        "Inform the user they are eligible for partial Marriage Allowance benefit" when {
          "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
            val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT() - 1))
          }

          "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
            val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT() - 1))
          }

          "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
            val higherEarnerIncome = PERSONAL_ALLOWANCE() + 5
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(1))
          }

          "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
            val higherEarnerIncome = RECIPIENT_ALLOWANCE - 40
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(242.0))
          }
        }
      }
    }

    "Northern Ireland is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT_NORTHERN_IRELAND() + 1
          val lowerEarnerIncome = 9000
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT_NORTHERN_IRELAND())))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.check.unlike-benefit-as-couple", messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE())))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT_NORTHERN_IRELAND() + 1
          val lowerEarnerIncome = PERSONAL_ALLOWANCE() + 1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
              messageParam = Some(currencyFormatter(PERSONAL_ALLOWANCE() + 1)), messageParam2 = Some(currencyFormatter(MAX_LIMIT_NORTHERN_IRELAND())))
        }
      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE + 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE - 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT()))
        }
      }

      "Inform the user they are eligible for partial Marriage Allowance benefit" when {
        "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT() - 1))
        }

        "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT() - 1))
        }

        "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = PERSONAL_ALLOWANCE() + 5
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(1))
        }

        "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE - 40
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE + 1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(242.0))
        }
      }
    }
  }
}
