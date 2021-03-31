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

package utils

import config.ApplicationConfig.appConfig._
import models._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import _root_.services.EligibilityCalculatorService
import uk.gov.hmrc.play.test.UnitSpec

class BenefitCalculatorHelperTest extends UnitSpec with GuiceOneAppPerSuite {

  val eligibilityCalculatorService = app.injector.instanceOf[EligibilityCalculatorService]

  "BenefitCalculatorHelper" when {

    "calculateTotalBenefitAcrossBands" must {

      "return the correct total benefit for an English tax payer" in {
        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(30650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(England)) shouldBe MAX_BENEFIT()

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(England)) shouldBe 130

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(England)) shouldBe 230
      }

      "return the correct total benefit for a Scottish tax payer" in {
        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(30650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland)) shouldBe MAX_BENEFIT()

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland)) shouldBe 123

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland)) shouldBe 218
      }

      "return the correct total benefit for a Scottish tax payer when hypothetical bands are used" in {
        val hypotheticalBandsOne = List(
          TaxBand("StarterRate", 11851, 12050, 0.19), TaxBand("BasicRate", 12051, 24000, 0.20), TaxBand("IntermediateRate", 24001, 43430, 0.21))

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(), hypotheticalBandsOne) shouldBe 128

        val hypotheticalBandsTwo = List(
          TaxBand("StarterRate", 11851, 13850, 0.10), TaxBand("BasicRate", 13851, 24000, 0.21), TaxBand("IntermediateRate", 24001, 43430, 0.22))

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(), hypotheticalBandsTwo) shouldBe 65

        val hypotheticalBandsThree = List(
          TaxBand("StarterRate", 11851, 12050, 0.10), TaxBand("BasicRate", 12051, 22050, 0.15), TaxBand("FakeRate1", 22051, 27050, 0.20),
          TaxBand("FakeRate2", 27051, 32050, 0.25))

        val personOne = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(30650 - PERSONAL_ALLOWANCE(), hypotheticalBandsThree)
        val personTwo = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(11650 - TRANSFEROR_ALLOWANCE, hypotheticalBandsThree)
        personOne - personTwo shouldBe 137

        val personThree = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(), hypotheticalBandsThree)
        val personFour = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(11650 - TRANSFEROR_ALLOWANCE, hypotheticalBandsThree)
        personThree - personFour shouldBe 37
      }

      "return the correct total benefit for a Welsh tax payer" in {
        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(30650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(Wales)) shouldBe MAX_BENEFIT()

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(Wales)) shouldBe 130

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(Wales)) shouldBe 230
      }

      "return the correct total benefit for a Northern Irish tax payer" in {
        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(30650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(NorthernIreland)) shouldBe MAX_BENEFIT()

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(NorthernIreland)) shouldBe 130

        BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(13650 - PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBandsFromFile(NorthernIreland)) shouldBe 230
      }

    }

    "getCountryTaxBandsFromFile is called" must {
      "return the correct banded income for an English tax payer" in {
        eligibilityCalculatorService.getCountryTaxBandsFromFile(England) shouldBe
        //TODO remove hard coded and repace with dynamic after new tax year 2021/22 started
        // List(TaxBand("BasicRate", PERSONAL_ALLOWANCE() + 1, MAX_LIMIT(), 0.20))
        List(TaxBand("BasicRate", 12570 + 1, 50270, 0.20))
      }

      "return the correct banded income for a Scottish tax payer" in {
        eligibilityCalculatorService.getCountryTaxBandsFromFile(Scotland) shouldBe
        //TODO add the dynamic values back in after 2021/22 tax year starts & remove the hard coded values
          //List(TaxBand("StarterRate", PERSONAL_ALLOWANCE() + 1, 14667, 0.19),
          List(TaxBand("StarterRate", 12570 + 1, 14667, 0.19),
            TaxBand("BasicRate", 14668, 25296, 0.20),
            //TaxBand("IntermediateRate", 25297, MAX_LIMIT_SCOT(), 0.21))
            TaxBand("IntermediateRate", 25297, 43662 , 0.21))
      }

      "return the correct banded income for a Welsh tax payer" in {
        eligibilityCalculatorService.getCountryTaxBandsFromFile(Wales) shouldBe
          //TODO add the dynamic values back in after 2021/22 tax year starts & remove the hard coded values
          //List(TaxBand("BasicRate", PERSONAL_ALLOWANCE() + 1, MAX_LIMIT_WALES(), 0.20))
          List(TaxBand("BasicRate", 12570 + 1, 50270, 0.20))
      }

      "return the correct banded income for a Northern Irish tax payer" in {
        eligibilityCalculatorService.getCountryTaxBandsFromFile(NorthernIreland) shouldBe
          //TODO add the dynamic values back in after 2021/22 tax year starts & remove the hard coded values
          //List(TaxBand("BasicRate", PERSONAL_ALLOWANCE() + 1, MAX_LIMIT_NORTHERN_IRELAND(), 0.20))
          List(TaxBand("BasicRate", 12570  + 1, 50270, 0.20))
      }
    }
  }
}