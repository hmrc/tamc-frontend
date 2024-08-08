/*
 * Copyright 2024 HM Revenue & Customs
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

import models.auth.AuthenticatedUserRequest
import models.{Gender, RegistrationFormInput, TaxYear}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.multiyear.transfer.previous_years

import java.time.LocalDate

class PreviousYearsContentTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  lazy val previousYears = instanceOf[previous_years]
  lazy val registrationForm = RegistrationFormInput("firstName", "lastName", Gender("M"), Nino(nino), LocalDate.now)
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))

  "previousYears" should {
    "return the correct title" in {

      val document = Jsoup.parse(previousYears(registrationForm,
        List(TaxYear(2022)),
        true).toString())

      val title = document.title()
      val expected = messages("pages.previousyear.header") + " - " + messages("title.application.pattern")

      title shouldBe expected
    }

    "display 'automatically renew every year' content" in {

      val document = Jsoup.parse(previousYears(registrationForm,
        List(TaxYear(2022)),
        true).toString())

      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.previousyear.para")

      paragraphTag should include(expected)

    }

    "display You can apply for earlier tax years h1" in {

      val document = Jsoup.parse(previousYears(registrationForm,
        List(TaxYear(2022)),
        true).toString())

      val paragraphTag = document.getElementsByTag("h1").toString
      val expected = messages("pages.previousyear.header")

      paragraphTag should include(expected)

    }
  }

}
