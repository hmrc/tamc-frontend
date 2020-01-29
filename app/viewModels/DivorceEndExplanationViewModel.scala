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

package viewModels

import models.{Recipient, Role, Transferor}
import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.time.TaxYear
import utils.LanguageUtils
import views.helpers.TextGenerators

case class DivorceEndExplanationViewModel(divorceDate: String, taxYearStatus: String, bulletStatement: (String, String))

object DivorceEndExplanationViewModel {

  def apply(role: Role, divorceDate: LocalDate)(implicit messages: Messages): DivorceEndExplanationViewModel = {

    val divorceDateFormatted = transformDate(divorceDate)
    val currentTaxYear = TaxYear.current

    val taxYearStatus = if(currentTaxYear.contains(divorceDate)) {
        messages("pages.divorce.explanation.current.taxYear")
     } else {
        messages("pages.divorce.explanation.previous.taxYear")
     }

     val bullets = bulletStatements(role, currentTaxYear: TaxYear, divorceDate: LocalDate)

    DivorceEndExplanationViewModel(divorceDateFormatted, taxYearStatus, bullets)
  }

  private def transformDate(date: LocalDate)(implicit messages: Messages): String = {
    TextGenerators.ukDateTransformer(Some(date), LanguageUtils.isWelsh(messages))
  }

  private def bulletStatements(role: Role, currentTaxYear: TaxYear, divorceDate: LocalDate)(implicit messages: Messages): (String, String) = {
    lazy val currentTaxYearEnd: String = transformDate(currentTaxYear.finishes)
    lazy val nextTaxYearStart: String = transformDate(currentTaxYear.next.starts)
    lazy val endOfPreviousTaxYear: String = transformDate(currentTaxYear.previous.finishes)
    lazy val taxYearEndForGivenYear: LocalDate => String = divorceDate => transformDate(TaxYear.taxYearFor(divorceDate).finishes)
    lazy val isCurrentYEarDivorced: Boolean = currentTaxYear.contains(divorceDate)

    //TODO remove duplicate case into case _ =>
    (role, isCurrentYEarDivorced) match  {
      case(Recipient, true) => {
        (messages("pages.divorce.explanation.recipient.current.bullet1", currentTaxYearEnd),
         messages("pages.divorce.explanation.recipient.current.bullet2", nextTaxYearStart))
      }
      case(Recipient, false) => {
        (messages("pages.divorce.explanation.previous.bullet1", endOfPreviousTaxYear),
         messages("pages.divorce.explanation.previous.bullet2"))
      }
      case(Transferor, true) => {
        (messages("pages.divorce.explanation.previous.bullet1", endOfPreviousTaxYear),
        messages("pages.divorce.explanation.previous.bullet2"))
      }
      case(Transferor, false) => {
        (messages("pages.divorce.explanation.previous.bullet1", taxYearEndForGivenYear),
         messages("pages.divorce.explanation.previous.bullet2"))
      }
    }
  }

}
