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

package views.multiyear

import config.ApplicationConfig
import models.auth.UserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import utils.{BaseTest, ViewTestUtils}
import views.html.multiyear.how_it_works

import java.text.NumberFormat

class HowItWorksTest extends BaseTest with ViewTestUtils {

  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]
  implicit val request: UserRequest[?] = UserRequest(FakeRequest(), None, true, None, false)
  override implicit lazy val messages: Messages = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val howItWorksView: how_it_works = instanceOf[how_it_works]
  implicit val doc: () => Document = () => Jsoup.parse(howItWorksView().toString())

  val maxAllowedTransfer: String = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER())
  val maxBenefit: String = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_BENEFIT())
  val personalAllowance: String = NumberFormat.getIntegerInstance().format(applicationConfig.PERSONAL_ALLOWANCE())
  val maxLimit: String = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_LIMIT())
  val lowerTresholdScotland: String = NumberFormat.getIntegerInstance().format(applicationConfig.STARTER_RATE_LOWER_TRESHOLD_SCOT())
  val maxLimitScotland: String = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_LIMIT_SCOT())

  private def shouldHaveText(id: String, expected: String): Unit =
    doc().getElementById(id).text() shouldBe expected


  "How it works" should {

    "display correct title" in {

      val expectedDocTitle = "Apply for Marriage Allowance - Marriage Allowance - GOV.UK"
      val expectedPageTitle = "Apply for Marriage Allowance"

      shouldHaveText("pageHeading", expectedPageTitle)
      doc().title().shouldBe(expectedDocTitle)
    }

    "display correct lede section" in {
      val line1 = s"Marriage Allowance lets you transfer £$maxAllowedTransfer of your Personal Allowance to your husband, wife or civil partner if your income is lower than theirs. This can reduce their tax by up to £$maxBenefit this tax year (6 April to 5 April the next year)."
      val line2 = "You can apply for:"
      val line3 = "Marriage Allowance automatically renews at the end of each tax year. You can cancel it, but it will not be stopped until the end of the tax year."
      val line4 = "If your partner has died, you can still make a Marriage Allowance claim as long as the conditions are met. If this applies to you, call HMRC on 0300 200 3300."

      shouldHaveText("claim-current-year", "the current year onwards online")
      shouldHaveText("claim-previous-year", "up to 4 previous years, by post, to have your allowance backdated")

      shouldHaveText("para-transfer", line1)
      shouldHaveText("para-apply", line2)
      shouldHaveText("para-renew", line3)
      shouldHaveText("para-partner", line4)

      shouldHaveText("marriage-allowance-link", "Marriage Allowance")
      doc().getElementById("marriage-allowance-link").attr("href") shouldBe "https://www.gov.uk/marriage-allowance"
    }


    "display correct eligibility section" in {
      val heading = "Eligibility"
      val line1 = "To be eligible for Marriage Allowance:"
      val line2 = s"If you live in Scotland, your partner must pay the starter, basic or intermediate rate, which usually means their income is between £$lowerTresholdScotland and £$maxLimitScotland."
      val headingMCA = "If you or your partner were born before 6 April 1935"

      shouldHaveText("must-be-married", "you must be married or in a civil partnership")
      shouldHaveText("be-lowest-income", s"your income must be lower than your partner’s and less than £$personalAllowance")
      shouldHaveText("earn-below-max-threshold", s"your partner’s income must be less than £$maxLimit in the current tax year")

      shouldHaveText("heading-eligibility", heading)
      shouldHaveText("para-eligible", line1)
      shouldHaveText("para-scotland", line2)
      shouldHaveText("heading-you-partner", headingMCA)

      shouldHaveText(
        "married-couples-allowance",
        "If one of you was born before 6 April 1935, " +
          "you might benefit more as a couple by applying for the Married Couple’s Allowance. " +
          "You can still apply for Marriage Allowance but you cannot receive both allowances at the same time."
      )
    }

    "display correct calculate section" in {

      val heading = "Calculate how much you could benefit"
      val line1 = "Use the Marriage Allowance calculator to see how much you could save in the current tax year. This does not form part of the application."

      shouldHaveText("calculate-heading", heading)
      shouldHaveText("para-calculate", line1)

      shouldHaveText("calculator", "Use the Marriage Allowance calculator")
      doc().getElementById("calculator").attr("href") shouldBe "/marriage-allowance-application/benefit-calculator-pta"
    }


    "display correct apply section" in {

      val heading = "Before you apply"
      val line1 = "You’ll need:"

      shouldHaveText("need-partner-national-insurance", "your partner’s National Insurance number")
      shouldHaveText("need-date-of-marriage", "the date of your marriage or civil partnership")

      shouldHaveText("heading-before-apply", heading)
      shouldHaveText("para-you-need", line1)
    }

    "display a start-now button" in {

      shouldHaveText("start-now", "Apply now")
      doc().getElementById("start-now").attr("href").shouldBe("/marriage-allowance-application/date-of-marriage")
    }
  }
}
