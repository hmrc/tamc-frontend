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

import utils.{BaseTest, NinoGenerator}
import views.html.multiyear.transfer.eligible_years
import forms.CurrentYearForm
import models.CurrentYearInput
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.data.Form
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

class EligibleYearsTest extends BaseTest with NinoGenerator {

  val nino: String = generateNino().nino
  val eligibleYears: eligible_years = instanceOf[eligible_years]
  val eligibleYearsForm: Form[CurrentYearInput] = CurrentYearForm.currentYearForm()
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))


  "EligibleYears" should {
    "return correct title on the lower earner page" in {

      val document = Jsoup.parse(eligibleYears(eligibleYearsForm, true, "testName", Some(LocalDate.now), Some(LocalDate.now)).toString)
      val title = document.getElementsByTag("main").toString
      val expected = messages("title.eligible-years")

      title should include(expected)
    }

    "display 'told us you married or formed a civil partnership' content" in {

      val document = Jsoup.parse(eligibleYears(eligibleYearsForm, true, "testName", Some(LocalDate.now), Some(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.eligibleyear.toldus")

      paragraphTag should include(expected)

    }

    "display 'This current tax year' content" in {

      val document = Jsoup.parse(eligibleYears(eligibleYearsForm, true, "testName", Some(LocalDate.now), Some(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("h2").toString
      val expectedThisYearOne = messages("pages.eligibleyear.thisyear1")
      val expectedThisYearTwo = messages("pages.eligibleyear.thisyear2", LocalDate.now.toString)

      paragraphTag should include(expectedThisYearOne)
      paragraphTag should include(expectedThisYearTwo)

    }

    "display 'automatically renew every year' content" in {

      val document = Jsoup.parse(eligibleYears(eligibleYearsForm, true, "testName", Some(LocalDate.now), Some(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("Html").toString
      val expected = messages("pages.eligibleyear.li3")

      paragraphTag should include(expected)

    }

    "display will pay up to £ less tax each year content" in {

      val document = Jsoup.parse(eligibleYears(eligibleYearsForm, true, "testName", Some(LocalDate.now), Some(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("Html").toString
      val expectedpayLessTax = messages("pages.eligibleyear.li1")
      val expectedAutomaticallyRenewEachYear = messages("pages.eligibleyear.li2")

      paragraphTag should include(expectedpayLessTax)
      paragraphTag should include(expectedAutomaticallyRenewEachYear)

    }
  }


}
