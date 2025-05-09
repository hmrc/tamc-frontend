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

package views

import config.ApplicationConfig
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import utils.BaseTest
import controllers.EligibilityCalculatorController

class CalculatorResultTest extends BaseTest {

  val applicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]
  lazy val controller: EligibilityCalculatorController = app.injector.instanceOf[EligibilityCalculatorController]

  "Check eligibility benefit" should {

    "be GBP 230 if transferor income=0 (< 11500) and recipient income=13000 (13000-11500)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "0", "recipient-income" -> "13000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("Based on the information you have given us, as a couple you would benefit by around")
    }

    "Transferor not eligible if transferor income is more than maximum limit" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 100)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> overThreshold, "recipient-income" -> "51000")
      val result = controller.gdsCalculatorAction()(request)

      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. As the person making the transfer, your income must be below £$higherThreshold."
    }

    "be GBP 238 if transferor income=9000 (< 9540) and recipient income=12000 (> 11660)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9000", "recipient-income" -> "16000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("Based on the information you have given us, as a couple you would benefit by around")
    }

    "be negative (12) if transferor income=11000 (11313-12570) and recipient income=12571 (12570-13827)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "11000", "recipient-income" -> "12571")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple."
    }

    "be GBP 230 if transferor income=10000 (9540-10600) and recipient income=20000 (11660-42385)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "10000", "recipient-income" -> "20000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("Based on the information you have given us, as a couple you would benefit by around")
    }

    "be 0 if transferor income=9540  and recipient income=11501" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9540", "recipient-income" -> lowerThreshold)
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("You will not benefit as a couple.")
    }

    "show ’recipient is not eligible’ if transferor income=9540  and recipient income<11501" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9540", "recipient-income" -> "11000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "show ’recipient is not eligible’ if transferor income=9540  and recipient income>45000" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 200)
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9540", "recipient-income" -> overThreshold)
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }
  }

  "Check validation message" should {

    "display form error message (one error)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/benefit-calculator")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "display form error message (multiple errors)" in {
      val request = FakeRequest().withMethod("POST")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/benefit-calculator")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
      document.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if transferor income is not provided (None)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income is not provided (Empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income is not provided (Blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> " ", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income contains letters" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "abc", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income contains negative number" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "-1", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income exceeds max Int" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "2147483648", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income is not provided (None)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income is not provided (Empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income is not provided (Blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> " ", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income contains letters" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "abc", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe "Error: Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income contains negative number" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "-1", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe "Error: Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income exceeds max Int" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "recipient-income" -> "2147483648", "transferor-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe "Error: Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income=0 (< 9540) and recipient income=0 (10600-11660)" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "0", "recipient-income" -> "0")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "be displayed if transferor income=9000 (< 9540) and recipient income=5000 (< 10600)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9000", "recipient-income" -> "5000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
    }

    "be displayed if transferor income=9000 (< 9540) and recipient exeeds limit" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "9000", "recipient-income" -> (applicationConfig.MAX_LIMIT() + 1).toString)
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "be displayed if transferor income=10000 (9540-11000) and recipient exceeds limit" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "10000", "recipient-income" -> (applicationConfig.MAX_LIMIT() + 1).toString)
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }
    "be displayed if transferor income is below limit and recipient income=20000" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 1).toString,
        "recipient-income" -> "20000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple because your income is over £$lowerThreshold."
    }
    "be displayed if transferor income exceeds limit and recipient income=20000" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england", "transferor-income" -> "43001", "recipient-income" -> "20000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
    }

    "retain previously entered values" when {
      Seq("england", "wales", "scotland", "northernireland").foreach { country =>
        s"transferor and recipient income are set and country has '$country' selected" in {
          val request = FakeRequest()
            .withMethod("POST")
            .withFormUrlEncodedBody("country" -> country, "transferor-income" -> "25000", "recipient-income" -> "3000")
          val result = controller.gdsCalculatorAction()(request)
          val document = Jsoup.parse(contentAsString(result))
          document.getElementById(s"country-$country").hasAttr("checked") shouldBe true
        }
      }
    }
  }

}
