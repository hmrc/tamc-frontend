/*
 * Copyright 2024 HM Revenue & Customs
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
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import views.helpers.{EnglishLangaugeUtils, LanguageUtils}
import views.html.coc.history_summary

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale


class HistorySummaryViewModelTest extends BaseTest with Injecting with NinoGenerator {

  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]
  val view: history_summary = inject[history_summary]
  val languageUtils: LanguageUtils = EnglishLangaugeUtils
  val historySummaryViewModelImpl: HistorySummaryViewModelImpl = instanceOf[HistorySummaryViewModelImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino

  val currentOfTaxYear: Int = TaxYear.current.currentYear
  val endOfTaxYear: LocalDate = TaxYear.current.finishes
  val maxPATransfer: Int = applicationConfig.PERSONAL_ALLOWANCE(currentOfTaxYear)
  val maxBenefit: Int = applicationConfig.MAX_BENEFIT(currentOfTaxYear)
  val maxPaTransferFormatted: String = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER())
  val formattedEndOfYear: String = languageUtils.ukDateTransformer(endOfTaxYear)

  val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo: LoggedInUserInfo = LoggedInUserInfo(cid = 1122L, timestamp = LocalDate.now().toString, has_allowance = None, name = Some(citizenName))
  val expectedDisplayName = "Test User"
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])


  "HistorySummaryViewModel" should {

    "create a view model with the correct content" when {

      "marriage allowance has not been cancelled" when {

        "the person is a recipient " in {
          val marriageAllowanceCancelled = false
          val expectedHistorySummaryButton = HistorySummaryButton("checkOrUpdateMarriageAllowance", "Check or update your Marriage Allowance",
            controllers.UpdateRelationship.routes.ChooseController.decision().url)

          val role = Recipient
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(
            s"""<p class="govuk-body">${s"Your partner is currently using Marriage Allowance to transfer " +
              s"£$maxPaTransferFormatted of their Personal Allowance to you."}</p>""".stripMargin +
            s"""<p class="govuk-body">${s"This can reduce the tax you pay by up to £$maxBenefit a year."}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

        "The person is a transferor" in {
          val marriageAllowanceCancelled = false
          val expectedHistorySummaryButton = HistorySummaryButton("checkOrUpdateMarriageAllowance", "Check or update your Marriage Allowance",
            controllers.UpdateRelationship.routes.ChooseController.decision().url)
          val role = Transferor
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"""<p class="govuk-body">${"You are currently helping your partner benefit from Marriage Allowance."}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }
      }

      "The record has cancelled marriage allowance" when {

        "the person is a recipient" in {
          val marriageAllowanceCancelled = true
          val expectedHistorySummaryButton = HistorySummaryButton("checkMarriageAllowance", "Check your Marriage Allowance claims",
            controllers.UpdateRelationship.routes.ClaimsController.claims().url)

          val role = Recipient
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"""<p class="govuk-body">${"Your Marriage Allowance claim has ended."}</p>""" +
            s"""<p class="govuk-body">${s"You will keep the tax-free allowances transferred to you until $formattedEndOfYear."}</p>""")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

        "the person is a transferor" in {
          val marriageAllowanceCancelled = true
          val expectedHistorySummaryButton = HistorySummaryButton("checkMarriageAllowance", "Check your Marriage Allowance claims",
            controllers.UpdateRelationship.routes.ClaimsController.claims().url)

          val role = Transferor
          val viewModel = historySummaryViewModelImpl(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"""<p class="govuk-body">${"Your Marriage Allowance claim has ended."}</p>""" +
            s"""<p class="govuk-body">${s"You will keep the tax-free allowances transferred by you until $formattedEndOfYear."}</p>""")

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
