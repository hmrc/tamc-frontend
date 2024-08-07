/*
 * Copyright 2023 HM Revenue & Customs
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

import models.ConfirmationUpdateAnswers
import play.api.i18n.Messages
import views.helpers.LanguageUtilsImpl

import javax.inject.Inject

case class ConfirmUpdateViewModel(endDate: String, effectiveDate: String, rows: Seq[SummaryRow])

case class SummaryRow(title: String, id: String, aria: String, userAnswer: String, changeLink: Option[String] = None, index: Int)

class ConfirmUpdateViewModelImpl@Inject()(languageUtilsImpl: LanguageUtilsImpl)  {

  def apply(updateAnswers: ConfirmationUpdateAnswers)(implicit messages: Messages): ConfirmUpdateViewModel = {

    val nameRow = SummaryRow(messages("pages.confirm.cancel.your-name"), "change-link-name", "change name", updateAnswers.loggedInUserInfo.name.flatMap(_.fullName).getOrElse(""), None, 1)
    val emailRow = SummaryRow(messages("pages.confirm.cancel.email"), "change-link-email" ,"change email", updateAnswers.email, Some(controllers.UpdateRelationship.routes.ConfirmEmailController.confirmEmail().url), 3)
    val defaultRows = List(nameRow, emailRow)
    val endDate = languageUtilsImpl().ukDateTransformer(updateAnswers.maEndingDates.marriageAllowanceEndDate)
    val effectiveDate = languageUtilsImpl().ukDateTransformer(updateAnswers.maEndingDates.personalAllowanceEffectiveDate)

    def createRows(defaultRows: List[SummaryRow])(implicit messages: Messages): List[SummaryRow] = {
      updateAnswers.divorceDate.fold(defaultRows){ divorceDate =>
        val formattedDate = languageUtilsImpl().ukDateTransformer(divorceDate)
        SummaryRow(messages("pages.divorce.title"), "change-link-date", "change date", formattedDate, Some(controllers.UpdateRelationship.routes.DivorceController.divorceEnterYear().url), 2) :: defaultRows
      }
    }

    ConfirmUpdateViewModel(endDate, effectiveDate, createRows(defaultRows).sortBy(_.index))
  }

}
