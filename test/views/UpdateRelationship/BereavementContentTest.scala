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

import models._
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.coc.bereavement

import java.util.Locale

class BereavementContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: bereavement = inject[bereavement]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "Bereavement page" should {

    "Display correct page heading" in {
      val recipientPageHeading = Jsoup.parse(view(Recipient).toString()).getElementById("bereavement").text()
      val transferorPageHeading = Jsoup.parse(view(Transferor).toString()).getElementById("bereavement").text()

      recipientPageHeading shouldBe transferorPageHeading
      recipientPageHeading shouldBe "We are sorry for your loss"
    }

    "Display all recipient text" in {
      val content = Jsoup.parse(view(Recipient).toString()).getElementsByTag("p").eachText().toArray()

      val expected = Array(
        "You can contact the Income Tax general enquiries helpline to tell us about a bereavement.",
        "You will keep the Marriage Allowance your partner transferred to you until the end of the tax year.",
        "Beta This is a new service – your feedback will help us to improve it."
      )

      content shouldBe expected
    }

    "Display all transferor text" in {
      val content = Jsoup.parse(view(Transferor).toString()).getElementsByTag("p").eachText().toArray()

      val expected = Array(
        "You can contact the Income Tax general enquiries helpline to tell us about a bereavement.",
        "If your partner dies after you have transferred some of your Personal Allowance to them:",
        "Beta This is a new service – your feedback will help us to improve it."
      )

      content shouldBe expected
    }

    "recipient bullet list" in {
      val bulletList = Jsoup.parse(view(Recipient).toString()).getElementById("main-content").getElementsByTag("li").eachText().toArray()

      bulletList shouldBe Array()
    }

    "transferor bullet list" in {
      val bulletList = Jsoup.parse(view(Transferor).toString()).getElementById("main-content").getElementsByTag("li").eachText().toArray()

      val expected = Array(
        "their estate will be treated as having the extra Personal Allowance you transferred to them",
        "your Personal Allowance will go back to the normal amount at the end of the tax year (5 April)"
      )

      bulletList shouldBe expected
    }

    "Display contact helpline link" in {
      val linkText = Jsoup.parse(view(Transferor).toString()).getElementById("main-content").getElementById("helpline").text()
      val linkHref = Jsoup.parse(view(Transferor).toString()).getElementById("main-content").getElementsByClass("govuk-link").attr("href")

      val expectedLinkText = "contact the Income Tax general enquiries helpline"

      linkText shouldBe expectedLinkText
      linkHref shouldBe
        "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
    }
  }

}
