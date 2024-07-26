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

import forms.EmailForm
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.coc.email

import java.util.Locale

class ConfirmEmailContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: email = inject[email]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "Change of circumstances confirm email page" should {
    "Display correct heading" in {
      val pageHeading = Jsoup.parse(view(EmailForm.emailForm).toString()).getElementById("pageHeading").text()

      pageHeading shouldBe "Confirmation email"
    }

    "Display correct page content" in {
      val content = Jsoup.parse(view(EmailForm.emailForm).toString()).getElementsByTag("p").eachText().toArray()

      content shouldBe Array(
        "We will email confirmation that you have cancelled your Marriage Allowance within 24 hours.",
        "We will not share your email with anyone else.",
        "Beta This is a new service – your feedback will help us to improve it.")
    }

    "Display email text input with label" in {
      val document = Jsoup.parse(view(EmailForm.emailForm).toString())
      val labelContent = document.getElementsByClass("govuk-label").text()
      val formInput = document.getElementsByTag("input").eachAttr("type")

      labelContent shouldBe "Your email address "
      formInput contains "text"
    }
  }

}
