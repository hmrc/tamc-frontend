/*
 * Copyright 2024 HM Revenue & Customs
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

import _root_.services.EligibilityCalculatorService
import config.ApplicationConfig
import models.{England, NorthernIreland, Scotland, TaxBand, Wales}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.time.TaxYear

class BenefitCalculatorHelperTest extends BaseTest with GuiceOneAppPerSuite {


  val benefitCalculatorHelper: BenefitCalculatorHelper = instanceOf[BenefitCalculatorHelper]
  val eligibilityCalculatorService: EligibilityCalculatorService = instanceOf[EligibilityCalculatorService]
  lazy val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]
  lazy val currentTaxYear: TaxYear = applicationConfig.currentTaxYear()

  def requiredRounding: Int = {
    val adjustmentNeeded: Int = 2
    val noAdjustment: Int = 0

    if (applicationConfig.PERSONAL_ALLOWANCE() / 10 !== 0) adjustmentNeeded
    else noAdjustment
  }

  "BenefitCalculatorHelper" when {

    "calculateTotalBenefitAcrossBands" must {
      "return the correct total benefit for an English tax payer over the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.25) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(England, currentTaxYear)
        ) shouldBe applicationConfig.MAX_BENEFIT()
      }

      "return the correct total benefit for a English tax payer half way between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          ((applicationConfig.PERSONAL_ALLOWANCE() * 1.05 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE()).round,
          eligibilityCalculatorService.getCountryTaxBands(England, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT() * 0.5).toInt
      }

      "return the correct total benefit for a English tax payer 80% between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          ((applicationConfig.PERSONAL_ALLOWANCE() * 1.08 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE()).round,
          eligibilityCalculatorService.getCountryTaxBands(England, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT() * 0.8).toInt
      }

      "return the correct total benefit for a Scottish tax payer over the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.25) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(Scotland, currentTaxYear)
        ) shouldBe applicationConfig.MAX_BENEFIT()
      }

      "return the correct total benefit for a Scottish tax payer half way between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.05 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(Scotland, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT_SCOT * 0.5).toInt
      }

      "return the correct total benefit for a Scottish tax payer 80% between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.08 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(Scotland, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT_SCOT * 0.8).toInt
      }

      //TODO: DDCNL-9296: Will MA transfer ever cross Scottish 19% and 20% thresholds? These are not dynamic and thus 5 years out of date.
      "return the correct total benefit for a Scottish tax payer when hypothetical bands are used" in {
        val hypotheticalBandsOne = List(
          TaxBand("StarterRate", 11851, 12050, 0.19), TaxBand("BasicRate", 12051, 24000, 0.20), TaxBand("IntermediateRate", 24001, 43430, 0.21))

        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - applicationConfig.PERSONAL_ALLOWANCE(), hypotheticalBandsOne) shouldBe 114

        val hypotheticalBandsTwo = List(
          TaxBand("StarterRate", 11851, 13850, 0.10), TaxBand("BasicRate", 13851, 24000, 0.21), TaxBand("IntermediateRate", 24001, 43430, 0.22))

        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - applicationConfig.PERSONAL_ALLOWANCE(), hypotheticalBandsTwo) shouldBe 58

        val hypotheticalBandsThree = List(
          TaxBand("StarterRate", 11851, 12050, 0.10), TaxBand("BasicRate", 12051, 22050, 0.15), TaxBand("FakeRate1", 22051, 27050, 0.20),
          TaxBand("FakeRate2", 27051, 32050, 0.25))

        val personOne = benefitCalculatorHelper.calculateTotalBenefitAcrossBands(30650 - applicationConfig.PERSONAL_ALLOWANCE(), hypotheticalBandsThree)
        val personTwo = benefitCalculatorHelper.calculateTotalBenefitAcrossBands(11650 - applicationConfig.TRANSFEROR_ALLOWANCE, hypotheticalBandsThree)
        personOne - personTwo shouldBe 148

        val personThree = benefitCalculatorHelper.calculateTotalBenefitAcrossBands(13150 - applicationConfig.PERSONAL_ALLOWANCE(), hypotheticalBandsThree)
        val personFour = benefitCalculatorHelper.calculateTotalBenefitAcrossBands(11650 - applicationConfig.TRANSFEROR_ALLOWANCE, hypotheticalBandsThree)
        personThree - personFour shouldBe 36
      }

      "return the correct total benefit for a Welsh tax payer over the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.25) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(Wales, currentTaxYear)
        ) shouldBe applicationConfig.MAX_BENEFIT()
      }

      "return the correct total benefit for a Welsh tax payer half way between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.05 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(Wales, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT() * 0.5).toInt
      }

      "return the correct total benefit for a Welsh tax payer 80% between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.08 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(Wales, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT() * 0.8).toInt
      }

      "return the correct total benefit for a Northern Irish tax payer over the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.25) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(NorthernIreland, currentTaxYear)
        ) shouldBe applicationConfig.MAX_BENEFIT()
      }

      "return the correct total benefit for a Northern Irish tax payer half way between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.05 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(NorthernIreland, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT() * 0.5).toInt
      }

      "return the correct total benefit for a Northern Irish tax payer 80% between personal allowance and the recipient allowance" in {
        benefitCalculatorHelper.calculateTotalBenefitAcrossBands(
          (applicationConfig.PERSONAL_ALLOWANCE() * 1.08 + requiredRounding) - applicationConfig.PERSONAL_ALLOWANCE(),
          eligibilityCalculatorService.getCountryTaxBands(NorthernIreland, currentTaxYear)
        ) shouldBe (applicationConfig.MAX_BENEFIT() * 0.8).toInt
      }
    }

    "getCountryTaxBandsFromFile is called" must {
      "return the correct banded income for an English tax payer" in {
        eligibilityCalculatorService.getCountryTaxBands(England, currentTaxYear) shouldBe
          List(TaxBand("BasicRate", applicationConfig.PERSONAL_ALLOWANCE() + 1, applicationConfig.MAX_LIMIT(), 0.20))
      }

      "return the correct banded income for a Scottish tax payer" in {
        val starterRate = applicationConfig.getTaxBand("StarterRate", "starter-rate", "scotland", currentTaxYear.startYear)
        val basicRate = applicationConfig.getTaxBand("BasicRate", "basic-rate", "scotland", currentTaxYear.startYear)
        val intermediateRate = applicationConfig.getTaxBand("IntermediateRate", "intermediate-rate", "scotland", currentTaxYear.startYear)

        eligibilityCalculatorService.getCountryTaxBands(Scotland, currentTaxYear) shouldBe
          List(TaxBand("StarterRate", applicationConfig.PERSONAL_ALLOWANCE() + 1, starterRate.upperThreshold, starterRate.rate),
            basicRate,
            TaxBand("IntermediateRate", intermediateRate.lowerThreshold, applicationConfig.MAX_LIMIT_SCOT(), intermediateRate.rate))
      }

      "return the correct banded income for a Welsh tax payer" in {
        eligibilityCalculatorService.getCountryTaxBands(Wales, currentTaxYear) shouldBe
          List(TaxBand("BasicRate", applicationConfig.PERSONAL_ALLOWANCE() + 1, applicationConfig.MAX_LIMIT_WALES(), 0.20))
      }

      "return the correct banded income for a Northern Irish tax payer" in {
        eligibilityCalculatorService.getCountryTaxBands(NorthernIreland, currentTaxYear) shouldBe
          List(TaxBand("BasicRate", applicationConfig.PERSONAL_ALLOWANCE() + 1, applicationConfig.MAX_LIMIT_NORTHERN_IRELAND(), 0.20))
      }
    }
  }
}