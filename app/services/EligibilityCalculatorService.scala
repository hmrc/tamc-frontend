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
import models._
import play.api.libs.json.Json
import uk.gov.hmrc.time
import utils.BenefitCalculatorHelper

import scala.io.Source

object EligibilityCalculatorService extends EligibilityCalculatorService

trait EligibilityCalculatorService {

  val currentTaxYear: Int = time.TaxYear.current.startYear

  private def maxLimit(country: Country): Int = country match {
    case England => MAX_LIMIT
    case Scotland => MAX_LIMIT_SCOT
    case Wales => MAX_LIMIT_WALES
    case NorthernIreland => MAX_LIMIT_NORTHERN_IRELAND
  }

  private def maxLimitToFormattedCurrency(country: Country): String = {
    val limit = maxLimit(country)
    val formatter = NumberFormat.getCurrencyInstance(Locale.UK)
    formatter.setMaximumFractionDigits(0)
    formatter.format(limit)
  }

  def calculate(transferorIncome: Int, recipientIncome: Int, countryOfResidence: Country): EligibilityCalculatorResult = {

    val hasMaxBenefit = transferorIncome<TRANSFEROR_ALLOWANCE&&recipientIncome>RECIPIENT_ALLOWANCE
    val recipientNotEligible = recipientIncome>maxLimit(countryOfResidence)||recipientIncome<PERSONAL_ALLOWANCE
    val bothOverMaxLimit = transferorIncome>maxLimit(countryOfResidence)&&recipientIncome>maxLimit(countryOfResidence)

    if(transferorIncome>recipientIncome)
      EligibilityCalculatorResult("eligibility.feedback.incorrect-role")
    else if(bothOverMaxLimit)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.transferor-not-eligible-" +
        currentTaxYear, messageParam = Some(maxLimitToFormattedCurrency(countryOfResidence)))
    else if(recipientNotEligible)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.recipient-not-eligible-" +
        currentTaxYear, messageParam = Some(maxLimitToFormattedCurrency(countryOfResidence)))
    else if(transferorIncome>PERSONAL_ALLOWANCE)
      EligibilityCalculatorResult("eligibility.check.unlike-benefit-as-couple-" + currentTaxYear)
    else if(hasMaxBenefit) {
      val basicRate = getCountryTaxBandsFromFile(countryOfResidence).find(band => band.name == "BasicRate").head.rate
      val maxBenefit = (MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER * basicRate).ceil.toInt
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(maxBenefit))
    }
    else {
      partialEligibilityScenario(transferorIncome,recipientIncome,countryOfResidence, getCountryTaxBandsFromFile(countryOfResidence))
    }
  }

  private def partialEligibilityScenario(transferorIncome: Int, recipientIncome: Int,
                                         countryOfResidence: Country, countryTaxBands: List[TaxBand]): EligibilityCalculatorResult = {
    val possibleGain = calculateGain(transferorIncome, recipientIncome, countryOfResidence, countryTaxBands)
    if(possibleGain>=1)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(possibleGain))
    else
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose")
  }

  private def calculateGain(transferorIncome: Int, recipientIncome: Int, country: Country, countryTaxBands: List[TaxBand]): Int = {
    val recipientIncomeMinusPA = recipientIncome - PERSONAL_ALLOWANCE
    val recipientBenefit = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(recipientIncomeMinusPA, countryTaxBands)

    val transferorDifference = transferorIncome - TRANSFEROR_ALLOWANCE
    val transferorLoss = if(transferorDifference > 0) BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(transferorDifference, countryTaxBands) else 0

    (recipientBenefit - transferorLoss.floor).toInt
  }

  def getCountryTaxBandsFromFile(countryOfResidence: Country): List[TaxBand] = {
    val resource = getClass.getResourceAsStream(s"/data/${countryOfResidence}Bands.json")
    Json.parse(Source.fromInputStream(resource).mkString).as[CountryTaxBands].taxBands
  }
}
