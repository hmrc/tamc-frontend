/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import models._
import org.scalatestplus.play.OneAppPerSuite
import services.EligibilityCalculatorService
import config.ApplicationConfig._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.TaxYearResolver

class MarriageAllowanceCalculatorTest extends UnitSpec with TaxYearResolver with OneAppPerSuite {

  "MarriageAllowanceCalculator" when {
    "England is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT+1
          val lowerEarnerIncome = 9000

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.check.unlike-benefit-as-couple-" + TaxYearResolver.currentTaxYear))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT+1
          val lowerEarnerIncome = 12000

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }

        "potential gain is less than £1" in {

          val higherEarnerIncome = PERSONAL_ALLOWANCE+1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose", Some(PERSONAL_ALLOWANCE))
        }
      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE+1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE-1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT))
        }
      }

      "Inform the user they are eligible for partial Marriage Allowance benefit" when {
        "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT-1))
        }

        "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT-1))
        }

        "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = PERSONAL_ALLOWANCE+5
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(1))
        }

        "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-40
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(230))
        }


        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = 12500
          val lowerEarnerIncome = 9000

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, England) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(130))
        }
      }
    }

    "Scotland is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT_SCOT+1
          val lowerEarnerIncome = 9000
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.check.unlike-benefit-as-couple-" + TaxYearResolver.currentTaxYear))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT_SCOT+1
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }

        "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = PERSONAL_ALLOWANCE+5
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult("eligibility.feedback.loose",Some(11850.0))
        }

      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE+1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE-1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT))
        }
      }

      "Inform the user they are eligible for partial Marriage Allowance benefit" when {
        "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(225))
        }

        "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(225))
        }

        "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-40
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(218))
        }

        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val incomes = List(
            (12500,9000,123),
            (12500,11000,59),
            (30000,11000,174)
          )

          incomes.foreach( income =>
          EligibilityCalculatorService.calculate(income._2, income._1, Scotland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(income._3))
          )
        }
      }
    }

    "Wales is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT_WALES+1
          val lowerEarnerIncome = 9000
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.check.unlike-benefit-as-couple-" + TaxYearResolver.currentTaxYear))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT_WALES+1
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }
      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE+1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE-1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT))
        }

        "Inform the user they are eligible for partial Marriage Allowance benefit" when {
          "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
            val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT-1))
          }

          "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
            val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT-1))
          }

          "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
            val higherEarnerIncome = PERSONAL_ALLOWANCE+5
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(1))
          }

          "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
            val higherEarnerIncome = RECIPIENT_ALLOWANCE-40
            val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

            EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, Wales) shouldBe
              EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(230))
          }
        }
      }
    }

    "Northern Ireland is selected" should {
      "Inform the user they are ineligible for marriage allowance" when {
        "The higher earners income is taxed at the higher rate tax band" in {

          val higherEarnerIncome = MAX_LIMIT_NORTHERN_IRELAND+1
          val lowerEarnerIncome = 9000
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }

        "The lower earners income is above personal allowance" in {

          val higherEarnerIncome = 30000
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.check.unlike-benefit-as-couple-" + TaxYearResolver.currentTaxYear))
        }

        "Both higher earners income is taxed at higher rate and lower earners income is above personal allowance" in {

          val higherEarnerIncome = MAX_LIMIT_NORTHERN_IRELAND+1
          val lowerEarnerIncome = PERSONAL_ALLOWANCE+1
          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear))
        }
      }

      "Inform the user they are eligible for maximum Marriage Allowance benefit" when {
        "The higher earners income is above recipient allowance and the lower earners income is below transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE+1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE-1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT))
        }
      }

      "Inform the user they are eligible for partial Marriage Allowance benefit" when {
        "The higher earners income is just below recipient allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT-1))
        }

        "The higher earners income is just below recipient allowance and the lower earners income is just above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-1
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT-1))
        }

        "The higher earners income is £5 above personal allowance and the lower earners income is exactly transferor allowance" in {
          val higherEarnerIncome = PERSONAL_ALLOWANCE+5
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(1))
        }

        "The higher earners income is above recipient allowance and the lower earners income is above transferor allowance" in {
          val higherEarnerIncome = RECIPIENT_ALLOWANCE-40
          val lowerEarnerIncome = TRANSFEROR_ALLOWANCE+1

          EligibilityCalculatorService.calculate(lowerEarnerIncome, higherEarnerIncome, NorthernIreland) shouldBe
            EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(230))
        }
      }
    }
  }

  "BandedIncome" when {

    "incomeChunker is called" must {

      "return appropriate chunked income for english tax payer" in {
        case class EnglishTestIncome(income: Int, basicRate: Int)

        val testCases = List(
          EnglishTestIncome(30000, 30000 - PERSONAL_ALLOWANCE),
          EnglishTestIncome(25000, 25000 - PERSONAL_ALLOWANCE),
          EnglishTestIncome(20000, 20000 - PERSONAL_ALLOWANCE)
        )

        testCases.foreach(test =>

          BandedIncome.incomeChunker(
            test.income, England) shouldBe EnglishBandedIncome(test.basicRate)
        )
      }

      "return appropriately chunked income for scottish tax payer" in {

        case class ScottishTestIncome(income: Int, starterRate: Int, basicRate: Int, intermediateRate: Int)

        val testCases = List(
          ScottishTestIncome(30000, 2000, 10150, 6000),
          ScottishTestIncome(25000, 2000, 10150, 1000),
          ScottishTestIncome(20000, 2000, 6150, 0)
        )

        testCases.foreach(test =>

          BandedIncome.incomeChunker(
            test.income, Scotland) shouldBe ScottishBandedIncome(test.starterRate, test.basicRate, test.intermediateRate)
        )
      }

    }
  }
}
