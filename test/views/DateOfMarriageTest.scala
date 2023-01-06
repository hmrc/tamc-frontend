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

package views

import forms.DateOfMarriageForm
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import views.html.date_of_marriage

import java.time.LocalDate

class DateOfMarriageTest extends BaseTest {

  lazy val dateOfMarriage = instanceOf[date_of_marriage]
  lazy val dateOfMarriageForm = instanceOf[DateOfMarriageForm]

  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))


  "dateOfMarriage" should {
    "return the correct title" in {

      val document = Jsoup.parse(dateOfMarriage(dateOfMarriageForm.dateOfMarriageForm(LocalDate.now)).toString)
      val title = document.title()
      val expected = messages("title.application.pattern", messages("title.date-of-marriage"))

      title shouldBe expected
    }

    "return the correct h1" in {

      val document = Jsoup.parse(dateOfMarriage(dateOfMarriageForm.dateOfMarriageForm(LocalDate.now)).toString)
      val h1Tag = document.getElementsByTag("h1").toString
      println(h1Tag + "H1 TAG IS HERERERERERERRRERRERERERERERERERERER")
      val expected = messages("pages.date-of-marriage.heading")

      h1Tag should include(expected)
    }

    "return claim from when Marriage Allowance was first introduced content" in {

      val document = Jsoup.parse(dateOfMarriage(dateOfMarriageForm.dateOfMarriageForm(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.date-of-marriage.para1")

      paragraphTag should include(expected)
    }

    "return You can claim for up to 4 previous years content" in {

      val document = Jsoup.parse(dateOfMarriage(dateOfMarriageForm.dateOfMarriageForm(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.date-of-marriage.para2")

      paragraphTag should include(expected)
    }


    "return When did you marry or form a civil partnership h2" in {

      val document = Jsoup.parse(dateOfMarriage(dateOfMarriageForm.dateOfMarriageForm(LocalDate.now)).toString)
      val paragraphTag = document.getElementsByTag("form").toString //can this be more specific
      val expected = messages("pages.date-of-marriage.h2")

      paragraphTag should include(expected)
    }
  }


}
