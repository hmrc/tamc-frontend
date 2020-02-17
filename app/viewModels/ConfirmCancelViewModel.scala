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

import models.{ConfirmationUpdateAnswers, Recipient, Role, Transferor}
import org.joda.time.LocalDate
import play.api.i18n.Messages
import services.EndDateDivorceCalculator
import uk.gov.hmrc.time.TaxYear
import utils.EndDateHelper

//TODO TESTS UP IN HERE
case class ConfirmCancelViewModel(taxYearEndDate: (String, String), rows: Seq[SummaryRow])

case class SummaryRow(title: String, userAnswer: String, changeLink: Option[String] = None)

object ConfirmCancelViewModel extends EndDateHelper {

  def apply(model: ConfirmationUpdateAnswers)(implicit messages: Messages): ConfirmCancelViewModel = {


    //TODO Make sure that these populate as expected types
    val nameRow: Option[SummaryRow] = model.fullName.map(SummaryRow(messages("pages.confirm.cancel.your-name"), _))
    val divorceDate: Option[SummaryRow] = model.divorceDate.map { date =>
      val formattedDate = transformDate(date)
      SummaryRow(messages("pages.divorce.title"), formattedDate, Some(controllers.routes.UpdateRelationshipController.divorceEnterYear().url))
    }
    val email: Option[SummaryRow] = Some(SummaryRow(messages("pages.confirm.cancel.email"), model.email, Some(controllers.routes.UpdateRelationshipController.confirmEmail().url)))

    val rows = List(nameRow, divorceDate, email).flatten


    val date = model.divorceDate



    val bullets = bulletStatement(model.role, date.getOrElse(LocalDate.now()))

    ConfirmCancelViewModel(bullets, rows)
  }

  //TODO CLEAN THIS UP!!!
  def bulletStatement(role: Role, divorceDate: LocalDate)(implicit messages: Messages): (String, String) = {

    lazy val currentTaxYearEnd: String = transformDate(currentTaxYear.finishes)
    lazy val currentTaxYearStart: String =transformDate(currentTaxYear.starts)
    lazy val endOfPreviousTaxYear: String = transformDate(currentTaxYear.previous.finishes)
    lazy val taxYearEndForGivenYear: LocalDate => String = divorceDate => transformDate(TaxYear.taxYearFor(divorceDate).finishes)
    lazy val taxYearStart: LocalDate => String = divorceDate => transformDate(TaxYear.taxYearFor(divorceDate).next.starts)
    val isCurrentYearDivorced = currentTaxYear.contains(divorceDate)


    (role, isCurrentYearDivorced) match {
      case(Transferor, true) => {
        (messages("pages.confirm.cancel.message1", transformDate(EndDateDivorceCalculator.calculateEndDate(role, divorceDate))),
          messages("pages.confirm.cancel.message2", currentTaxYearStart))
      }
      case(Transferor, false) => {
        (messages("pages.confirm.cancel.message1", transformDate(EndDateDivorceCalculator.calculateEndDate(role, divorceDate))),
          messages("pages.confirm.cancel.message2", taxYearStart(divorceDate)))
      }
      case(Recipient, true) => {
        (messages("pages.confirm.cancel.message1", transformDate(EndDateDivorceCalculator.calculateEndDate(role,  divorceDate))),
          messages("pages.confirm.cancel.message2", nextTaxYearStart))
      }
      case(Recipient, false) => {
        (messages("pages.confirm.cancel.message1", transformDate(EndDateDivorceCalculator.calculateEndDate(role, divorceDate))),
          messages("pages.confirm.cancel.message2", currentTaxYearStart))
      }
    }

    }

}

