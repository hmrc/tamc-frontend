/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig._
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}

class PtaEligibilityCalcControllerTest extends ControllerBaseSpec {

  private def calculatorRequestAction(income: Map[String, String] = null) = {
    val controller = app.injector.instanceOf[EligibilityController]
    income match {
      case nullMap if null == income =>
        controller.ptaCalculator()(request)

      case bothKeys if (income.contains("transferor-income") &&
        income.contains("recipient-income")) =>
        val fakeRequest = FakeRequest()
          .withFormUrlEncodedBody("country" -> "england", "transferor-income" -> income("transferor-income"),
            "recipient-income" -> income("recipient-income"))
        controller.ptaCalculatorAction()(fakeRequest)

      case transferorKey if (income.contains("transferor-income") &&
        !income.contains("recipient-income")) =>
        val request = FakeRequest().
          withFormUrlEncodedBody("country" -> "england", "transferor-income" -> income("transferor-income"))
        controller.ptaCalculatorAction()(request)

      case transferorKey if (!income.contains("transferor-income") &&
        income.contains("recipient-income")) =>
        val request = FakeRequest().
          withFormUrlEncodedBody("country" -> "england", "recipient-income" -> income("recipient-income"))
        controller.ptaCalculatorAction()(request)
    }
  }

  "Check eligibility benefit" should {

    "be GBP 80 if transferor income=9000 (< 11500) and recipient income=14000" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "14,000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("Based on the information you have given us, as a couple you would benefit by around")
    }

    "be GBP 212 if transferor income=0 (< 11500) and recipient income=14000" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "0", "recipient-income" -> "14000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("Based on the information you have given us, as a couple you would benefit by around")
    }

    "if transferor income=14000 and recipient income=20000" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val result = calculatorRequestAction(Map("transferor-income" -> "14000", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("You will not benefit as a couple")
    }

    "be GBP 230 if transferor income=10000 (9540-11000) and recipient income=20000 (11660-42385)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "10000", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() should include("Based on the information you have given us, as a couple you would benefit by around")
    }


    "show ’recipient is not eligible’ if transferor income=9540  and recipient income<11001" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(MAX_LIMIT())
      val result = calculatorRequestAction(Map("transferor-income" -> "9540", "recipient-income" -> "11000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. " +
        s"Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "show ’recipient is not eligible’ if transferor income=9540  and recipient income>45000" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(MAX_LIMIT())
      val overHigherThreshold = formatter.format(MAX_LIMIT() + 10)
      val result = calculatorRequestAction(Map("transferor-income" -> "9540", "recipient-income" -> overHigherThreshold))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. " +
        s"Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

  }

  "Check validation message" should {

    "display form error message (one error)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
    }

    "display form error message (multiple errors)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "", "recipient-income" -> ""))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
    }

    "be displayed if transferor income is not provided (None)" in {
      val result = calculatorRequestAction(Map("recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      document.getElementById("transferor-income-error").text shouldBe "Enter your income (low), before tax is taken off"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income is not provided (Empty)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      document.getElementById("transferor-income-error").text shouldBe "Enter your income (low), before tax is taken off"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Enter your income (low), before tax is taken off"
    }

    "be displayed if transferor income contains letters" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "abc", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income contains negative number" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "-1", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income exceeds max Int" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "2147483648", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Your income (low), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income is not provided (None)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      document.getElementById("recipient-income-error").text shouldBe "Enter your partner’s income (high), before tax is taken off"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income is not provided (Empty)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> ""))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      document.getElementById("recipient-income-error").text shouldBe "Enter your partner’s income (high), before tax is taken off"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Enter your partner’s income (high), before tax is taken off"
    }

    "be displayed if recipient income contains letters" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> "abc"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income contains negative number" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> "-1"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if recipient income exceeds max Int" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> "2147483648"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Your partner’s income (high), before tax is taken off must only include numbers 0 to 9"
    }

    "be displayed if transferor income=0 (< 9540) and recipient income=0 (10600-11660)" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(MAX_LIMIT())
      val result = calculatorRequestAction(Map("transferor-income" -> "0", "recipient-income" -> "0"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "be displayed if transferor income=9000 (< 9540) and recipient income=5000 (< 11000)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
    }

    "be displayed if transferor income=9000 (< 9540) and recipient income=50001" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(MAX_LIMIT())
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "50001"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You are not eligible for Marriage Allowance. Your partner’s annual income must be between £$lowerThreshold and £$higherThreshold."
    }

    "be displayed if transferor income=12600 and recipient income=20000" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE())
      val result = calculatorRequestAction(Map("transferor-income" -> "12600", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe s"You will not benefit as a couple because your income is over £$lowerThreshold."
    }
    "be displayed if transferor income=43001 (>42385) and recipient income=20000" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "43001", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Check the numbers you have entered. Please enter the lower earner’s income followed by the higher earner’s income."
    }
  }
}
