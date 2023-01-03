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
import models._
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.BaseTest
import views.helpers.{EnglishLangaugeUtils, LanguageUtils}

import java.time.LocalDate


class HistorySummaryViewModelTest extends BaseTest {

  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]
  val languageUtils: LanguageUtils = EnglishLangaugeUtils
  val historySummaryViewModelImpl: HistorySummaryViewModelImpl = instanceOf[HistorySummaryViewModelImpl]

  val currentOfTaxYear: Int = TaxYear.current.currentYear
  val endOfTaxYear: LocalDate = TaxYear.current.finishes
  val maxPATransfer: Int = applicationConfig.PERSONAL_ALLOWANCE(currentOfTaxYear)
  val maxBenefit: Int = applicationConfig.MAX_BENEFIT(currentOfTaxYear)
  val maxPaTransferFormatted: Int = applicationConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(currentOfTaxYear)
  val formattedEndOfYear = languageUtils.ukDateTransformer(endOfTaxYear)

  val citizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo = LoggedInUserInfo(cid = 1122L, timestamp = LocalDate.now().toString, has_allowance = None, name = Some(citizenName))
  val expectedDisplayName = "Test User"


  "HistorySummaryViewModel" should {

    "create a view model with the correct content" when {

      "marriage allowance has not been cancelled" when {

        "the person is a recipient " in {
          val marriageAllowanceCancelled = false
          val expectedHistorySummaryButton = HistorySummaryButton("checkOrUpdateMarriageAllowance", messages("pages.history.active.button"),
            controllers.routes.UpdateRelationshipController.decision.url)

          val role = Recipient
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"""<p class="govuk-body">${messages("pages.history.active.recipient.paragraph1", maxPaTransferFormatted)}</p>""" +
            s"""<p class="govuk-body">${messages("pages.history.active.recipient.paragraph2", maxBenefit)}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

        "The person is a transferor" in {
          val marriageAllowanceCancelled = false
          val expectedHistorySummaryButton = HistorySummaryButton("checkOrUpdateMarriageAllowance", messages("pages.history.active.button"),
            controllers.routes.UpdateRelationshipController.decision.url)

          val role = Transferor
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)
          val expectedContent = Html(s"""<p class="govuk-body">${messages("pages.history.active.transferor")}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

      }

      "The record has cancelled marriage allowance" when {

        "the person is a recipient" in {
          val marriageAllowanceCancelled = true
          val expectedHistorySummaryButton = HistorySummaryButton("checkMarriageAllowance", messages("pages.history.historic.button"),
            controllers.routes.UpdateRelationshipController.claims.url)

          val role = Recipient
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"""<p class="govuk-body">${messages("pages.history.historic.ended")}</p>""" +
            s"""<p class="govuk-body">${messages("pages.history.historic.recipient", formattedEndOfYear)}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

        "the person is a transferor" in {
          val marriageAllowanceCancelled = true
          val expectedHistorySummaryButton = HistorySummaryButton("checkMarriageAllowance", messages("pages.history.historic.button"),
            controllers.routes.UpdateRelationshipController.claims.url)

          val role = Transferor
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"""<p class="govuk-body">${messages("pages.history.historic.ended")}</p>""" +
            s"""<p class="govuk-body">${messages("pages.history.historic.transferor", formattedEndOfYear)}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }
      }
    }

    "There is no citizen name to allow a display name to be created" in  {
      val marriageAllowanceCancelled = false
      val role = Transferor
      val loggedInUserInfo = LoggedInUserInfo(cid = 1122L, timestamp = LocalDate.now().toString, has_allowance = None, name = None)

      val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

      viewModel.displayName shouldBe ""
    }
  }
}