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
import models.{Gender, RegistrationFormInput, TaxYear}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.multiyear.transfer.single_year_select

import java.time.LocalDate

class SingleYearSelectTest extends BaseTest with NinoGenerator {

  lazy val nino = generateNino().nino
  lazy val singleYearSelect = instanceOf[single_year_select]
  lazy val earlyYearForm = EarlierYearForm.earlierYearsForm()
  lazy val registrationForm = RegistrationFormInput("firstName", "lastName", Gender("M"), Nino(nino), LocalDate.now)
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))

  "SingleYearSelect" should {
    "return the correct title" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val title = document.title()
      val expected = messages("title.confirm-extra-years") + " - " + messages("title.application.pattern")

      title shouldBe expected
    }


    "return TaxYear content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val h2Tag = document.getElementsByTag("h2").toString
      val expected = messages("pages.multiyear.taxyear")

      h2Tag should include(expected)

    }

    "return you and (partner) can claim content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.multiyear.canclaim")

      paragraphTag should include(expected)

    }

    "return your income was £x or less content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("ul").toString
      val expected = messages("your-income")

      paragraphTag should include(expected)

    }

    "return x income was between content" in {

      val document = Jsoup.parse(singleYearSelect(earlyYearForm, registrationForm, List(TaxYear(2022))).toString())
      val paragraphTag = document.getElementsByTag("ul").toString
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
      val paragraphTag = document.getElementsByTag("div").toString
      val expected = messages("pages.multiyear.extrayears.from.to")

      paragraphTag should include(expected)

    }

  }

}
