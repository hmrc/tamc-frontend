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

import config.ApplicationConfig._
import models.EligibilityCalculatorInput
import models.EligibilityCalculatorResult
import uk.gov.hmrc.time.TaxYearResolver

object EligibilityCalculatorService {

  def calculate(input: EligibilityCalculatorInput): EligibilityCalculatorResult = {

    val transIncome = input.transferorIncome
    val recIncome = input.recipientIncome
    val currentTaxYear = TaxYearResolver.currentTaxYear
    input match {
      case incorrectRoles if (transIncome > recIncome) =>
        EligibilityCalculatorResult(messageKey = "eligibility.feedback.incorrect-role")
      case transferor_not_eligible if (transIncome > MAX_LIMIT || transIncome < 0) =>
        EligibilityCalculatorResult(messageKey = ("eligibility.feedback.transferor-not-eligible-" + currentTaxYear))
      case recipient_not_eligible if (recIncome > MAX_LIMIT || recIncome <= PERSONAL_ALLOWANCE) =>
        EligibilityCalculatorResult(messageKey = ("eligibility.feedback.recipient-not-eligible-" + currentTaxYear))
      case transferor_not_benefit if (transIncome > PERSONAL_ALLOWANCE && transIncome < MAX_LIMIT) =>
        EligibilityCalculatorResult(messageKey = ("eligibility.check.unlike-benefit-as-couple-" + currentTaxYear))
      case benefit => getEligibilityBenefitResult(transIncome: Int, recIncome: Int)
    }
  }

  private def getEligibilityBenefitResult(transIncome: Int, recIncome: Int): EligibilityCalculatorResult =
    calculateGain(transIncome, recIncome) match {
      case gain if gain > 0 => EligibilityCalculatorResult(messageKey = "eligibility.feedback.gain", Some(gain))
      case _ => EligibilityCalculatorResult(messageKey = "eligibility.feedback.loose")
    }

  private def calculateGain(transIncome: Int, recIncome: Int): Int = {
    (transIncome, recIncome) match {
      case (tIncome, rIncome) if (tIncome <= TRANSFEROR_ALLOWANCE) &&
        recIncome.fromTo(PERSONAL_ALLOWANCE, RECIPIENT_ALLOWANCE) => (recIncome - PERSONAL_ALLOWANCE) / 5
      case (tIncome, rIncome) if (tIncome <= TRANSFEROR_ALLOWANCE) &&
        recIncome.fromTo(RECIPIENT_ALLOWANCE, MAX_LIMIT) => MAX_BENEFIT
      case (tIncome, rIncome) if (tIncome.fromTo(TRANSFEROR_ALLOWANCE, PERSONAL_ALLOWANCE) &&
        recIncome.fromTo(PERSONAL_ALLOWANCE, RECIPIENT_ALLOWANCE)) => ((recIncome - PERSONAL_ALLOWANCE) / 5) + ((TRANSFEROR_ALLOWANCE - transIncome) / 5)
      case (tIncome, rIncome) if (tIncome.fromTo(TRANSFEROR_ALLOWANCE, PERSONAL_ALLOWANCE) &&
        recIncome.fromTo(RECIPIENT_ALLOWANCE, MAX_LIMIT)) => MAX_BENEFIT + ((TRANSFEROR_ALLOWANCE - transIncome) / 5)
      case _ => 0
    }
  }

  implicit class Between(value: Int) {

    // The method excludes the lower limit from range
    def fromTo(lower: Int, upper: Int): Boolean = {
      val excludeLowerLimit = 1
      (lower + excludeLowerLimit) to upper contains value
    }

  }

}
