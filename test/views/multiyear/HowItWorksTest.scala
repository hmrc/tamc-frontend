/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import utils.BaseTest
import views.html.multiyear.how_it_works

import java.text.NumberFormat

class HowItWorksTest extends BaseTest {

  val applicationConfig : ApplicationConfig = instanceOf[ApplicationConfig]
  implicit val request: UserRequest[_] = UserRequest(FakeRequest(), None, true, None, false)
  override implicit lazy val messages = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val howItWorksView = instanceOf[how_it_works]

  lazy val maxBenefit = NumberFormat.getNumberInstance().format(applicationConfig.MAX_BENEFIT())
  lazy val maxPersonalAllowanceTransferable = NumberFormat.getNumberInstance().format(applicationConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER())
  lazy val currentYearPersonalAllowance = NumberFormat.getNumberInstance().format(applicationConfig.PERSONAL_ALLOWANCE())
  lazy val earliestClaimYear = (applicationConfig.actualTaxYear(0) -4).toString

  "How it works" should {

    "return correct page title of how it works page" in {

      val document = Jsoup.parse(howItWorksView().toString())
      val title = document.title()
      val expected = messages("title.pattern", messages("title.how-it-works"))

      title shouldBe expected
    }

    "display the current year maximum amount of personal allowance transferable" in {

      val document = Jsoup.parse(howItWorksView().toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.how-it-works.lede-pre1.part2", maxPersonalAllowanceTransferable)

      paragraphTag should include(expected)
    }

    "display current years maximum amount of tax reduction" in {

      val document = Jsoup.parse(howItWorksView().toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.how-it-works.lede-pre2", maxBenefit)

      paragraphTag should include(expected)
    }

    "display correct eligibility to transferor, their income should be below partner and currentYear personal allowance" in {

      val document = Jsoup.parse(howItWorksView().toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.how-it-works.lede-pre4", currentYearPersonalAllowance)

      paragraphTag should include(expected)
    }

    "display the correct year a claim can be backdated from: Current Year & 4 Previous years" in {

      val document = Jsoup.parse(howItWorksView().toString())
      val paragraphTag = document.getElementsByClass("govuk-inset-text").toString
      val expected = messages("pages.how-it-works.lede-pre5", earliestClaimYear)

      paragraphTag should include(expected)

    }
  }
}
