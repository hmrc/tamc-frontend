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

import forms.coc._
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.coc.reason_for_change

import java.util.Locale

class MakeChangesContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: reason_for_change = inject[reason_for_change]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  lazy val nino: String = generateNino().nino

  "Make change - get view" should {

    "display correct pageHeading" in {
      val pageHeading = Jsoup.parse(view(MakeChangesDecisionForm.form()).toString()).getElementById("pageHeading").text()

      pageHeading shouldBe "Why do you need to stop your Marriage Allowance?"
    }

    "show all appropriate radio buttons" in {
      val radioButtons = Jsoup.parse(view(MakeChangesDecisionForm.form()).toString())
        .getElementsByClass("govuk-label govuk-radios__label")
        .eachText()
        .toArray()

      radioButtons shouldBe Array(
        "Do not need Marriage Allowance any more",
        "Divorce, end of civil partnership or legally separated",
        "Bereavement"
      )
    }

    "Display continue button" in {
      val button = Jsoup.parse(view(MakeChangesDecisionForm.form()).toString()).getElementById("submit").text()

      button shouldBe "Continue"
    }

    "Display all correct page text" in {
      val pageText = Jsoup.parse(view(MakeChangesDecisionForm.form()).toString()).getElementsByTag("p").eachText().toArray()

      pageText shouldBe Array("Beta This is a new service – your feedback will help us to improve it.")
    }
  }

}
