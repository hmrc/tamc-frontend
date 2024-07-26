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

package views.UpdateRelationship

import models.MarriageAllowanceEndingDates
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import views.html.coc.{cancel, stopAllowance}

import java.util.Locale

class StopAllowanceContentTest extends BaseTest with Injecting with NinoGenerator {

  val stopAllowanceView: stopAllowance = inject[stopAllowance]
  val cancelView: cancel = inject[cancel]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "Display Stop allowance page for a RECIPIENT" should {
    "Display stop allowance page heading" in {
      val pageHeading = Jsoup.parse(stopAllowanceView().toString).getElementById("pageHeading").text()

      pageHeading shouldBe "Your partner needs to stop the Marriage Allowance claim"
    }

    "Show correct stop allowance content" in {
      val content = Jsoup.parse(stopAllowanceView().toString).getElementsByTag("p").eachText().toArray()

      val expectedContent = Array(
        "As your partner is transferring their allowances to you, they will need to stop the claim.",
        "Your partner can do this from the Marriage Allowance section of their personal tax account.",
        "Back to your Marriage Allowance claim summary",
        "Beta This is a new service – your feedback will help us to improve it."
      )

      content shouldBe expectedContent
    }

    "Display back to summary page link" in {
      val summaryLinkText = Jsoup.parse(stopAllowanceView().toString).getElementById("backToSummary").text
      val summaryLinkHref = Jsoup.parse(stopAllowanceView().toString).getElementById("backToSummary").attr("href")

      summaryLinkText shouldBe "Back to your Marriage Allowance claim summary"
      summaryLinkHref shouldBe "/marriage-allowance-application/history"
    }
  }

  "Display cancel page for a TRANSFEROR" should {
    "Display cancel page heading" in {
      val pageHeading = Jsoup.parse(cancelView(MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))
          .toString())
          .getElementById("cancel-heading").text()

      pageHeading shouldBe "Cancelling Marriage Allowance"
    }

    "Display transferor content" in {
      val content = Jsoup.parse(cancelView(MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))
        .toString)
        .getElementsByTag("p")
        .eachText()
        .toArray()


      content shouldBe Array(
        s"We will cancel your Marriage Allowance, but it will remain in place until 5 April " +
          s"${TaxYear.current.finishes.getYear}, the end of the current tax year.",
        s"Your Personal Allowance will not include any Marriage Allowance from 6 April " +
          s"${TaxYear.current.next.starts.getYear}, the start of the new tax year.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "Display continue button" in {
      val button = Jsoup.parse(cancelView(MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))
          .toString)
          .getElementById("confirmUpdate").text()

      button shouldBe "Continue"
    }
  }

}
