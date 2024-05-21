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

package errorHandler

import config.ApplicationConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.Html
import utils.UnitSpec

import java.util.Locale

class ErrorHandlerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting{

  implicit val request: Request[_] = FakeRequest()

  val mockApplicationConfig: ApplicationConfig = mock[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder().build()

  val errorHandler: ErrorHandler = inject[ErrorHandler]
  implicit val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "standardErrorTemplate" must {
    "return the global error view" in {
      val title = "testTitle"
      val heading = "testHeading"
      val message = "testMessage"

      val standardErrorTemplate: Html = errorHandler.standardErrorTemplate(title, heading, message)
      val doc: Document = Jsoup.parse(standardErrorTemplate.toString())

      val docTitle = doc.select("title").text()
      val docHeading = doc.select("#errorHeading").text()
      val docMessage = doc.select("#errorText").text()

      docTitle shouldBe title + " GOV.UK"
      docHeading shouldBe heading
      docMessage shouldBe message
    }
  }

  "notFoundTemplate" must {
    "return the page not found view" in {
      val notFoundTemplate: Html = errorHandler.notFoundTemplate
      val doc: Document = Jsoup.parse(notFoundTemplate.toString())

      val docTitle = doc.select("title").text()
      docTitle should include(messages("title.pattern"))
    }
  }
}