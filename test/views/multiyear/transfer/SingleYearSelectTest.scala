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

package views.multiyear.transfer

import forms.EarlierYearForm
import models.auth.AuthenticatedUserRequest
import models.{RegistrationFormInput, TaxYear}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import views.html.multiyear.transfer.single_year_select

class SingleYearSelectTest extends BaseTest {

  lazy val singleYearSelect = instanceOf[single_year_select]
  lazy val earlyYearForm = EarlierYearForm.earlierYearsForm()
  lazy val registrationForm = instanceOf[RegistrationFormInput]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))

  "SingleYearSelect" should {
    "return the correct title" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val title = document.title()
      val expected = messages("title.pattern", messages("technical.other-ways.h1"))

      title shouldBe expected
    }


    "return you can call hmrc to make an application content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("technical.other-ways.para1")

      paragraphTag should include(expected)

    }

    "return you and (partner) can claim content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.multiyear.canclaim")

      paragraphTag should include(expected)

    }

    "return your income was £x or less content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("your-income")

      paragraphTag should include(expected)

    }

    "return x income was between content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("income-between")

      paragraphTag should include(expected)

    }

    "return if your application is successful content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.multiyear.successful")

      paragraphTag should include(expected)

    }

    "return do you want to apply for extra tax year content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.multiyear.extrayears.from.to")

      paragraphTag should include(expected)

    }

  }

}
