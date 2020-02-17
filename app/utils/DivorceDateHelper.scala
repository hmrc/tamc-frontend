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

package utils

import models.Role
import org.joda.time.LocalDate
import play.api.i18n.Messages
import services.{EndDateDivorceCalculator, EndDateMACeasedCalculator}
import uk.gov.hmrc.time.TaxYear
import views.helpers.TextGenerators


//TODO TESTS UP IN HERE
trait EndDateHelper {

  def currentTaxYear: TaxYear = TaxYear.current


  lazy val nextTaxYearStart = currentTaxYear.next.starts
  lazy val currentTaxYearEnd = currentTaxYear.finishes
  lazy val currentTaxYearStart = currentTaxYear.starts
  lazy val endOfPreviousTaxYear = currentTaxYear.previous.finishes
  lazy val taxYearEndForGivenYear: LocalDate => LocalDate = divorceDate => TaxYear.taxYearFor(divorceDate).finishes
  lazy val taxYearStart: LocalDate => LocalDate= divorceDate => TaxYear.taxYearFor(divorceDate).next.starts

  def calculateMaEndDate(role: Role, endReason: String, divorceDate: LocalDate): LocalDate = {
    if (endReason == "divorce") EndDateDivorceCalculator.calculateEndDate(role, divorceDate)
    else EndDateMACeasedCalculator.calculateEndDate
  }

  def calculatePaEffectiveDate: LocalDate = ???

  def transformDate(date: LocalDate)(implicit messages: Messages): String = {
    TextGenerators.ukDateTransformer(Some(date), LanguageUtils.isWelsh(messages))
  }

}
