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
import views.html.multiyear.transfer.apply_by_post

class ApplyByPostTest extends BaseTest with ViewTestUtils with NinoGenerator {

  lazy val nino: String                                                  = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: Messages                          = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val applyByPostView: apply_by_post                                = instanceOf[apply_by_post]

  val taxYears: Seq[String] = Seq("currentTaxYear", "PreviousTaxYears")

  implicit val doc: Document                                             = Jsoup.parse(applyByPostView(taxYears).toString())

  "Apply By Post" should {
    "display the correct title" in {
      doc.title() shouldBe "You must apply by post - Marriage Allowance application - GOV.UK"
    }

    "display correct heading" in {
      doc.getElementsByTag("h1").text().shouldBe("You must apply by post")
    }

    "display dynamic text" when {
      "only previous tax years are available" in {
        val taxYears = Seq("previousTaxYears")
        implicit val doc: Document = Jsoup.parse(applyByPostView(taxYears).toString())
        doc.getElementsByTag("p").eachText().toArray.shouldBe(Array(
          "You cannot apply for previous tax years online.",
          "Apply for Marriage Allowance by post",
          "Beta This is a new service – your feedback will help us to improve it."
        ))
      }

      "both current and previous tax years are available" in {
        val taxYears = Seq("currentTaxYear", "previousTaxYears")
        implicit val doc: Document = Jsoup.parse(applyByPostView(taxYears).toString())
        doc.getElementsByTag("p").eachText().toArray.shouldBe(Array(
          "Your application includes a previous tax year. You cannot apply for previous tax years online.",
          "Apply for Marriage Allowance by post",
          "Beta This is a new service – your feedback will help us to improve it."
        ))
      }
    }

    "display correct link" in {
      doc.getElementById("apply-by-post-link").text().shouldBe("Apply for Marriage Allowance by post")
      doc.getElementById("apply-by-post-link").attr("href").shouldBe("https://www.gov.uk/guidance/apply-for-marriage-allowance-by-post")
    }

  }
}
