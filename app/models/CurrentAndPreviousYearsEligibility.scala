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

package models

import utils.SystemTaxYear

case class CurrentAndPreviousYearsEligibility(currentYearAvailable: Boolean, previousYears: List[TaxYear],
                                              registrationInput: RegistrationFormInput, availableTaxYears: List[TaxYear])

object CurrentAndPreviousYearsEligibility {

  def apply(recipient: RecipientRecord, taxYear: SystemTaxYear): CurrentAndPreviousYearsEligibility = {
    val currentTaxYear: Int = taxYear.current().startYear
    val currentYearAvailable = recipient.availableTaxYears.exists(_.year == currentTaxYear)
    val previousYears = recipient.availableTaxYears.filterNot(_.year == currentTaxYear)

    CurrentAndPreviousYearsEligibility(currentYearAvailable, previousYears, recipient.data, recipient.availableTaxYears)
  }
}