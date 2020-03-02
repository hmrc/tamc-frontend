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

import models.ConfirmationUpdateAnswers
import play.api.i18n.Messages
import views.helpers.TextGenerator

case class ConfirmUpdateViewModel(endDate: String, effectiveDate: String, rows: Seq[SummaryRow])

case class SummaryRow(title: String, userAnswer: String, changeLink: Option[String] = None, index: Int)

object ConfirmUpdateViewModel  {

  def apply(model: ConfirmationUpdateAnswers)(implicit messages: Messages): ConfirmUpdateViewModel = {

    val nameRow = SummaryRow(messages("pages.confirm.cancel.your-name"), model.fullName, None, 1)
    val emailRow = SummaryRow(messages("pages.confirm.cancel.email"), model.email, Some(controllers.routes.UpdateRelationshipController.confirmEmail().url), 3)
    val defaultRows = List(nameRow, emailRow)
    val endDate = TextGenerator().ukDateTransformer(model.maEndingDates.marriageAllowanceEndDate)
    val effectiveDate = TextGenerator().ukDateTransformer(model.maEndingDates.personalAllowanceEffectiveDate)

    def createRows(defaultRows: List[SummaryRow])(implicit messages: Messages): List[SummaryRow] = {
      model.divorceDate.fold(defaultRows){ divorceDate =>
        val formattedDate = TextGenerator().ukDateTransformer(divorceDate)
        SummaryRow(messages("pages.divorce.title"), formattedDate, Some(controllers.routes.UpdateRelationshipController.divorceEnterYear().url), 2) :: defaultRows
      }
    }

    ConfirmUpdateViewModel(endDate, effectiveDate, createRows(defaultRows).sortBy(_.index))
  }

}

