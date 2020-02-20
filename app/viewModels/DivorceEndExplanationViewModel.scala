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

import models.{Recipient, Role}
import org.joda.time.LocalDate
import play.api.i18n.Messages
import uk.gov.hmrc.time.TaxYear
import views.helpers.TextGenerator

//TODO TESTS UP IN HERE

case class DivorceEndExplanationViewModel(divorceDate: String, taxYearStatus: String, bulletStatement: (String, String))

object DivorceEndExplanationViewModel {

  def apply(role: Role, divorceDate: LocalDate, maEndDate: LocalDate, paEffectiveDate: LocalDate)(implicit messages: Messages): DivorceEndExplanationViewModel = {

    val divorceDateFormatted = TextGenerator().ukDateTransformer(divorceDate)
    val isCurrentYearDivorced: Boolean =  TaxYear.current.contains(divorceDate)

    val taxYearStatus = if(isCurrentYearDivorced) {
        messages("pages.divorce.explanation.current.taxYear")
     } else {
        messages("pages.divorce.explanation.previous.taxYear")
     }

     val bullets = bulletStatements(role, isCurrentYearDivorced, maEndDate, paEffectiveDate)

    DivorceEndExplanationViewModel(divorceDateFormatted, taxYearStatus, bullets)
  }

  def bulletStatements(role: Role,
                       isCurrentYearDivorced: Boolean,
                       maEndDate: LocalDate,
                       paEffectiveDate: LocalDate)(implicit messages: Messages): (String, String) = {



    (role, isCurrentYearDivorced) match  {
      case(Recipient, true) => {
        (messages("pages.divorce.explanation.recipient.current.bullet1", TextGenerator().ukDateTransformer(maEndDate)),
          messages("pages.divorce.explanation.recipient.current.bullet2", TextGenerator().ukDateTransformer(paEffectiveDate)))
      }
      case _ => {
        (messages("pages.divorce.explanation.previous.bullet1", TextGenerator().ukDateTransformer(maEndDate)),
          messages("pages.divorce.explanation.previous.bullet2"))
      }
    }
  }

}
