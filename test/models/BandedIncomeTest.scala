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

package models

import _root_.services.EligibilityCalculatorService
import config.ApplicationConfig._
import org.scalatestplus.play.OneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.TaxYearResolver

class BandedIncomeTest extends UnitSpec with TaxYearResolver with OneAppPerSuite {

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
            test.income, England, EligibilityCalculatorService.getCountryTaxBandsFromFile(England)) shouldBe EnglishBandedIncome(test.basicRate, bands = List(TaxBand("BasicRate", 11851,46350,0.20)))
        )
      }

      "return appropriately chunked income for scottish tax payer" in {
        case class ScottishTestIncome(income: Int, starterRateIncome: Int, basicRateIncome: Int, intermediateRateIncome: Int)

        val testCases = List(
          ScottishTestIncome(30000, 2000, 10150, 6000),
          ScottishTestIncome(25000, 2000, 10150, 1000),
          ScottishTestIncome(20000, 2000, 6150, 0)
        )

        testCases.foreach(test =>

          BandedIncome.incomeChunker(
            test.income, Scotland, EligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland)) shouldBe ScottishBandedIncome(test.starterRateIncome, test.basicRateIncome, test.intermediateRateIncome, EligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland))
        )
      }
    }

    "getCountryTaxBandsFromFile is called" must {
      "return the correct banded income for an English tax payer" in {
        EligibilityCalculatorService.getCountryTaxBandsFromFile(England) shouldBe
          List(TaxBand("BasicRate",PERSONAL_ALLOWANCE + 1, MAX_LIMIT,0.20))
      }

//      "return the correct banded income for a Scottish tax payer" in {
//        EligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland) shouldBe
//          List(TaxBand("StarterRate",PERSONAL_ALLOWANCE + 1, MAX_LIMIT_SCOT,0.19),
//          TaxBand("BasicRate",PERSONAL_ALLOWANCE + 1, MAX_LIMIT_SCOT,0.20),
//          TaxBand("IntermediateRate",PERSONAL_ALLOWANCE + 1, MAX_LIMIT_SCOT,0.21))
//      }

      "return the correct banded income for a Welsh tax payer" in {
        EligibilityCalculatorService.getCountryTaxBandsFromFile(Wales) shouldBe
          List(TaxBand("BasicRate",PERSONAL_ALLOWANCE + 1, MAX_LIMIT_WALES,0.20))
      }

      "return the correct banded income for a Northern Irish tax payer" in {
        EligibilityCalculatorService.getCountryTaxBandsFromFile(NorthernIreland) shouldBe
          List(TaxBand("BasicRate",PERSONAL_ALLOWANCE + 1, MAX_LIMIT_NORTHERN_IRELAND,0.20))
      }
    }
  }
}
