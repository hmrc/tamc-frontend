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

import java.util.Locale

import config.ApplicationConfig._
import models._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest


class HistorySummaryViewModelTest extends TamcViewModelTest {

  lazy val currentOfTaxYear: Int = TaxYear.current.currentYear
  lazy val endOfTaxYear: LocalDate = TaxYear.current.finishes
  lazy val maxPATransfer: Int = PERSONAL_ALLOWANCE(currentOfTaxYear)
  lazy val maxBenefit: Int = MAX_BENEFIT(currentOfTaxYear)
  lazy val maxPaTransferFormatted: Int = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(currentOfTaxYear)
  lazy val formattedEndOfTaxYear: String = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))


  val citizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo = LoggedInUserInfo(cid = 1122L, timestamp = new LocalDate().toString, has_allowance = None, name = Some(citizenName))

  def createButtonForHistorySummaryView: HistorySummaryButton = {
    HistorySummaryButton("checkOrUpdateMarriageAllowance", messagesApi("pages.history.active.button"),
      controllers.routes.UpdateRelationshipController.decision().url)
  }

  "HistorySummaryViewModel" should {

    "create a view model with the correct content" when {

      "marriage allowance has not been cancelled" when {

        "the person is a recipient " in {

          val role = Recipient
          val marriageAllowanceCancelled = false
          val viewModel = HistorySummaryViewModel(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.active.recipient.paragraph1", maxPaTransferFormatted)}</p>" +
            s"<p>${messagesApi("pages.history.active.recipient.paragraph2", maxBenefit)}</P>")

          val expectedHistorySummaryButton = HistorySummaryButton("checkOrUpdateMarriageAllowance", messagesApi("pages.history.active.button"),
            controllers.routes.UpdateRelationshipController.decision().url)

          val expectedDisplayName = "Test User"

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

        "The person is a transferor" in {

          val role = Transferor
          val marriageAllowanceCancelled = false
          val viewModel = HistorySummaryViewModel(role, marriageAllowanceCancelled, loggedInUserInfo)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.active.transferor")}</p>")

          val expectedHistorySummaryButton = HistorySummaryButton("checkOrUpdateMarriageAllowance", messagesApi("pages.history.active.button"),
            controllers.routes.UpdateRelationshipController.decision().url)

          val expectedDisplayName = "Test User"

          viewModel shouldBe HistorySummaryViewModel(expectedContent, expectedHistorySummaryButton, expectedDisplayName)
        }

      }

      "The record has cancelled marriage allowance" when {

//        "the person is a recipient" in {
//
//          val role = Recipient
//          val marriageAllowanceCancelled = true
//          val viewModel = HistorySummaryViewModel(role, marriageAllowanceCancelled, loggedInUserInfo)
//
//          val expectedContent = Html(s"<p>${messagesApi("pages.history.historic.ended")}</p>" +
//            s"<p>${messagesApi("pages.history.historic.recipient", formattedEndOfTaxYear)}</P>")
//
//          val expectedDisplayName = "Test User"
//
//          viewModel shouldBe HistorySummaryViewModel(expectedContent, createButtonForHistorySummaryView, expectedDisplayName)
//        }
//
//        "the person is a transferor" in {
//          val viewModel = buildHistorySummaryViewModel(Transferor)
//
//          val expectedContent = Html(s"<p>${messagesApi("pages.history.historic.ended")}</p>" +
//            s"<p>${messagesApi("pages.history.historic.transferor", formattedEndOfTaxYear)}</P>")
//
//          viewModel shouldBe HistorySummaryViewModel(expectedContent, createButtonForHistorySummaryView, name)
//        }
      }
    }

  }
}
