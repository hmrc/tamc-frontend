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
  val formatter = java.text.NumberFormat.getIntegerInstance

  "Check eligibility benefit" should {

    "display potential MAX BENEFIT content: transferor has no income & recipient income over recipient allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "0",
        "recipient-income" -> (applicationConfig.RECIPIENT_ALLOWANCE + 1).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include(
        s"Based on the information you have given us, as a couple you would benefit by around £${applicationConfig.MAX_BENEFIT()} a year."
      )
    }

    "display potential MAX BENEFIT content: transferor income is under transferor allowance & recipient income much over recipient allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE - 1).toString,
        "recipient-income" -> (applicationConfig.RECIPIENT_ALLOWANCE + 15000).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include(
        s"Based on the information you have given us, as a couple you would benefit by around £${applicationConfig.MAX_BENEFIT()} a year."
      )
    }

    "display potential PART BENEFIT content: recipient income in between personal allowance & recipient allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "0",
        "recipient-income" -> (applicationConfig.RECIPIENT_ALLOWANCE - 1000).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include(
        s"Based on the information you have given us, as a couple you would benefit by around £${applicationConfig.MAX_BENEFIT() - 200} a year."
      )
    }

    "display NOT ELIGIBLE content: transferor income is more than maximum limit but lower than recipient income" in {
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 100)
      val aboveTransferor = formatter.format(applicationConfig.MAX_LIMIT() + 200)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> overThreshold,
        "recipient-income" -> aboveTransferor
      )
      val result = controller.gdsCalculatorAction()(request)

      status(result) shouldBe 200
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. As the person making the transfer, your income must be below £$higherThreshold."
    }

    "display NO BENEFIT content: recipient earns £1 over personal allowance, transferor is within max benefit threshold" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE -1).toString,
        "recipient-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 1).toString)
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple."
    }

    "display NO BENEFIT content: transferor earns over personal allowance, whilst recipient is within max limit" in {
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 1).toString,
        "recipient-income" -> (applicationConfig.MAX_LIMIT() - 10000).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple because your income is over £$lowerThreshold."
    }

    "display NOT ELIGIBLE content: recipient income equal to or below personal allowance for the year" in {
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE -1).toString,
        "recipient-income" -> applicationConfig.PERSONAL_ALLOWANCE().toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "dispay NOT ELIGIBLE content: recipient earns over max limit for eligibility" in {
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 200)
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE -1).toString,
        "recipient-income" -> overThreshold
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "display NOT ELIGIBLE content: recipient over max limit, even if transferor is over personal allowance" in {
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "9000",
        "recipient-income" -> (applicationConfig.MAX_LIMIT() + 1).toString)
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "display NOT ELIGIBLE content: both transferor and recipient income are 0 " in {
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "0",
        "recipient-income" -> "0"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "display INCORRECT ROLE content: transferor earns more than the recipient" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "9000",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
    }

    "display INCORRECT ROLE content: transferor earns more than the recipient, even if both incomes exceed the max limit" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "250000",
        "recipient-income" -> "150000")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
    }
  }

  "Check validation message" should {

    "display form error message, recipient income not provided (one error)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "9000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/benefit-calculator")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "display form error message, no incomes provided (multiple errors)" in {
      val request = FakeRequest().withMethod("POST")
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/benefit-calculator")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
      document.getElementById("recipient-income-error").text() shouldBe "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if transferor income is not provided (None)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income is not provided (Empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income is not provided (Blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> " ",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe "Error: Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income contains letters" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "abc",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe
        "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income contains negative number" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "-1",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe
        "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income exceeds max Int" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "2147483648",
        "recipient-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("transferor-income-error").text() shouldBe
        "Error: Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income is not provided (None)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" ->
          "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe
        "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income is not provided (Empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "recipient-income" -> "",
        "transferor-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe
        "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income is not provided (Blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "recipient-income" -> " ",
        "transferor-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe
        "Error: Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income contains letters" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "recipient-income" -> "abc",
        "transferor-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe
        "Error: Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income contains negative number" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "recipient-income" -> "-1",
        "transferor-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe
        "Error: Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income exceeds max Int" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "recipient-income" -> "2147483648",
        "transferor-income" -> "5000"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementById("recipient-income-error").text() shouldBe
        "Error: Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
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
