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
import views.helpers.TextGenerator

//TODO TESTS UP IN HERE
case class ConfirmCancelViewModel(endDate: String, effectiveDate: String, rows: Seq[SummaryRow])

case class SummaryRow(title: String, userAnswer: String, changeLink: Option[String] = None)

object ConfirmCancelViewModel  {

  def apply(model: ConfirmationUpdateAnswers)(implicit messages: Messages): ConfirmCancelViewModel = {


    //TODO Make sure that these populate as expected types
    val nameRow: SummaryRow = model.fullName.map(SummaryRow(messages("pages.confirm.cancel.your-name"), _))
    val divorceDate: Option[SummaryRow] = model.divorceDate.map { date =>
      val formattedDate = TextGenerator(messages).ukDateTransformer(date)
      SummaryRow(messages("pages.divorce.title"), formattedDate, Some(controllers.routes.UpdateRelationshipController.divorceEnterYear().url))
    }
    val email: SummaryRow = SummaryRow(messages("pages.confirm.cancel.email"), model.email, Some(controllers.routes.UpdateRelationshipController.confirmEmail().url))

    val rows = List(nameRow, email)

    val endDate = TextGenerator(messages).ukDateTransformer(model.maEndDate)
    val effectiveDate = TextGenerator(messages).ukDateTransformer(model.paEffectiveDate)


    ConfirmCancelViewModel(endDate, effectiveDate, rows)
  }

}

