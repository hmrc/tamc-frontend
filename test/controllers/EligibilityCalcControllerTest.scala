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

package controllers

import config.ApplicationConfig
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.FakeRequest
import play.api.test.Helpers.baseApplicationBuilder.injector
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import utils.BaseTest

//TODO: Update values in test names
class EligibilityCalcControllerTest extends BaseTest {

  val applicationConfig: ApplicationConfig = injector().instanceOf[ApplicationConfig]

  "Check eligibility benefit" should {

    "return 200 if form is submitted with no errors" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "0", "recipient-income" -> "13000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe OK
    }

    "return 200 if transferor income is more than maximum limit" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 100)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> overThreshold, "recipient-income" -> "51000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe OK
    }
  }

  "Check validation message" should {

    "return 400 when form is submitted with a single error" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 when form is submitted with multiple errors" in {
      val request = FakeRequest().withMethod("POST")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if transferor income is not provided (None)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if transferor income contains letters" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "abc", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "return 400 if transferor income contains negative number" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "-1", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if transferor income exceeds max Int" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "2147483648", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if recipient income is not provided (None)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if recipient income contains letters" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "abc", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if recipient income contains negative number" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "-1", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return 400 if recipient income exceeds max Int" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "2147483648", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      status(result) shouldBe BAD_REQUEST
    }
  }
  lazy val controller: EligibilityCalculatorController = app.injector.instanceOf[EligibilityCalculatorController]
}
