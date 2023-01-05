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

import config.ApplicationConfig

import java.text.NumberFormat

import models._
import play.api.i18n.Messages
import play.twirl.api.Html
import views.helpers.LanguageUtilsImpl

import javax.inject.Inject

case class HistorySummaryButton(id: String, content: String, href: String)

case class HistorySummaryViewModel(paragraphContent: Html, button: HistorySummaryButton, displayName: String)

class HistorySummaryViewModelImpl @Inject()(applicationConfig: ApplicationConfig, languageUtilsImpl: LanguageUtilsImpl) {

  def apply(role: Role, hasMarriageAllowanceBeenCancelled: Boolean, loggedInUserInfo: LoggedInUserInfo)(implicit messages: Messages): HistorySummaryViewModel = {

    val (paragraphContent, button) = if (hasMarriageAllowanceBeenCancelled) {
      marriageAllowanceCancelledContent(role)
    } else {
      activeRecordContent(role)
    }

    val displayName = loggedInUserInfo.name.flatMap(_.fullName).getOrElse("")

    HistorySummaryViewModel(paragraphContent, button, displayName)
  }

  private def activeRecordContent(role: Role)(implicit messages: Messages): (Html, HistorySummaryButton) = {
    lazy val maxPersonalAllowanceTransfer = applicationConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(applicationConfig.currentTaxYear.currentYear)
    lazy val maxPersonalBenefit = applicationConfig.MAX_BENEFIT(applicationConfig.currentTaxYear.currentYear)

    lazy val formattedMaxPATransfer = NumberFormat.getIntegerInstance().format(maxPersonalAllowanceTransfer)
    lazy val formattedMaxBenefit = NumberFormat.getIntegerInstance().format(maxPersonalBenefit)

    val paragraphContent = if (role == Transferor) {
      Html(s"""<p class="govuk-body">${messages("pages.history.active.transferor")}</p>""")
    } else {
      Html(s"""<p class="govuk-body">${messages("pages.history.active.recipient.paragraph1", formattedMaxPATransfer)}</p>""" +
        s"""<p class="govuk-body">${messages("pages.history.active.recipient.paragraph2", formattedMaxBenefit)}</p>""")
    }

    val button = HistorySummaryButton(
      "checkOrUpdateMarriageAllowance",
      messages("pages.history.active.button"),
      controllers.routes.UpdateRelationshipController.decision.url
    )

    (paragraphContent, button)
  }


  private def marriageAllowanceCancelledContent(role: Role)(implicit messages: Messages): (Html, HistorySummaryButton) = {
    val formattedEndOfYear = languageUtilsImpl().ukDateTransformer(applicationConfig.currentTaxYear.finishes)

    val paragraphContent = if (role == Transferor) {
      Html(s"""<p class="govuk-body">${messages("pages.history.historic.ended")}</p>""" +
        s"""<p class="govuk-body">${messages("pages.history.historic.transferor", formattedEndOfYear)}</p>""")

    } else {
      Html(s"""<p class="govuk-body">${messages("pages.history.historic.ended")}</p>""" +
        s"""<p class="govuk-body">${messages("pages.history.historic.recipient", formattedEndOfYear)}</p>""")
    }

    val button = HistorySummaryButton("checkMarriageAllowance", messages("pages.history.historic.button"),
      controllers.routes.UpdateRelationshipController.claims.url)

    (paragraphContent, button)
  }

}
