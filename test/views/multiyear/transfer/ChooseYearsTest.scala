/*
 * Copyright 2025 HM Revenue & Customs
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

import config.ApplicationConfig
import forms.ChooseYearForm
import models.auth.AuthenticatedUserRequest
import models.{Gender, RegistrationFormInput}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import views.helpers.LanguageUtilsImpl
import views.html.multiyear.transfer.choose_eligible_years

import java.time.LocalDate

class ChooseYearsTest extends BaseTest with NinoGenerator {

  val mockAppConfig: ApplicationConfig = mock[ApplicationConfig]
  lazy val nino: String = generateNino().nino

  val form: Form[Seq[String]] = new ChooseYearForm().apply()
  lazy val registrationForm: RegistrationFormInput = RegistrationFormInput("firstName", "lastName", Gender("M"), Nino(nino), currentTaxYearDate)
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  override implicit lazy val messages: Messages = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  val languageUtilsImpl: LanguageUtilsImpl = instanceOf[LanguageUtilsImpl]
  lazy val chooseYearsView: choose_eligible_years = instanceOf[choose_eligible_years]

  val currentTaxYear: TaxYear = TaxYear.current
  val currentTaxYearDate: LocalDate = currentTaxYear.starts

  implicit val doc: Document = Jsoup.parse(chooseYearsView(form, registrationForm.name,registrationForm.dateOfMarriage, currentTaxYearDate).toString())

  val dateOfMarriageWithNBSP: String = languageUtilsImpl.apply().ukDateTransformer(registrationForm.dateOfMarriage)
  val dateOfMarriage: String = dateOfMarriageWithNBSP.replace("\u00A0", " ")

  val currentTaxYearWithNBSP: String = languageUtilsImpl.apply().ukDateTransformer(currentTaxYearDate)
  val currentTaxYearStartDate: String = dateOfMarriageWithNBSP.replace("\u00A0", " ")

  "chooseYears" should {
    "display correct title" in {
      doc.title() shouldBe "Choose the years you want to apply for - Marriage Allowance application - GOV.UK"
    }

    "display correct headings" in {
      doc.getElementsByTag("h1").text() shouldBe "Choose the years you want to apply for"
    }

    "display correct text" in {
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "display correct checkbox items" in {
      doc.getElementsByClass("govuk-checkboxes__item").eachText().toArray shouldBe Array(
        s"Current tax year onwards, from $currentTaxYearStartDate",
        s"Previous tax years, on or before $currentTaxYearStartDate")

    }

    "display continue button" in {
      doc.getElementsByClass("govuk-button").text() shouldBe "Continue"

    }
  }

}
