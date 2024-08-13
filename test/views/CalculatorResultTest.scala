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

import config.ApplicationConfig
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.baseApplicationBuilder.injector
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import utils.BaseTest
import controllers.EligibilityCalculatorController

class CalculatorResultTest extends BaseTest {

  val applicationConfig: ApplicationConfig = injector().instanceOf[ApplicationConfig]
  lazy val controller: EligibilityCalculatorController = app.injector.instanceOf[EligibilityCalculatorController]

  val recipientAllowance: Int = applicationConfig.RECIPIENT_ALLOWANCE

  "Check eligibility benefit" should {

    "display potential MAX BENEFIT content: transferor has no income & recipient income over recipient allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "0",
        "recipient-income" -> (applicationConfig.RECIPIENT_ALLOWANCE + 1).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"Based on the information you have given us, as a couple you would benefit by around £${applicationConfig.MAX_BENEFIT()} a year."
    }

    "display potential MAX BENEFIT content: transferor income is under transferor allowance & recipient income much over recipient allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE - 1).toString,
        "recipient-income" -> (applicationConfig.RECIPIENT_ALLOWANCE + 15000).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"Based on the information you have given us, as a couple you would benefit by around £${applicationConfig.MAX_BENEFIT()} a year."
    }

    "display potential PART BENEFIT content: recipient income in between personal allowance & recipient allowance" in {
      val underRecipientAllowance = applicationConfig.RECIPIENT_ALLOWANCE - 1000
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "0",
        "recipient-income" -> underRecipientAllowance.toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"Based on the information you have given us, as a couple you would benefit by around £${applicationConfig.MAX_BENEFIT() - 200} a year."
    }

    "display TRANSFEROR NOT ELIGIBLE content if transferor income is more than maximum limit RECIPIENT higher income" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 1)
      val aboveTransferor = formatter.format(applicationConfig.MAX_LIMIT() + 100)
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

    "display NO BENEFIT content: transferor income same as transferor allowance & recipient earns £1 over personal allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> applicationConfig.TRANSFEROR_ALLOWANCE.toString,
        "recipient-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 1).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You will not benefit as a couple."
    }

    "display NO BENEFIT content: transferor £1 over transferor allowance & recipient earns £1 over personal allowance" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE + 1).toString,
        "recipient-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 1).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple."
    }

    "display NO BENEFIT AS COUPLE content: transferor is over personal allowance" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE())
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 1).toString,
        "recipient-income" -> (applicationConfig.PERSONAL_ALLOWANCE() + 10000).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple because your income is over £$lowerThreshold."
    }

    "show RECIPIENT NOT ELIGIBLE content: for recipient income below personal allowance" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> (applicationConfig.TRANSFEROR_ALLOWANCE + 1).toString,
        "recipient-income" -> (applicationConfig.PERSONAL_ALLOWANCE() - 1).toString
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "show RECIPIENT NOT ELIGIBLE: recipient is taxed at higher rate" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val higherThreshold = formatter.format(applicationConfig.MAX_LIMIT())
      val overThreshold = formatter.format(applicationConfig.MAX_LIMIT() + 200)
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> applicationConfig.TRANSFEROR_ALLOWANCE.toString,
        "recipient-income" -> overThreshold
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "display CHECK INCOMES content: transferor income is above recipients income" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "country" -> "england",
        "transferor-income" -> "100",
        "recipient-income" -> "1"
      )
      val result = controller.gdsCalculatorAction()(request)
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe
        "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
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
        "transferor-income" -> "5000"
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
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("country" -> "england",
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

  }

}
