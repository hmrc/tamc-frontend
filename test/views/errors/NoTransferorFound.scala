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

package views.errors

import models.auth.UserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import utils.BaseTest
import views.html.errors.transferor_not_found

class NoTransferorFound extends BaseTest {

  implicit val request: UserRequest[?]                  = UserRequest(FakeRequest(), None, true, None, false)
  override implicit lazy val messages: Messages         = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val transferorNotFoundView: transferor_not_found = instanceOf[transferor_not_found]

  implicit val doc: Document = Jsoup.parse(transferorNotFoundView()(messages, request).toString())

  "Transferor Not Found" should {
    "display the correct title" in {
      doc.title() shouldBe "Error - Marriage Allowance - GOV.UK"
    }

    "display correct heading" in {
      doc.getElementsByTag("h1").text() shouldBe "We cannot find your Marriage Allowance details"
    }

    "display correct text" in {
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        "If you need to make a change to your Marriage Allowance, contact HMRC Income Tax general enquiries.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "display correct link" in {
      doc.getElementById("income-tax-enquiries-link").text() shouldBe "contact HMRC Income Tax general enquiries."
      doc
        .getElementById("income-tax-enquiries-link")
        .attr("href")                                        shouldBe "https://www.gov.uk/find-hmrc-contacts/income-tax-enquiries"
    }

  }
}
