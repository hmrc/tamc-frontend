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

import java.text.NumberFormat

import config.ApplicationConfig.{MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER, MAX_BENEFIT}
import models._
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import views.helpers.TextGenerator

case class HistorySummaryButton(id: String, content: String, href: String)

case class HistorySummaryViewModel(paragraphContent: Html, button: HistorySummaryButton)

object HistorySummaryViewModel {

  //TODO is the taxyear being handled correctly here?
  def apply(relationshipRecords: RelationshipRecords)(implicit messages: Messages): HistorySummaryViewModel = {

    if (relationshipRecords.recordStatus == Active) {
      createActiveRecordBasedViewModel(relationshipRecords.role)
    } else {
      createHistoricBasedViewModel(relationshipRecords.role)
    }
  }

  private def createActiveRecordBasedViewModel(role: Role)(implicit messages: Messages): HistorySummaryViewModel = {
    lazy val maxPersonalAllowanceTransfer = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(TaxYear.current.currentYear)
    lazy val maxPersonalBenefit = MAX_BENEFIT(TaxYear.current.currentYear)

    lazy val formattedMaxPATransfer = NumberFormat.getIntegerInstance().format(maxPersonalAllowanceTransfer)
    lazy val formattedMaxBenefit = NumberFormat.getIntegerInstance().format(maxPersonalBenefit)

    val paragraphContent = if (role == Transferor) {
      Html(s"<p>${messages("pages.history.active.transferor")}</p>")
    } else {
      Html(s"<p>${messages("pages.history.active.recipient.paragraph1", formattedMaxPATransfer)}</p>" +
        s"<p>${messages("pages.history.active.recipient.paragraph2", formattedMaxBenefit)}</P>")
    }

    val button = HistorySummaryButton(
      "checkOrUpdateMarriageAllowance",
      messages("pages.history.active.button"),
      controllers.routes.UpdateRelationshipController.decision().url
    )

    HistorySummaryViewModel(paragraphContent, button)
  }


  private def createHistoricBasedViewModel(role: Role)(implicit messages: Messages): HistorySummaryViewModel = {
    val formattedEndOfYear = TextGenerator().ukDateTransformer(TaxYear.current.finishes)

    val paragraphContent = if (role == Transferor) {

      //TODO welsh
      Html(s"<p>${messages("pages.history.historic.ended")}</p>" +
        s"<p>${messages("pages.history.historic.transferor", formattedEndOfYear)}</P>")

    } else {
      //TODO welsh
      Html(s"<p>${messages("pages.history.historic.ended")}</p>" +
        s"<p>${messages("pages.history.historic.recipient", formattedEndOfYear)}</P>")
    }

    val button = HistorySummaryButton("checkMarriageAllowance", messages("pages.history.historic.button"),
      controllers.routes.UpdateRelationshipController.claims().url)
    HistorySummaryViewModel(paragraphContent, button)

  }

}
