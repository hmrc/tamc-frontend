/*
 * Copyright 2016 HM Revenue & Customs
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

import config.ApplicationConfig
import models.EligibilityCalculatorInput
import models.EligibilityCalculatorResult
import uk.gov.hmrc.time.TaxYearResolver

object EligibilityCalculatorService {

  def calculate(input: EligibilityCalculatorInput): EligibilityCalculatorResult = {

    val transIncome = input.transferorIncome
    val recIncome = input.recipientIncome
    val transferableAllowance = ApplicationConfig.PERSONAL_ALLOWANCE - transIncome
    val currentTaxYear = TaxYearResolver.currentTaxYear
    input match {
      case incorrectRoles if (transIncome > recIncome) =>
        EligibilityCalculatorResult(messageKey = "eligibility.feedback.incorrect-role")
      case transferor_not_eligible if (transIncome > ApplicationConfig.MAX_LIMIT || transIncome < 0) =>
        EligibilityCalculatorResult(messageKey = ("eligibility.feedback.transferor-not-eligible-"+currentTaxYear))
      case recipient_not_eligible if (recIncome > ApplicationConfig.MAX_LIMIT || recIncome <= ApplicationConfig.PERSONAL_ALLOWANCE) =>
        EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-"+currentTaxYear))
      case transferor_not_benefit if (transIncome > ApplicationConfig.PERSONAL_ALLOWANCE && transIncome < ApplicationConfig.MAX_LIMIT) =>
        EligibilityCalculatorResult(messageKey = ("eligibility.check.unlike-benefit-as-couple-"+currentTaxYear))
      case benefit => getEligibilityBenefitResult(transIncome: Int, recIncome: Int)
    }
  }

  private def getEligibilityBenefitResult(transIncome: Int, recIncome: Int): EligibilityCalculatorResult = {
    var gain = 0
    if (transIncome <= ApplicationConfig.TRANSFEROR_ALLOWANCE) {
      if (ApplicationConfig.PERSONAL_ALLOWANCE < recIncome && recIncome <= ApplicationConfig.RECIPIENT_ALLOWANCE)
        gain = (recIncome - ApplicationConfig.PERSONAL_ALLOWANCE) / 5
      else if (recIncome > ApplicationConfig.RECIPIENT_ALLOWANCE && recIncome <= ApplicationConfig.MAX_LIMIT)
        gain = ApplicationConfig.MAX_BENEFIT
    }

    if (transIncome > ApplicationConfig.TRANSFEROR_ALLOWANCE && transIncome <= ApplicationConfig.PERSONAL_ALLOWANCE) {
      if (recIncome > ApplicationConfig.PERSONAL_ALLOWANCE && recIncome <= ApplicationConfig.RECIPIENT_ALLOWANCE)
        gain = ((recIncome - ApplicationConfig.PERSONAL_ALLOWANCE) / 5) + ((ApplicationConfig.TRANSFEROR_ALLOWANCE - transIncome) / 5)
      else if (recIncome > ApplicationConfig.RECIPIENT_ALLOWANCE && recIncome <= ApplicationConfig.MAX_LIMIT)
        gain = ApplicationConfig.MAX_BENEFIT + ((ApplicationConfig.TRANSFEROR_ALLOWANCE - transIncome) / 5)
    }
    if (gain > 0) {
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(gain))
    } else {
      EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose")
    }
  }
}
