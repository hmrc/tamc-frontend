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
import org.jsoup.nodes.Document
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
  val recipientDoc: Document = Jsoup.parse(view(Recipient).toString())
  val transferorDoc: Document = Jsoup.parse(view(Transferor).toString())

  "Bereavement page" should {

    "Display correct page heading" in {
      recipientDoc.getElementById("bereavement").text() shouldBe "We are sorry for your loss"
      transferorDoc.getElementById("bereavement").text() shouldBe "We are sorry for your loss"
    }

    "Display all recipient text" in {
      recipientDoc.getElementsByTag("p").eachText().toArray() shouldBe Array(
        "You can contact the Income Tax general enquiries helpline to tell us about a bereavement.",
        "You will keep the Marriage Allowance your partner transferred to you until the end of the tax year.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "Display all transferor text" in {
      transferorDoc.getElementsByTag("p").eachText().toArray() shouldBe Array(
        "You can contact the Income Tax general enquiries helpline to tell us about a bereavement.",
        "If your partner dies after you have transferred some of your Personal Allowance to them:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "recipient bullet list" in {
      recipientDoc.getElementById("main-content").getElementsByTag("li").eachText().toArray() shouldBe Array()
    }

    "transferor bullet list" in {
      transferorDoc.getElementById("main-content").getElementsByTag("li").eachText().toArray() shouldBe Array(
        "their estate will be treated as having the extra Personal Allowance you transferred to them",
        "your Personal Allowance will go back to the normal amount at the end of the tax year (5 April)"
      )
    }

    "Display contact helpline link" in {
      transferorDoc.getElementById("main-content")
        .getElementById("helpline")
        .text() shouldBe "contact the Income Tax general enquiries helpline"
      
      recipientDoc.getElementById("main-content").getElementsByClass("govuk-link").attr("href") shouldBe
        "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"
    }
  }

}
