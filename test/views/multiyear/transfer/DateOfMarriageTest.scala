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

import controllers.actions.{AuthRetrievals, UnauthenticatedActionTransformer}
import controllers.auth.PertaxAuthAction
import controllers.transfer.DateOfMarriageController
import helpers.FakePertaxAuthAction
import models.auth.AuthenticatedUserRequest
import org.apache.pekko.util.Timeout
import org.jsoup.Jsoup
import play.api.Application
import play.api.http.Status.BAD_REQUEST
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import services.TransferService
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, MockAuthenticatedAction, MockUnauthenticatedAction, NinoGenerator}

import java.time.LocalDate
import scala.concurrent.duration._
import scala.language.postfixOps


class DateOfMarriageTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  val dateOfMarriageController: DateOfMarriageController = app.injector.instanceOf[DateOfMarriageController]

  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    )
    .build()

  "Calling Date Of Marriage page with error in dom field" should {

    "display form error message (date of marriage is before 1900)" in {
      val localDate = LocalDate.now()
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "01",
        "dateOfMarriage.month" -> "01",
        "dateOfMarriage.year" -> "1899"
      )
      val result = dateOfMarriageController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/date-of-marriage")

      val form = document.getElementById("date-of-marriage-form")
      form.toString should include("/marriage-allowance-application/date-of-marriage")

      val field = form.getElementById("dateOfMarriageFieldset")
      field.text should include("When did you marry or form a civil partnership with your partner?")

      val error = field.getElementsByClass("govuk-error-message")
      error.size() shouldBe 1
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("dateOfMarriage-error").text shouldBe s"Error: The year must be a number between 1900 and ${localDate.getYear}"
      document.getElementsByClass("govuk-back-link").attr("href") shouldBe controllers.transfer.routes.PartnersDetailsController.transfer().url
    }

    "display form error message (date of marriage is after today’s date)" in {
      val localDate = LocalDate.now().plusYears(1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "01",
        "dateOfMarriage.month" -> "01",
        "dateOfMarriage.year" -> s"${localDate.getYear}"
      )
      val result = dateOfMarriageController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/date-of-marriage")

      val form = document.getElementById("date-of-marriage-form")
      form.toString should include("/marriage-allowance-application/date-of-marriage")
      form.getElementById("dateOfMarriageFieldset").text should include("When did you marry or form a civil partnership with your partner?")

      val error = form.getElementsByClass("govuk-error-message")
      error.size() shouldBe 1
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("dateOfMarriage-error").text shouldBe s"Error: The date of marriage or civil partnership must be today or in the past"
    }

    "display form error message (date of marriage is left empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "",
        "dateOfMarriage.month" -> "",
        "dateOfMarriage.year" -> ""
      )
      val result = dateOfMarriageController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/date-of-marriage")

      val form = document.getElementById("date-of-marriage-form")
      form.toString should include("/marriage-allowance-application/date-of-marriage")

      val field = form.getElementById("dateOfMarriageFieldset")
      field.text should include("When did you marry or form a civil partnership with your partner?")

      val error = field.getElementsByClass("govuk-error-message")
      val labelName = form.select("fieldset[id=dateOfMarriageFieldset]").first()
      error.size() shouldBe 1
      labelName
        .getElementsByClass("govuk-error-message")
        .first()
        .text() shouldBe "Error: Enter the date of your marriage or civil partnership"
      document
        .getElementById("dateOfMarriage-error")
        .text() shouldBe "Error: Enter the date of your marriage or civil partnership"
    }
  }
}
