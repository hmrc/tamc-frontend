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

package views.multiyear.transfer

import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator, ViewTestUtils}
import views.html.multiyear.transfer.dont_apply_current_tax_year

class DoNotApplyTest extends BaseTest with ViewTestUtils with NinoGenerator {

  lazy val nino: String                                                  = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: Messages                          = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val doNotApplyView: dont_apply_current_tax_year                   = instanceOf[dont_apply_current_tax_year]
  implicit val doc: Document                                             = Jsoup.parse(doNotApplyView().toString())

  "Don't apply current tax year" should {
    "display correct title" in {
      doc.title() shouldBe "You don't want to apply for the current tax year onwards - Marriage Allowance - GOV.UK"
    }

    "display correct heading" in {
      doc.getElementsByTag("h1").text() shouldBe "You don't want to apply for the current tax year onwards"
    }

    "display correct text" in {
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        "You can:",
        "change the years you want to apply for",
        "find out how Marriage Allowance works",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "display correct bullet text and links" in {

      val bullets =
        doc.getElementById("do-not-apply-current-tax-year-list").getElementsByTag("li").eachText().toArray

      bullets                                              shouldBe Array(
        "change the years you want to apply for",
        "find out how Marriage Allowance works"
      )
      doc.getElementById("change-year-link").attr("href")  shouldBe "/marriage-allowance-application/eligible-years"
      doc.getElementById("how-it-works-link").attr("href") shouldBe "/marriage-allowance-application/how-it-works"
    }

  }
}
