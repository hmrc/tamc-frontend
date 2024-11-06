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

package views

import controllers.EligibilityCalculatorController
import org.jsoup.Jsoup
import org.scalatest.Assertion
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import utils.BaseTest

import scala.concurrent.Future

class CalculatorViewTest extends BaseTest {

  val controller: EligibilityCalculatorController = app.injector.instanceOf[EligibilityCalculatorController]
  implicit val request: Request[AnyContent] = FakeRequest()

  val result: Future[Result] = controller.gdsCalculator()(request)

  def getContent(id: String = "calculator", expected: String): Assertion = {
    Jsoup.parse(contentAsString(result)).getElementById(id).text() should include(expected)
  }

  "calculator" should {
    "display correct title" in {
      getContent(
        "pageHeading",
        "Marriage Allowance calculator"
      )
    }

    "display correct paragraphs" in {
      getContent(
        id = "how-much-tax-you-can-save",
        expected = "Find out how much tax you could save as a couple by applying for Marriage Allowance."
      )
    }

    "display radio button header" in {
      getContent(expected = "Where do you live?")
      getContent(expected = "Where you live can affect how much you benefit because of different tax rates.")
    }

    "display radio buttons" in {
      getContent(expected = "England")
      getContent(expected = "Northern Ireland")
      getContent(expected = "Scotland")
      getContent(expected = "Wales")
    }

    "display income headers" in {
      getContent(expected = "Your income (low), before tax is taken off")
      getContent(expected = "Your partner’s income (high), before tax is taken off")
    }

  }

}
