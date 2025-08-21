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

import config.ApplicationConfig
import models.auth.AuthenticatedUserRequest
import models.{CitizenName, ConfirmationModel, DateOfMarriageFormInput, TaxYear}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, EmailAddress, NinoGenerator}
import views.html.confirm

import java.text.NumberFormat
import java.time.LocalDate

class ConfirmTest extends BaseTest with NinoGenerator {

  lazy val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]
  lazy val maxBenefit = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_BENEFIT(2020))

  lazy val confirm = instanceOf[confirm]
  lazy val confirmationModel = ConfirmationModel
  lazy val citizenName = CitizenName(Some("George"), Some("Forrest"))
  lazy val emailAddress = EmailAddress("test@test.com")
  lazy val nino = generateNino().nino
  lazy val listOfTaxYears = List(TaxYear(2020))
  lazy val dateOfMarriageForm = DateOfMarriageFormInput(LocalDate.now())
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))
  override implicit lazy val messages: Messages = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "Alex", "Smith", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())

  "Confirm" should {
    "return the correct title" in {
      val title = document.title()
      val expected = "Check your answers before sending your application - Marriage Allowance application - GOV.UK"

      title shouldBe expected
    }

    "display Your details (low income) h2" in {
      val h2Tag: String = document.getElementsByTag("h2").toString
      val expected: String = "Your details (low income)"

      h2Tag should include(expected)
    }

    "display Your partner’s details (high income) h2" in {
      val h2Tag: String = document.getElementsByTag("h2").toString
      val expected: String = "Your partner’s details (high income)"

      h2Tag should include(expected)
    }

    "display Application details h2" in {
      val h2Tag: String = document.getElementsByTag("h2").toString
      val expected: String = messages("pages.confirm.application.details")

      h2Tag should include(expected)
    }

    "display HMRC will check the details you have supplied content" in {
      val paragraph: String = document.getElementById("outcome-2020").toString
      val expected: String = s"HMRC will check the details you have supplied before sending Alex a cheque by post for up to £$maxBenefit."

      paragraph should include(expected)
    }

    "display Date of marriage or civil partnership content" in {
      val paragraphTag = document.getElementsByTag("dt").toString
      val expected = "Date of marriage or civil partnership"

      paragraphTag should include(expected)
    }

    "display Previous tax year content" in {
      val paragraphTag = document.getElementById("year-2020").text()
      val expected = "Previous tax year: 6 April 2020 to 5 April 2021"

      paragraphTag should include(expected)
    }
  }
}
