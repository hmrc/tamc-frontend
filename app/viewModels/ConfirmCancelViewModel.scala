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
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.EndDateHelper

//TODO TESTS UP IN HERE
case class ConfirmCancelViewModel(table: Html, taxYearEndDate: (String, String))


object ConfirmCancelViewModel extends EndDateHelper {

  def apply(model: ConfirmationUpdateAnswers)(implicit messages: Messages): ConfirmCancelViewModel = {

    val date = model.divorceDate

    val bullets = bulletStatement(model.role, date)

    ConfirmCancelViewModel(createTable(model.fullName.getOrElse(""), model.divorceDate, model.email), bullets)
  }

    def createTable(name: String, divorceDate: Option[LocalDate], email: String)(implicit messages: Messages): Html = {

    if (divorceDate.isDefined) {

      val date = transformDate(divorceDate.get)
      //TODO move HTML into partial. Construct and place rows individually
      Html(
        s"""<table>
           |  <tbody>
           |    <tr>
           |      <th scope="row" class="bold">${messages("pages.confirm.cancel.your-name")}</th>
           |      <td>$name</td>
           |    </tr>
           |    <tr>
           |      <th scope="row" class="bold">${messages("pages.divorce.title")}</th>
           |      <td>$date</td>
           |      <td><a href="divorce-enter-year" id="edit-divorcee" data-journey-click="marriage-allowance:button:edit_divorce">${messages("generic.change")}</a>
           |    </tr>
           |      <th scope="row" class="bold">${messages("pages.confirm.cancel.email")}</th>
           |      <td>$email</td>
           |      <td><a href="confirm-email" id="edit-email" data-journey-click="marriage-allowance:button:edit_email">${messages("generic.change")}</a>
           |    </tr>
           |  </tbody>
           |</table>""".stripMargin)
    } else {
      Html(
        s"""<table>
           |  <tbody>
           |    <tr>
           |      <th scope="row" class="bold">${messages("pages.confirm.cancel.your-name")}</th>
           |      <td>$name</td>
           |    </tr>
           |      <th scope="row" class="bold">${messages("pages.confirm.cancel.email")}</th>
           |      <td>$email</td>
           |      <td><a href="confirm-email" id="edit-email" data-journey-click="marriage-allowance:button:edit_email">${messages("generic.change")}</a>
           |    </tr>
           |  </tbody>
           |</table>""".stripMargin)
    }
  }

  def bulletStatement(role: Role,
                       divorceDate: LocalDate)(implicit messages: Messages): (String, String) = {

    lazy val currentTaxYearEnd: String = transformDate(currentTaxYear.finishes)
    lazy val currentTaxYearStart: String =transformDate(currentTaxYear.starts)
    lazy val endOfPreviousTaxYear: String = transformDate(currentTaxYear.previous.finishes)
    lazy val taxYearEndForGivenYear: LocalDate => String = divorceDate => transformDate(TaxYear.taxYearFor(divorceDate).finishes)
    lazy val taxYearStart: LocalDate => String = divorceDate => transformDate(TaxYear.taxYearFor(divorceDate).next.starts)
    val isCurrentYearDivorced = currentTaxYear.contains(divorceDate)


    (role, isCurrentYearDivorced) match {
      case(Transferor, true) => {
        (messages("pages.confirm.cancel.message1", transformDate(calculateEndDate(role, "divorce", divorceDate))),
          messages("pages.confirm.cancel.message2", currentTaxYearStart))
      }
      case(Transferor, false) => {
        (messages("pages.confirm.cancel.message1", transformDate(calculateEndDate(role, "divorce", divorceDate))),
          messages("pages.confirm.cancel.message2", taxYearStart(divorceDate)))
      }
      case(Recipient, true) => {
        (messages("pages.confirm.cancel.message1", transformDate(calculateEndDate(role, "divorce", divorceDate))),
          messages("pages.confirm.cancel.message2", nextTaxYearStart))
      }
      case(Recipient, false) => {
        (messages("pages.confirm.cancel.message1", transformDate(calculateEndDate(role, "divorce", divorceDate))),
          messages("pages.confirm.cancel.message2", currentTaxYearStart))
      }
    }

    }

}

