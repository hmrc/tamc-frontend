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

package views.html.coc

import java.util.Locale

import models._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.jsoup.nodes.Document
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewTest
import viewModels.HistorySummaryViewModel

class HistorySummaryTest extends TamcViewTest with MockitoSugar {

  val cid = 1122L
  val timeStamp = new LocalDate().toString
  val hasAllowance = None
  val citizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo = Some(LoggedInUserInfo(cid, timeStamp, hasAllowance, Some(citizenName)))
  val endOfTaxYear = TaxYear.current.finishes
  val formattedEndOfYear = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

  override def view: Html = views.html.coc.history_summary(loggedInUserInfo, createViewModel(isActive = true, isRecipient = true))

  def createDocument(isActive: Boolean, isRecipient: Boolean): Document = {
    val viewModel = createViewModel(isActive, isRecipient)
    doc(views.html.coc.history_summary(loggedInUserInfo, viewModel))
  }

  private def createViewModel(isActive: Boolean, isRecipient: Boolean): HistorySummaryViewModel ={
    val maxBenefit = 200
    val maxPATransfer = 12500
    val relationShipRecord = mock[RelationshipRecord]

    lazy val activeHistoryViewModel = HistorySummaryViewModel(isActiveRecord = true, isHistoricActiveRecord = false,
      Some(relationShipRecord), None, maxPATransfer, maxBenefit, endOfTaxYear)

    lazy val historicHistoryViewModel = HistorySummaryViewModel(isActiveRecord = false, isHistoricActiveRecord = true,
      None, Some(Seq(relationShipRecord)), maxPATransfer, maxBenefit, endOfTaxYear)

    (isActive, isRecipient) match {
      case(true, true) => {
        when(relationShipRecord.participant).thenReturn("Recipient")
        activeHistoryViewModel

      }
      case(true, false) => {
        when(relationShipRecord.participant).thenReturn("Transferor")
        activeHistoryViewModel
      }
      case(false, true) => {
        when(relationShipRecord.participant).thenReturn("Recipient")
        historicHistoryViewModel
      }

      case(false, false) => {
        when(relationShipRecord.participant).thenReturn("Transferor")
        historicHistoryViewModel
      }
    }
  }

  "HistorySummary page" should {

    behave like pageWithTitle(messagesApi("title.history"))
    behave like pageWithCombinedHeader(s"${citizenName.firstName.getOrElse("")} ${citizenName.lastName.getOrElse("")}", messagesApi("title.history"))

  }

  "contain the correct paragraphs" when {

    "a user is an active recipient" in {

      val doc = createDocument(isActive = true, isRecipient = true)

      doc should haveParagraphWithText(messagesApi("pages.history.active.recipient.paragraph1", "12,500"))
      doc should haveParagraphWithText(messagesApi("pages.history.active.recipient.paragraph2", "200"))

    }

    "a user is an active transferor" in {

      val doc = createDocument(isActive = true, isRecipient = false)

      doc should haveParagraphWithText(messagesApi("pages.history.active.transferor"))
    }

    "a user is a historic recipient" in {

      val doc = createDocument(isActive = false, isRecipient = true)

      doc should haveParagraphWithText(messagesApi("pages.history.historic.ended"))
      doc should haveParagraphWithText(messagesApi("pages.history.historic.recipient", formattedEndOfYear))

    }

    "a user is a historic transferor" in {

      val doc = createDocument(isActive = false, isRecipient = false)

      doc should haveParagraphWithText(messagesApi("pages.history.historic.ended"))
      doc should haveParagraphWithText(messagesApi("pages.history.historic.transferor", formattedEndOfYear))

    }

  }

  "contain the correct button" when {

    "a user is active" in {

      val doc = createDocument(isActive = true, isRecipient = true)

      doc should haveLinkElement("checkOrUpdateMarriageAllowance", "", messagesApi("pages.history.active.button"))
    }

    "a user is historic active" in {

      val doc = createDocument(isActive = false, isRecipient = true)

      doc should haveLinkElement("checkMarriageAllowance", "", messagesApi("pages.history.historic.button"))
    }

  }
}