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

package views.UpdateRelationship

import forms.coc.CheckClaimOrCancelDecisionForm
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.coc.decision

import java.util.Locale

class ChooseContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: decision = inject[decision]
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  lazy val nino: String = generateNino().nino
  val doc: Document = Jsoup.parse(view(CheckClaimOrCancelDecisionForm.form()).toString())

  "Choose page - get view" should {

    "Display correct page heading" in {
      doc.getElementById("pageHeading").text() shouldBe "What do you want to do?"
    }

    "Display correct page content & radio options" in {
      doc.getElementsByClass("govuk-label govuk-radios__label")
        .eachText()
        .toArray() shouldBe Array("Check current or earlier Marriage Allowance claims", "Stop current Marriage Allowance claim")
    }

    "Display continue button" in {
      doc.getElementById("submit").text() shouldBe "Continue"
    }

    "Display all correct page text" in {
      doc.getElementsByTag("p")
        .eachText()
        .toArray() shouldBe Array("Beta This is a new service – your feedback will help us to improve it.")
    }
  }

}
