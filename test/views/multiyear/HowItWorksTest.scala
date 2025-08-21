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

  object Selectors {
    val pageTitle: String = "#pageHeading"
    val startNow: String = "#start-now"
    def nthParagraph(n: Int): String = s"#main-content > div > div > p:nth-child($n)"
    def nthInset(n: Int): String = s"#main-content > div > div > div:nth-child($n)"
    def nthSubheading(n: Int): String = s"#main-content > div > div > h2:nth-child($n)"
    def nthListItem(n: Int): String = s"#main-content > div > div > ul > li:nth-child($n)"
    def nthInlineLink(n: Int): String = s"#main-content > div > div > p:nth-child($n) > a"
  }

  "How it works" should {

    "display correct title" in {

      val expectedDocTitle = "Apply for Marriage Allowance - Marriage Allowance - GOV.UK"
      val expectedPageTitle = "Apply for Marriage Allowance"

      checkDocTitle(expectedDocTitle)
      checkPageTitle(expectedPageTitle, Selectors.pageTitle)
    }

    "display correct lede section" in {

      val line1 = s"Marriage Allowance (opens in a new tab) lets you transfer £$maxAllowedTransfer of your Personal Allowance to your husband, wife or civil partner if your income is lower than theirs. This can reduce their tax by up to £$maxBenefit this tax year (6 April to 5 April the next year)."
      val line2 = "You can apply for:"
      doc().getElementById("claim-current-year").text().shouldBe("the current year onwards online")
      doc().getElementById("claim-previous-year").text().shouldBe("up to 4 previous years, by post, to have your allowance backdated")
      val line3 = "Marriage Allowance automatically renews at the end of each tax year. You can cancel it, but it will not be stopped until the end of the tax year."
      val line4 = "If your partner has died, you can still make a Marriage Allowance claim as long as the conditions are met. If this applies to you, call HMRC on 0300 200 3300."

      checkTextInElement(line1, Selectors.nthParagraph(2))
      checkTextInElement(line2, Selectors.nthParagraph(3))
      checkTextInElement(line3, Selectors.nthParagraph(5))
      checkTextInElement(line4, Selectors.nthInset(6))

      val marriageAllowanceLink = selectFirst(Selectors.nthInlineLink(2))
      marriageAllowanceLink.text.shouldBe("Marriage Allowance (opens in a new tab)")
      marriageAllowanceLink.attr("href") shouldBe "https://www.gov.uk/marriage-allowance"
    }

    "display correct eligibility section" in {

      val heading = "Eligibility"
      val line1 = "To be eligible for Marriage Allowance:"
      doc().getElementById("must-be-married").text().shouldBe("you must be married or in a civil partnership")
      doc().getElementById("be-lowest-income").text().shouldBe(s"your income must be lower than your partner’s and less than £$personalAllowance")
      doc().getElementById("earn-below-max-threshold").text().shouldBe(s"your partner’s income must be less than £$maxLimit in the current tax year")
      val line2 = s"If you live in Scotland, your partner must pay the starter, basic or intermediate rate, which usually means their income is between £$lowerTresholdScotland and £$maxLimitScotland."
      val headingMCA = "If you or your partner were born before 6 April 1935"

      checkTextInElement(heading, Selectors.nthSubheading(7))
      checkTextInElement(line1, Selectors.nthParagraph(8))
      checkTextInElement(line2, Selectors.nthInset(10))
      checkTextInElement(headingMCA, Selectors.nthSubheading(12))

      doc().getElementById("married-couples-allowance").text.shouldBe("If one of you was born before 6 April 1935, " +
        "you might benefit more as a couple by applying for the Married Couple’s Allowance (opens in a new tab). " +
        "You can still apply for Marriage Allowance but you cannot receive both allowances at the same time.")
    }

    "display correct calculate section" in {

      val heading = "Calculate how much you could benefit"
      val line1 = "Use the Marriage Allowance calculator to see how much you could save in the current tax year. This does not form part of the application."

      checkTextInElement(heading, Selectors.nthSubheading(15))
      checkTextInElement(line1, Selectors.nthParagraph(16))

      val calculatorLink = selectFirst(Selectors.nthInlineLink(16))
      calculatorLink.text.shouldBe("Use the Marriage Allowance calculator")
      calculatorLink.attr("href") shouldBe "/marriage-allowance-application/benefit-calculator-pta"
    }


    "display correct apply section" in {

      val heading = "Before you apply"
      val line1 = "You’ll need:"
      doc().getElementById("need-partner-national-insurance").text().shouldBe("your partner’s National Insurance number")
      doc().getElementById("need-date-of-marriage").text().shouldBe("the date of your marriage or civil partnership")

      checkTextInElement(heading, Selectors.nthSubheading(17))
      checkTextInElement(line1, Selectors.nthParagraph(18))
    }

    "display a start-now button" in {

      val startNow = selectFirst(Selectors.startNow)
      startNow.text.shouldBe("Apply now")
      startNow.attr("href").shouldBe("/marriage-allowance-application/transfer-allowance")
    }
  }
}
