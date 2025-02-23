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

import models.auth.AuthenticatedUserRequest
import models.{CitizenName, ConfirmationModel, DateOfMarriageFormInput, TaxYear}
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, EmailAddress, NinoGenerator}
import views.html.confirm

import java.time.LocalDate

class ConfirmTest extends BaseTest with NinoGenerator {

  lazy val confirm = instanceOf[confirm]
  lazy val confirmationModel = ConfirmationModel
  lazy val citizenName = CitizenName(Some("firstName"), Some("lastName"))
  lazy val emailAddress = EmailAddress("test@test.com")
  lazy val nino = generateNino().nino
  lazy val listOfTaxYears = List(TaxYear(2020))
  lazy val dateOfMarriageForm = DateOfMarriageFormInput(LocalDate.now())
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))

  "Confirm" should {
    "return the correct title" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val title = document.title()
      val expected = messages("title.confirm") + " - " + messages("title.application.pattern")

      title shouldBe expected
    }

    "display Your details (low income) h2" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val h2Tag = document.getElementsByTag("h2").toString
      val expected = messages("pages.confirm.lower.earner")

      h2Tag should include(expected)
    }

    "display Your partner’s details (high income) h2" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val h2Tag = document.getElementsByTag("h2").toString
      val expected = messages("pages.confirm.higher.earner")

      h2Tag should include(expected)
    }

    "display Your Marriage Allowance details h2" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val h2Tag = document.getElementsByTag("h2").toString
      val expected = messages("pages.confirm.marriage.details")

      h2Tag should include(expected)
    }

    "display HMRC will check the details you have supplied content" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val paragraphTag = document.getElementsByTag("dd").toString
      val expected = messages("pages.confirm.previous.tax.desc")

      paragraphTag should include(expected)
    }

    "display Date of marriage or civil partnership content" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val paragraphTag = document.getElementsByTag("dt").toString
      val expected = messages("pages.confirm.date.of.marriage")

      paragraphTag should include(expected)
    }

    "display Previous tax year content" in {

      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName", "lastName", Nino(nino), listOfTaxYears, dateOfMarriageForm)).toString())
      val paragraphTag = document.getElementsByTag("dt").toString
      val expected = messages("pages.confirm.previous.tax")

      paragraphTag should include(expected)
    }


  }

}
