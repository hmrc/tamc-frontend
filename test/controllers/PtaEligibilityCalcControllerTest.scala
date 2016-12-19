/*
 * Copyright 2016 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout}
import test_utils.TestUtility
import uk.gov.hmrc.play.test.UnitSpec

class PtaEligibilityCalcControllerTest extends UnitSpec with TestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

  private def calculatorRequestAction(income: Map[String, String] = null) = {
    val testComponent = makePtaEligibilityTestComponent("user_happy_path")
    val request = income match {
      case nullMap if (null == income) =>
        val request = testComponent.request
        val controllerToTest = testComponent.controller
        controllerToTest.calculator()(request)

      case bothKeys if (income.contains("transferor-income") &&
        income.contains("recipient-income")) =>
        val request = testComponent.request.
          withFormUrlEncodedBody("transferor-income" -> income.get("transferor-income").get,
            "recipient-income" -> income.get("recipient-income").get)
        val controllerToTest = testComponent.controller
        controllerToTest.calculatorAction()(request)

      case transferorKey if (income.contains("transferor-income") &&
        !income.contains("recipient-income")) =>
        val request = testComponent.request.
          withFormUrlEncodedBody("transferor-income" -> income.get("transferor-income").get)
        val controllerToTest = testComponent.controller
        controllerToTest.calculatorAction()(request)

      case transferorKey if (!income.contains("transferor-income") &&
        income.contains("recipient-income")) =>
        val request = testComponent.request.
          withFormUrlEncodedBody("recipient-income" -> income.get("recipient-income").get)
        val controllerToTest = testComponent.controller
        controllerToTest.calculatorAction()(request)
    }

    request
  }

  "Hitting calculator page" should {
    "return OK (200) response" in {
      val result = calculatorRequestAction()
      status(result) shouldBe OK
    }

    "have form" in {
      val result = calculatorRequestAction()
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator") shouldNot be(null)
    }

    "have form calculate button" in {
      val result = calculatorRequestAction()
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("button").attr("type") shouldEqual "submit"
    }

    "have required fields" in {
      val result = calculatorRequestAction()
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.select("input[name=transferor-income]").first() shouldNot be(null)
      form.select("input[name=recipient-income]").first() shouldNot be(null)
    }

    "not have calculation result" in {
      val result = calculatorRequestAction()
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result") shouldBe null
    }
  }

  "Check eligibility benefit" should {

    "be GBP 80 if transferor income=9000 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 212 if transferor income=0 (< 9540) and recipient income=12060 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "0", "recipient-income" -> "12060"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £212 a year."
    }

    "be GBP 200 if transferor income=9000 (< 9540) and recipient income=12000 (> 11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "12000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £200 a year."
    }

    "be GBP 130 if transferor income=9580 (9540-11000) and recipient income=11650 (11000-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9580", "recipient-income" -> "11650"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £130 a year."
    }

    "be negative (12) if transferor income=11000 (9540-11000) and recipient income=12000 (11000-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "11000", "recipient-income" -> "12000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the details you have provided you will not benefit from making the transfer."
    }

    "be GBP 200 if transferor income=10000 (9540-11000) and recipient income=20000 (11660-42385)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "10000", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £200 a year."
    }


    "show 'recipient is not eligible' if transferor income=9540  and recipient income<11001" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9540", "recipient-income" -> "11000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You are not eligible for Marriage Allowance. Your spouse or civil partner's annual income must be between £11,001 and £43,000."
    }

    "show 'recipient is not eligible' if transferor income=9540  and recipient income>43000" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9540", "recipient-income" -> "43001"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You are not eligible for Marriage Allowance. Your spouse or civil partner's annual income must be between £11,001 and £43,000."
    }

  }

  "When transferor income is in different format, calculation" should {

    "be GBP 80 if transferor income=9000.05 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000.05", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=9,000.05 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9,000.05", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=£9,000.05 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "£9,000.05", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=£9 000.05 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "£9 000,05", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=£9.000.05 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "£9.000,05", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }
  }

  "When recipient income is in different format, calculation" should {

    "be GBP 80 if transferor income=9000 (< 9540) and recipient income=11400.05 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "£9.000,05", "recipient-income" -> "11400"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=9000 (< 9540) and recipient income=11400 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "11,400.05"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=9000 (< 9540) and recipient income=£11,400.05 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "£11,400.05"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }

    "be GBP 80 if transferor income=9000 (< 9540) and recipient income=£11.400,05 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "£11.400,05"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Based on the information you have given us, as a couple you would benefit by around £80 a year."
    }
  }

  "Check validation message" should {

    "display form error message (one error)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("form-error-message").text() shouldBe "Check your information is correct, in the right place and in the right format."
    }

    "display form error message (multiple errors)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "", "recipient-income" -> ""))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("form-error-message").text() shouldBe "Check your information is correct, in the right place and in the right format."
    }

    "be displayed if transferor income is not provided (None)" in {
      val result = calculatorRequestAction(Map("recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      document.getElementById("transferor-income-error").text shouldBe "Confirm your annual income"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Tell us your annual income."
    }

    "be displayed if transferor income is not provided (Empty)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      document.getElementById("transferor-income-error").text shouldBe "Confirm your annual income"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Tell us your annual income."
    }

    "be displayed if transferor income contains letters" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "abc", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Use numbers only."
    }

    "be displayed if transferor income contains negative number" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "-1", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Use numbers only."
    }

    "be displayed if transferor income exceeds max Int" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "2147483648", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=transferor-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Use numbers only."
    }

    "be displayed if recipient income is not provided (None)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      document.getElementById("recipient-income-error").text shouldBe "Confirm your spouse or civil partner's annual income"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's annual income."
    }

    "be displayed if recipient income is not provided (Empty)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> ""))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      document.getElementById("recipient-income-error").text shouldBe "Confirm your spouse or civil partner's annual income"
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's annual income."
    }

    "be displayed if recipient income contains letters" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> "abc"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Use numbers only."
    }

    "be displayed if recipient income contains negative number" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> "-1"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Use numbers only."
    }

    "be displayed if recipient income exceeds max Int" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "5000", "recipient-income" -> "2147483648"))
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("calculator")
      form.getElementsByClass("error-message").first() shouldNot be(null)
      val yourIncome = form.select("label[for=recipient-income]").first()
      yourIncome.getElementsByClass("error-message").first().text() shouldBe "Use numbers only."
    }

    "be displayed if transferor income=0 (< 9540) and recipient income=0 (10600-11660)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "0", "recipient-income" -> "0"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You are not eligible for Marriage Allowance. Your spouse or civil partner's annual income must be between £11,001 and £43,000."
    }

    "be displayed if transferor income=9000 (< 9540) and recipient income=5000 (< 11000)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "5000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Check the numbers you've entered. Please enter the lower earner's income followed by the higher earner's income."
    }

    "be displayed if transferor income=9000 (< 9540) and recipient income=45000 (> 43000)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "9000", "recipient-income" -> "45000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You are not eligible for Marriage Allowance. Your spouse or civil partner's annual income must be between £11,001 and £43,000."
    }

    "be displayed if transferor income=10000 (9540-10600) and recipient income=44000 (> 43000)" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "10000", "recipient-income" -> "44000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You are not eligible for Marriage Allowance. Your spouse or civil partner's annual income must be between £11,001 and £43,000."
    }
    "be displayed if transferor income=11001 (>11000) and recipient income=20000" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "11001", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "You are eligible for Marriage Allowance, but you are unlikely to benefit as a couple because your income is over £11,000."
    }
    "be displayed if transferor income=43001 (>42385) and recipient income=20000" in {
      val result = calculatorRequestAction(Map("transferor-income" -> "43001", "recipient-income" -> "20000"))
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("calculator-result").text() shouldBe "Check the numbers you've entered. Please enter the lower earner's income followed by the higher earner's income."
    }
  }
}
