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

import com.google.inject.Inject
import config.ApplicationConfig
import models.{Country, EligibilityCalculatorResult, TaxBand}
import uk.gov.hmrc.time.TaxYear
import utils.{BenefitCalculatorHelper, TaxBandReader}

class EligibilityCalculatorService @Inject()(
  appConfig: ApplicationConfig,
  benefitCalculatorHelper: BenefitCalculatorHelper,
  taxBandReader: TaxBandReader
) {

  def calculate(transferorIncome: BigDecimal, recipientIncome: BigDecimal, countryOfResidence: Country, taxYear: TaxYear): EligibilityCalculatorResult = {
    val maxBenefitLimit = benefitCalculatorHelper.maxLimit(countryOfResidence)

    val hasMaxBenefit = transferorIncome < appConfig.TRANSFEROR_ALLOWANCE && recipientIncome > appConfig.RECIPIENT_ALLOWANCE
    val recipientNotEligible = recipientIncome > maxBenefitLimit || recipientIncome < appConfig.PERSONAL_ALLOWANCE()
    val bothOverMaxLimit = transferorIncome > maxBenefitLimit && recipientIncome > maxBenefitLimit

    if (transferorIncome > recipientIncome)
      EligibilityCalculatorResult("eligibility.feedback.incorrect-role")
    else if (bothOverMaxLimit)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.transferor-not-eligible",
        messageParam = Some(benefitCalculatorHelper.setCurrencyFormat(countryOfResidence, "ML")))
    else if (recipientNotEligible)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
        messageParam = Some(benefitCalculatorHelper.setCurrencyFormat(countryOfResidence, "PA")),
        messageParam2 = Some(benefitCalculatorHelper.setCurrencyFormat(countryOfResidence, "ML")))
    else if (transferorIncome > appConfig.PERSONAL_ALLOWANCE()) {
      val paFormat = benefitCalculatorHelper.currencyFormatter(appConfig.PERSONAL_ALLOWANCE())
      EligibilityCalculatorResult("eligibility.check.unlike-benefit-as-couple", messageParam = Some(paFormat))
    } else if (hasMaxBenefit) {
      val basicRate = getCountryTaxBands(countryOfResidence, taxYear).find(band => band.name == "BasicRate").head.rate
      val maxBenefit = (appConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER() * basicRate).ceil.toInt
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(maxBenefit))
    } else {
      partialEligibilityScenario(transferorIncome, recipientIncome, getCountryTaxBands(countryOfResidence, taxYear))
    }
  }

  private def partialEligibilityScenario(
                                          transferorIncome: BigDecimal,
                                          recipientIncome: BigDecimal,
                                          countryTaxBands: List[TaxBand]
                                        ): EligibilityCalculatorResult = {
    val possibleGain = calculateGain(transferorIncome, recipientIncome, countryTaxBands)
    if (possibleGain >= 1)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(possibleGain))
    else
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose")
  }

  private def calculateGain(transferorIncome: BigDecimal, recipientIncome: BigDecimal, countryTaxBands: List[TaxBand]): Int = {
    val recipientIncomeMinusPA = recipientIncome - appConfig.PERSONAL_ALLOWANCE()
    val recipientBenefit = benefitCalculatorHelper.calculateTotalBenefitAcrossBands(recipientIncomeMinusPA, countryTaxBands)

    val transferorDifference = transferorIncome - appConfig.TRANSFEROR_ALLOWANCE
    val transferorLoss = if (transferorDifference > 0) benefitCalculatorHelper.calculateTotalBenefitAcrossBands(transferorDifference, countryTaxBands) else 0

    (recipientBenefit - transferorLoss.toFloat).toInt
  }

  def getCountryTaxBands(countryOfResidence: Country, taxYear: TaxYear): List[TaxBand] = {
    taxBandReader.read(countryOfResidence, taxYear)
  }
}
