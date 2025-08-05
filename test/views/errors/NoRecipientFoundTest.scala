/*
 * Copyright 2025 HM Revenue & Customs
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

package views.errors

import models.auth.UserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import utils.BaseTest
import views.html.errors.recipient_not_found

class NoRecipientFoundTest extends BaseTest {

  implicit val request: UserRequest[?]                = UserRequest(FakeRequest(), None, true, None, false)
  override implicit lazy val messages: Messages       = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
    
  lazy val recipientNotFoundView: recipient_not_found = instanceOf[recipient_not_found]

  implicit val doc: Document = Jsoup.parse(recipientNotFoundView()(messages, request).toString())

  "Recipient Not Found" should {
    "display the correct title" in {
      doc.title() shouldBe "Error - Marriage Allowance - GOV.UK"
    }

    "display correct heading" in {
      doc.getElementsByTag("h1").text() shouldBe "We cannot find your partner’s HMRC details"
    }

    "display correct text" in {
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        "Check your partner’s information is correct and update your application.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "display correct link" in {
      doc.getElementById("partner-details-link").text() shouldBe "Check your partner’s information is correct"
      doc
        .getElementById("partner-details-link")
        .attr("href")                                   shouldBe "/marriage-allowance-application/transfer-allowance"
    }

  }
}
