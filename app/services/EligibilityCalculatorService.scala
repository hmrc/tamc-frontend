/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig._
import models._
import play.api.libs.json.Json
import utils.BenefitCalculatorHelper

import scala.io.Source

object EligibilityCalculatorService extends EligibilityCalculatorService

trait EligibilityCalculatorService {

  def calculate(transferorIncome: Int, recipientIncome: Int, countryOfResidence: Country): EligibilityCalculatorResult = {

    val hasMaxBenefit = transferorIncome < TRANSFEROR_ALLOWANCE && recipientIncome > RECIPIENT_ALLOWANCE
    val recipientNotEligible = recipientIncome > BenefitCalculatorHelper.maxLimit(countryOfResidence) || recipientIncome < PERSONAL_ALLOWANCE()
    val bothOverMaxLimit = transferorIncome > BenefitCalculatorHelper.maxLimit(countryOfResidence) && recipientIncome > BenefitCalculatorHelper.maxLimit(countryOfResidence)

    if (transferorIncome > recipientIncome)
      EligibilityCalculatorResult("eligibility.feedback.incorrect-role")
    else if (bothOverMaxLimit)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.transferor-not-eligible",
        messageParam = Some(BenefitCalculatorHelper.setCurrencyFormat(countryOfResidence, "ML")))
    else if (recipientNotEligible)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible",
        messageParam = Some(BenefitCalculatorHelper.setCurrencyFormat(countryOfResidence, "PA")),
        messageParam2 = Some(BenefitCalculatorHelper.setCurrencyFormat(countryOfResidence, "ML")))
    else if (transferorIncome > PERSONAL_ALLOWANCE()) {
      val paFormat = BenefitCalculatorHelper.currencyFormatter(PERSONAL_ALLOWANCE())
      EligibilityCalculatorResult("eligibility.check.unlike-benefit-as-couple", messageParam = Some(paFormat))
    } else if (hasMaxBenefit) {
      val basicRate = getCountryTaxBandsFromFile(countryOfResidence).find(band => band.name == "BasicRate").head.rate
      val maxBenefit = (MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER() * basicRate).ceil.toInt
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(maxBenefit))
    } else {
      partialEligibilityScenario(transferorIncome, recipientIncome, countryOfResidence, getCountryTaxBandsFromFile(countryOfResidence))
    }
  }

  private def partialEligibilityScenario(transferorIncome: Int, recipientIncome: Int,
                                         countryOfResidence: Country, countryTaxBands: List[TaxBand]): EligibilityCalculatorResult = {
    val possibleGain = calculateGain(transferorIncome, recipientIncome, countryOfResidence, countryTaxBands)
    if (possibleGain >= 1)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(possibleGain))
    else
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose")
  }

  private def calculateGain(transferorIncome: Int, recipientIncome: Int, country: Country, countryTaxBands: List[TaxBand]): Int = {
    val recipientIncomeMinusPA = recipientIncome - PERSONAL_ALLOWANCE()
    val recipientBenefit = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(recipientIncomeMinusPA, countryTaxBands)

    val transferorDifference = transferorIncome - TRANSFEROR_ALLOWANCE
    val transferorLoss = if (transferorDifference > 0) BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(transferorDifference, countryTaxBands) else 0

    (recipientBenefit - transferorLoss.floor).toInt
  }

  def getCountryTaxBandsFromFile(countryOfResidence: Country): List[TaxBand] = {
    val resource = getClass.getResourceAsStream(s"/data/${countryOfResidence}Bands.json")
    Json.parse(Source.fromInputStream(resource).mkString).as[CountryTaxBands].taxBands
  }
}
