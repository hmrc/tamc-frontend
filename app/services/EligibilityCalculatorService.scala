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
import uk.gov.hmrc.time.TaxYearResolver

object EligibilityCalculatorService {

  def calculate(transferorIncome: Int, recipientIncome: Int, countryOfResidence: Country): EligibilityCalculatorResult = {

    val maxLimit = countryOfResidence match {
      case England => MAX_LIMIT
      case Scotland => MAX_LIMIT_SCOT
      case Wales => MAX_LIMIT_WALES
      case NorthernIreland => MAX_LIMIT_NORTHERN_IRELAND
    }

    val hasMaxBenefit = transferorIncome<TRANSFEROR_ALLOWANCE&&recipientIncome>RECIPIENT_ALLOWANCE
    val recipientNotEligible = recipientIncome>maxLimit||recipientIncome<PERSONAL_ALLOWANCE
    val bothOverLimit = transferorIncome>maxLimit&&recipientIncome>maxLimit

    if(transferorIncome>recipientIncome) EligibilityCalculatorResult("eligibility.feedback.incorrect-role")
    else if(bothOverLimit) EligibilityCalculatorResult("eligibility.feedback.transferor-not-eligible-"+ TaxYearResolver.currentTaxYear)
    else if(recipientNotEligible) EligibilityCalculatorResult("eligibility.feedback.recipient-not-eligible-" + TaxYearResolver.currentTaxYear)
    else if(transferorIncome>PERSONAL_ALLOWANCE) EligibilityCalculatorResult("eligibility.check.unlike-benefit-as-couple-" + TaxYearResolver.currentTaxYear)
    else if(hasMaxBenefit) EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(MAX_BENEFIT))
    else {
      val possibleGain = calculateGain(transferorIncome, recipientIncome, countryOfResidence)
      if(possibleGain>=1)
        EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(possibleGain))
      else
        EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose", Some(PERSONAL_ALLOWANCE))
    }
  }

  private def calculateGain(transferorIncome: Int, recipientIncome: Int, country: Country): Int = {
    val recipientDifference = recipientIncome - PERSONAL_ALLOWANCE
    val recipientBenefit = math.min(recipientDifference*0.2, MAX_BENEFIT)

    val transferorDifference = transferorIncome - TRANSFEROR_ALLOWANCE
    val transferorLoss = math.max(transferorDifference*0.2,0)

    (recipientBenefit.floor-transferorLoss.floor).toInt
  }
}
