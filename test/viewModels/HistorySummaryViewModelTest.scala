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


class HistorySummaryViewModelTest extends TamcViewModelTest with MockitoSugar {

  lazy val currentOfTaxYear: Int = TaxYear.current.currentYear
  lazy val endOfTaxYear: LocalDate = TaxYear.current.finishes
  lazy val maxPATransfer: Int = PERSONAL_ALLOWANCE(currentOfTaxYear)
  lazy val maxBenefit: Int = MAX_BENEFIT(currentOfTaxYear)
  lazy val maxPaTransferFormatted: Int = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(currentOfTaxYear)
  lazy val formattedEndOfTaxYear: String = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

  "HistorySummaryViewModel" should {

    "create a view model with the correct content" when {

      "the record is active " when {

        "the person is a recipient " in {
          val viewModel = buildHistorySummaryViewModel(Recipient, Active)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.active.recipient.paragraph1", maxPaTransferFormatted)}</p>" +
            s"<p>${messagesApi("pages.history.active.recipient.paragraph2", maxBenefit)}</P>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, createButtonForHistorySummaryView(Active))
        }

        "The person is a transferor" in {
          val viewModel = buildHistorySummaryViewModel(Transferor, Active)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.active.transferor")}</p>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, createButtonForHistorySummaryView(Active))
        }

      }

      "The record is historic" when {

        "the person is a recipient" in {
          val viewModel = buildHistorySummaryViewModel(Recipient, Historic)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.historic.ended")}</p>" +
            s"<p>${messagesApi("pages.history.historic.recipient", formattedEndOfTaxYear)}</P>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, createButtonForHistorySummaryView(Historic))
        }

        "the person is a transferor" in {
          val viewModel = buildHistorySummaryViewModel(Transferor, Historic)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.historic.ended")}</p>" +
            s"<p>${messagesApi("pages.history.historic.transferor", formattedEndOfTaxYear)}</P>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent, createButtonForHistorySummaryView(Historic))
        }
      }
    }

  }
}
