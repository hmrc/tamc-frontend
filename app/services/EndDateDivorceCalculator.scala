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

import models.{Recipient, Role, Transferor}
import org.joda.time.LocalDate
import uk.gov.hmrc.time.TaxYear
//TODO TESTS UP IN HERE
object EndDateDivorceCalculator {


  def calculateEndDate(role: Role, divorceDate: LocalDate): LocalDate = {

    val isDivorceDateInTaxYear: Boolean = TaxYear.current.contains(divorceDate)

    (role, isDivorceDateInTaxYear) match {
      case(Recipient, true) => TaxYear.current.finishes
      case(Recipient, false) => TaxYear.current.previous.finishes
      case(Transferor, true) => TaxYear.current.previous.finishes
      case(Transferor, false) => TaxYear.taxYearFor(divorceDate).finishes
    }
  }

  def calculatePersonalAllowanceEffectiveDate(marriageAllowanceEndDate: LocalDate): LocalDate = TaxYear.taxYearFor(marriageAllowanceEndDate).next.starts
}
