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

package views.multiyear

import akka.util.Timeout
import config.ApplicationConfig
import controllers.EligibilityController
import models.auth.UserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import utils.BaseTest
import forms.MultiYearEligibilityCheckForm
import play.api.Play.materializer
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.test.Helpers.baseApplicationBuilder.injector
import play.api.test.Helpers.contentAsString
import services.{CachingService, TimeService}
import views.html.multiyear.eligibility_check

import java.text.NumberFormat
import scala.concurrent.duration._
import scala.language.postfixOps

class EligibilityCheckTest extends BaseTest {

  lazy val eligibilityCheck = instanceOf[eligibility_check]
  implicit val request: UserRequest[_] = UserRequest(FakeRequest(), None, true, None, false)
  lazy val eligibilityCheckForm = MultiYearEligibilityCheckForm.eligibilityForm
  def eligibilityController: EligibilityController = instanceOf[EligibilityController]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]
  val applicationConfig: ApplicationConfig = injector().instanceOf[ApplicationConfig]

  implicit val duration: Timeout = 20 seconds

  private val lowerEarnerHelpText =
    "This is your total earnings from all employment, pensions, benefits, trusts, " +
      "rental income, including dividend income above your Dividend Allowance – before any tax and National " +
      "Insurance is taken off."

  "Eligibility Check page" should {
    "return correct page title of how it works page" in {

      val document = Jsoup.parse(eligibilityCheck(eligibilityCheckForm).toString())
      val title = document.title()
      val expected = messages("title.eligibility.pattern", messages("eligibility.check.h1"))

      title shouldBe expected

    }
    "display you can claim marriage allowance if partner has died content" in {

      val document = Jsoup.parse(eligibilityCheck(eligibilityCheckForm).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("eligibility.check.married.error2")

      paragraphTag should include(expected)

    }
    "display you are not eligible for married allowance content" in {

      val document = Jsoup.parse(eligibilityCheck(eligibilityCheckForm).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("eligibility.check.married.error1")

      paragraphTag should include(expected)

    }
  }

  "PTA Benefit calculator page " should {

    "successfully load the calculator page " in {
      val result = eligibilityController.ptaCalculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Eligibility Criteria - Marriage Allowance - GOV.UK"

      val heading = document.getElementById("pageHeading").text
      heading shouldBe "Marriage Allowance calculator"
    }
  }

  "PTA Eligibility check page for multiyear" should {

    "successfully authenticate the user and have eligibility-check page action" in {
      val result = eligibilityController.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/eligibility-check")
      document.getElementById("pageHeading").text() shouldBe ("Are you married or in a civil partnership?")
    }

    "diplay errors as none of the radio buttons are selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("marriage-criteria-error").text() shouldBe "Error: Select yes if you are married or in a civil partnership"

      val form = document.getElementById("eligibility-form")
      form.select("fieldset[id=marriage-criteria]").first()
        .getElementsByClass("govuk-error-message").text() shouldBe "Error: Select yes if you are married or in a civil partnership"
    }
  }

  "GDS Eligibility check page for multiyear" should {

    "successfully authenticate the user and have eligibility-check page action" in {
      val result = eligibilityController.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/eligibility-check")
      document.getElementById("pageHeading").text() shouldBe ("Are you married or in a civil partnership?")
    }

    "diplay errors as none of the radio buttons are selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("marriage-criteria-error").text() shouldBe "Error: Select yes if you are married or in a civil partnership"

      val form = document.getElementById("eligibility-form")
      form.select("fieldset[id=marriage-criteria]").first()
        .getElementsByClass("govuk-error-message").text() shouldBe "Error: Select yes if you are married or in a civil partnership"

    }
  }

  "PTA date of birth check page for multiyear" should {

    "successfully authenticate the user and have date of birth page and content" in {
      val result = eligibilityController.dateOfBirthCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document
        .title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "PTA lower earner check page for multiyear" should {

    "successfully authenticate the user and have income-check page and content" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE())
      val result = eligibilityController.lowerEarnerCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe s"Is your income less than £$lowerThreshold a year? - Marriage Allowance eligibility - GOV.UK"

      document.getElementById("lower-earner-information").text shouldBe lowerEarnerHelpText
    }
  }

  "PTA partners income check page for multiyear" should {

    "have partners-income page and content for English resident" in {
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_LIMIT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document
        .getElementById("partner-income-text")
        .text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document
        .getElementById("pageHeading")
        .text shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year?"

    }

    "have partners-income page and content for Scottish resident" in {
      val request = FakeRequest().withMethod("POST").withSession("scottish_resident" -> "true")
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThresholdScot = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_LIMIT_SCOT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year? - Marriage Allowance eligibility - GOV.UK"
      document
        .getElementById("partner-income-text")
        .text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document
        .getElementById("pageHeading")
        .text shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year?"

    }
  }

  "PTA do you want to apply page for multiyear" should {
    "successfully authenticate the user and have do-you-want-to-apply page and content" in {
      val result = eligibilityController.doYouWantToApply()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you want to apply for Marriage Allowance? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS date of birth page for multiyear" should {

    "successfully authenticate the user and have date of birth page and content" in {
      val result = eligibilityController.dateOfBirthCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS do you live in scotland page for multiyear" should {

    "successfully authenticate the user and have do you live in scotland page and content" in {
      val result = eligibilityController.doYouLiveInScotland()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you live in Scotland? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS do you want to apply page for multiyear" should {

    "successfully authenticate the user and have do you want to apply page and content" in {
      val result = eligibilityController.doYouWantToApply()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you want to apply for Marriage Allowance? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS lower earner page for multiyear" should {

    "successfully authenticate the user and have lower earner page and content" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(applicationConfig.PERSONAL_ALLOWANCE())
      val result = eligibilityController.lowerEarnerCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe s"Is your income less than £$lowerThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("lower-earner-information").text shouldBe lowerEarnerHelpText
    }
  }

  "GDS partners income page for multiyear" should {
    "have partners-income page and content for English resident" in {
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_LIMIT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document
        .getElementById("partner-income-text")
        .text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document
        .getElementById("pageHeading")
        .text shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year?"

    }

    "have partners-income page and content for Scottish resident" in {
      val request = FakeRequest().withMethod("POST").withSession("scottish_resident" -> "true")
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
      val higherThresholdScot = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_LIMIT_SCOT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document
        .title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year? - Marriage Allowance eligibility - GOV.UK"
      document
        .getElementById("partner-income-text")
        .text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document
        .getElementById("pageHeading")
        .text shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year?"

    }
  }
}
