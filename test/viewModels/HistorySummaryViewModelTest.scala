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

import models.RelationshipRecord
import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest


class HistorySummaryViewModelTest extends TamcViewModelTest with MockitoSugar {

  val maxBenefit = 200
  val maxPATransfer = 12500
  val endOfTaxYear = TaxYear.current.finishes

  "HistorySummaryViewModel" should {

    "create a view model with the correct content" when {

      "the record is active " when {

        "the person is a recipient " in {

          val isRecordActive = true
          val isRecordHistoric = false
          val activeRelationshipMock = mock[RelationshipRecord]
          val historicRelationShipMock = None
          val maxPaTransferFormatted = "12,500"

          //TODO constant
          when(activeRelationshipMock.participant).thenReturn("Recipient")

          val viewModel = HistorySummaryViewModel(isRecordActive, isRecordHistoric, Some(activeRelationshipMock),
            historicRelationShipMock, maxPATransfer, maxBenefit, endOfTaxYear)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.active.recipient.paragraph1", maxPaTransferFormatted)}</p>" +
            s"<p>${messagesApi("pages.history.active.recipient.paragraph2", maxBenefit)}</P>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent)

        }

        "The person is a transferor" in {

          val isRecordActive = true
          val isRecordHistoric = false
          val activeRelationshipMock = mock[RelationshipRecord]
          val historicRelationShipMock = None

          //TODO constant
          when(activeRelationshipMock.participant).thenReturn("Transferor")

          val viewModel = HistorySummaryViewModel(isRecordActive, isRecordHistoric, Some(activeRelationshipMock),
            historicRelationShipMock, maxPATransfer, maxBenefit, endOfTaxYear)

          val expectedContent = Html(s"<p>${messagesApi("pages.history.active.transferor")}</p>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent)

        }

      }

      "The record is historic" when {

        "the person is a recipient" in {

          val isRecordActive = false
          val isRecordHistoric = true
          val activeRelationshipMock = None
          val historicRelationShipMock = mock[RelationshipRecord]

          //TODO constant
          when(historicRelationShipMock.participant).thenReturn("Recipient")

          val viewModel = HistorySummaryViewModel(isRecordActive, isRecordHistoric, activeRelationshipMock,
            Some(Seq(historicRelationShipMock)), maxPATransfer, maxBenefit, endOfTaxYear)

          val formatedEndOfTaxYear = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

          val expectedContent = Html(s"<p>${messagesApi("pages.history.historic.ended")}</p>" +
            s"<p>${messagesApi("pages.history.historic.recipient", formatedEndOfTaxYear)}</P>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent)

        }

        "the person is a transferor" in {

          val isRecordActive = false
          val isRecordHistoric = true
          val activeRelationshipMock = None
          val historicRelationShipMock = mock[RelationshipRecord]

          //TODO constant
          when(historicRelationShipMock.participant).thenReturn("Transferor")

          val viewModel = HistorySummaryViewModel(isRecordActive, isRecordHistoric, activeRelationshipMock,
            Some(Seq(historicRelationShipMock)), maxPATransfer, maxBenefit, endOfTaxYear)

          val formatedEndOfTaxYear = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

          val expectedContent = Html(s"<p>${messagesApi("pages.history.historic.ended")}</p>" +
            s"<p>${messagesApi("pages.history.historic.transferor", formatedEndOfTaxYear)}</P>")

          viewModel shouldBe HistorySummaryViewModel(expectedContent)

        }
      }
    }

    "throw an exception " when {

      "no active records are found" in {

        val isRecordActive = true
        val isRecordHistoric = false
        val activeRelationshipMock = None
        val historicRelationShipMock = None

        lazy val viewModel = HistorySummaryViewModel(isRecordActive, isRecordHistoric, activeRelationshipMock,
          historicRelationShipMock, maxPATransfer, maxBenefit, endOfTaxYear)

        val thrown = the[RuntimeException] thrownBy viewModel
        thrown.getMessage shouldBe "No active relationship record found"
      }

      "no historic records are found" in {

        val isRecordActive = false
        val isRecordHistoric = true
        val activeRelationshipMock = None
        val historicRelationShipMock = None

        lazy val viewModel = HistorySummaryViewModel(isRecordActive, isRecordHistoric, activeRelationshipMock,
          historicRelationShipMock, maxPATransfer, maxBenefit, endOfTaxYear)

        val thrown = the[RuntimeException] thrownBy viewModel
        thrown.getMessage shouldBe "No historic relationship record found"

      }
    }
  }
}
