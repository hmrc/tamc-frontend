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

//import models.{CitizenName, ConfirmationModel, TaxYear}
//import org.jsoup.Jsoup
//import uk.gov.hmrc.domain.Nino
//import uk.gov.hmrc.emailaddress.EmailAddress
//import utils.BaseTest
//import views.html.confirm
//import forms.DateOfMarriageForm
//import models.auth.AuthenticatedUserRequest
//import play.api.test.FakeRequest
//
//import java.time.LocalDate

//class ConfirmTest extends BaseTest {
//
//  lazy val confirm = instanceOf[confirm]
//  lazy val confirmationModel = ConfirmationModel
//  lazy val citizenName = CitizenName(Some("firstName"), Some("lastName"))
//  lazy val emailAddress = EmailAddress("test@test.com")
//  lazy val nino = Nino("AA000000A")
//  lazy val listOfTaxYears = List(TaxYear(2020))
//  lazy val dateOfMarriageForm = instanceOf[DateOfMarriageForm]
//  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))
//
//  "Confirm" should {
//    "return the correct title" in {
//
//      val document = Jsoup.parse(confirm(confirmationModel(Some(citizenName), emailAddress, "firstName","lastName", nino, listOfTaxYears, dateOfMarriageForm.dateOfMarriageForm(LocalDate.now))))
//      val title = document.title()
//      val expected = messages("title.application.pattern", messages("title.confirm"))
//
//      title shouldBe expected
//    }
//
//    "display lower income content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("p").toString
//      val expected = messages("pages.form.details")
//
//      paragraphTag should include(expected)
//    }
//
//    "display Your details (low income) h2" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val h2Tag = document.getElementsByTag("h2").toString
//      val expected = messages("pages.confirm.lower.earner")
//
//      h2Tag should include(expected)
//    }
//
//    "display Your partner’s details (high income) h2" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val h2Tag = document.getElementsByTag("h2").toString
//      val expected = messages("pages.confirm.higher.earner")
//
//      h2Tag should include(expected)
//    }
//
//    "display Your Marriage Allowance details h2" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val h2Tag = document.getElementsByTag("h2").toString
//      val expected = messages("pages.confirm.marriage.details")
//
//      h2Tag should include(expected)
//    }
//
//    "display Date of marriage or civil partnership content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("p").toString
//      val expected = messages("pages.confirm.date.of.marriage")
//
//      paragraphTag should include(expected)
//    }
//
//    "display HMRC will change your tax code content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("dd").toString
//      val expected = messages("pages.confirm.current.tax.desc")
//
//      paragraphTag should include(expected)
//    }
//
//    "display HMRC will check the details you have supplied content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("dd").toString
//      val expected = messages("pages.confirm.previous.tax.desc")
//
//      paragraphTag should include(expected)
//    }
//
//    "display Check the details you have entered content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("p").toString
//      val expected = messages("pages.confirm.warning")
//
//      paragraphTag should include(expected)
//    }
//
//    "display Check the details you have entered content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("dt").toString
//      val expected = messages("pages.confirm.current.tax")
//
//      paragraphTag should include(expected)
//    }
//
//    "display Previous tax year content" in {
//
//      val document = Jsoup.parse(confirm.toString)
//      val paragraphTag = document.getElementsByTag("dt").toString
//      val expected = messages("pages.confirm.previous.tax")
//
//      paragraphTag should include(expected)
//    }
//
//
//  }
//
//}
