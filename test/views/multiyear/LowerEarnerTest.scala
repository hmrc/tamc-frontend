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
import forms.MultiYearLowerEarnerForm
import models.auth.UserRequest
import org.jsoup.Jsoup
import play.api.mvc._
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import utils.BaseTest
import views.html.multiyear.lower_earner

import java.text.NumberFormat

class LowerEarnerTest extends BaseTest {

  val applicationConfig : ApplicationConfig = instanceOf[ApplicationConfig]
  implicit val request: UserRequest[_]      = UserRequest(FakeRequest(), None, true, None, false)
  lazy val multiYearLowerEarnerForm         = instanceOf[MultiYearLowerEarnerForm].lowerEarnerForm
  lazy val lowerEarnerView                  = instanceOf[lower_earner]
  override implicit lazy val messages       = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val view                             = lowerEarnerView(multiYearLowerEarnerForm)
  lazy val personalAllowance                = NumberFormat.getNumberInstance().format(applicationConfig.PERSONAL_ALLOWANCE())


  "Lower Earner Test" should {

    "return correct title on the lower earner page" in {

      val document = Jsoup.parse(view.toString())
      val title = document.title()
      val expected = messages("title.eligibility.pattern", messages("eligibility.check.lower.earner.h1", personalAllowance))

      title should include(expected)
    }

    "show the correct lower earner, non-benefit warning if transferor's current year income is above personal allowance" in {

      val document = Jsoup.parse(view.toString())
      val paragrapghTag = document.getElementsByTag("p").toString
      val expected = messages("eligibility.check.lower.earner.error", personalAllowance)

      paragrapghTag should include(expected)
    }
  }
}
