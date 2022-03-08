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
import forms.MultiYearPartnersIncomeQuestionForm
import models.auth.UserRequest
import org.jsoup.Jsoup
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import utils.BaseTest
import views.html.multiyear.partners_income_question
import java.text.NumberFormat

class PartnersIncomeQuestionTest extends BaseTest {

  val applicationConfig : ApplicationConfig = instanceOf[ApplicationConfig]
  implicit val request: UserRequest[_] = UserRequest(FakeRequest(), None, true, None, false)
  lazy val partnersIncomeFormInputForm = instanceOf[MultiYearPartnersIncomeQuestionForm].partnersIncomeForm
  lazy val partnersIncomeQuestionView = instanceOf[partners_income_question]
  override implicit lazy val messages = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])

  lazy val view = partnersIncomeQuestionView(partnersIncomeFormInputForm, false)
  lazy val scottishResidentView = partnersIncomeQuestionView(partnersIncomeFormInputForm, true)
  lazy val personalAllowance = NumberFormat.getNumberInstance().format(applicationConfig.PERSONAL_ALLOWANCE() + 1)
  lazy val maxLimit = NumberFormat.getNumberInstance().format(applicationConfig.MAX_LIMIT())
  lazy val maxLimitScottish = NumberFormat.getNumberInstance().format(applicationConfig.MAX_LIMIT_SCOT())

  "Partners Income Question Test" should {

    "return correct title for partners income question page" in {

      val document = Jsoup.parse(view.toString())
      val title = document.title()
      val expected = messages("title.eligibility.pattern", messages("eligibility.check.partners.income.h1", personalAllowance, maxLimit))

      title should include(expected)
    }

    "return the correct Scottish resident income thresholds for partners income question page" in {

      val document = Jsoup.parse(scottishResidentView.toString())
      val title = document.title()
      val expected = messages("title.eligibility.pattern", messages("eligibility.check.partners.income.h1", personalAllowance, maxLimitScottish))

      title should include(expected)

    }
  }
}
