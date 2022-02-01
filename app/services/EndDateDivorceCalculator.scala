/*
 * Copyright 2022 HM Revenue & Customs
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
import models.{Recipient, Role, Transferor}
import java.time.LocalDate

import uk.gov.hmrc.time.CurrentTaxYear

class EndDateDivorceCalculator @Inject()(taxYear: CurrentTaxYear) {

  def calculateEndDate(role: Role, divorceDate: LocalDate): LocalDate = {

    val isDivorceDateInTaxYear: Boolean = taxYear.current.contains(divorceDate)

    (role, isDivorceDateInTaxYear) match {
      case(Recipient, true) => taxYear.current.finishes
      case(Recipient, false) => taxYear.current.previous.finishes
      case(Transferor, true) => taxYear.current.previous.finishes
      case(Transferor, false) => taxYear.taxYearFor(divorceDate).finishes
    }
  }

  def calculatePersonalAllowanceEffectiveDate(marriageAllowanceEndDate: LocalDate): LocalDate = taxYear.taxYearFor(marriageAllowanceEndDate).next.starts
}
