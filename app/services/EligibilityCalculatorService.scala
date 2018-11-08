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

package services

import config.ApplicationConfig._
import models.{EligibilityCalculatorResult, _}
import play.api.libs.json.Json
import uk.gov.hmrc.time.TaxYearResolver
import utils.BenefitCalculatorHelper

import scala.io.Source

object EligibilityCalculatorService {

  private def maxLimit(country: Country): Int = country match {
    case England => MAX_LIMIT
    case Scotland => MAX_LIMIT_SCOT
    case Wales => MAX_LIMIT_WALES
    case NorthernIreland => MAX_LIMIT_NORTHERN_IRELAND
  }

  def calculate(transferorIncome: Int, recipientIncome: Int, countryOfResidence: Country): EligibilityCalculatorResult = {

    val hasMaxBenefit = transferorIncome<TRANSFEROR_ALLOWANCE&&recipientIncome>RECIPIENT_ALLOWANCE
    val recipientNotEligible = recipientIncome>maxLimit(countryOfResidence)||recipientIncome<PERSONAL_ALLOWANCE
    val bothOverMaxLimit = transferorIncome>maxLimit(countryOfResidence)&&recipientIncome>maxLimit(countryOfResidence)

    if(transferorIncome>recipientIncome)
      EligibilityCalculatorResult("eligibility.feedback.incorrect-role")
    else if(bothOverMaxLimit)
      EligibilityCalculatorResult("eligibility.feedback.transferor-not-eligible-" + TaxYearResolver.currentTaxYear)
    else if(recipientNotEligible)
      EligibilityCalculatorResult("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear)
    else if(transferorIncome>PERSONAL_ALLOWANCE)
      EligibilityCalculatorResult("eligibility.check.unlike-benefit-as-couple-" + TaxYearResolver.currentTaxYear)
    else if(hasMaxBenefit)
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT))
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
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose", Some(PERSONAL_ALLOWANCE))
  }

  private def calculateGain(transferorIncome: Int, recipientIncome: Int, country: Country, countryTaxBands: List[TaxBand]): Int = {

    val recipientBenefit = BenefitCalculatorHelper.calculateTotalBenefitAcrossBands(recipientIncome, country, countryTaxBands)
    val transferorDifference = transferorIncome - TRANSFEROR_ALLOWANCE
    val taxPercentage = getCountryTaxBandsFromFile(country).head.rate
    val transferorLoss = math.max(transferorDifference*taxPercentage, 0)

    (recipientBenefit - transferorLoss.floor).toInt
  }

  def getCountryTaxBandsFromFile(countryOfResidence: Country): List[TaxBand] = {
    val resource = getClass.getResourceAsStream(s"/data/${countryOfResidence}Bands.json")
    Json.parse(Source.fromInputStream(resource).mkString).as[CountryTaxBands].taxBands
  }
}
