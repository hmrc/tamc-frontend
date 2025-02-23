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

import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.coc.finished

import java.util.Locale

class FinishedChangeContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: finished = inject[finished]
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  val doc: Document = Jsoup.parse(view().toString())

  "Finished change" should {
    "Display correct page heading" in {
      doc.getElementById("pageHeading").text() shouldBe "Marriage Allowance cancelled"
    }

    "Display finished change content" in{
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        "You will receive an email acknowledging your cancellation within 24 hours.",
        "If you do not receive it, please check your spam or junk folder.",
        "You do not need to contact us.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }
  }

}
