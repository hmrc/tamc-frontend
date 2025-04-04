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
import controllers.transfer.TransferAllowanceController
import forms.RecipientDetailsForm
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
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, MockAuthenticatedAction, MockUnauthenticatedAction, NinoGenerator}
import views.html.multiyear.transfer.transfer

import java.time.LocalDate
import scala.concurrent.duration._
import scala.language.postfixOps


class TransferAllowanceTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  lazy val transferView: transfer = instanceOf[transfer]
  lazy val transferForm: RecipientDetailsForm = instanceOf[RecipientDetailsForm]
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  val transferAllowanceController: TransferAllowanceController = app.injector.instanceOf[TransferAllowanceController]

  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    )
    .build()


  "Transfer page" should {
    "display the correct page title of transfer page" in {

      val document = Jsoup.parse(transferView(transferForm.recipientDetailsForm(LocalDate.now, Nino(nino))).toString())
      val title = document.title()
      val expected = messages("title.transfer") + " - " + messages("title.application.pattern")

      title shouldBe expected
    }

    "display lower income content" in {

      val document = Jsoup.parse(transferView(transferForm.recipientDetailsForm(LocalDate.now, Nino(nino))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.form.details")

      paragraphTag should include(expected)

    }
  }

  "Calling Transfer Submit page" should {

    "display form error message (first name and last name missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "gender" -> "M",
        "nino" -> Ninos.nino1,
        "transferor-email" -> "example@example.com"
      )

      val result = transferAllowanceController.transferAction()(request)
      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (request body missing form data)" in {
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text() shouldBe "Error: Enter your partner’s National Insurance number"
    }
  }

  "Calling Transfer Submit page with error in name field" should {
    "display form error message (first name missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (first name is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (first name is blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> " ",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (first name contains more than 35 characters)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "a" * 36,
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Your partner’s first name must be 35 characters or less"
    }

    "display form error message (first name contains numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "12345",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Your partner’s first name must only include letters a to z and hyphens"
    }

    "display form error message (first name contains letters and numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "abc123",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Your partner’s first name must only include letters a to z and hyphens"
    }

    "display form error message (last name missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Enter your partner’s last name"
    }

    "display form error message (last name is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Enter your partner’s last name"
    }

    "display form error message (last name is blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> " ",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Enter your partner’s last name"
    }

    "display form error message (last name contains more than 35 characters)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "a" * 36,
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Your partner’s last name must be 35 characters or less"
    }

    "display form error message (last name contains numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "12345",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Your partner’s last name must only include letters a to z and hyphens"
    }

    "display form error message (last name contains letters and numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "abc123",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Your partner’s last name must only include letters a to z and hyphens"

    }

    "display form error message when recipient nino equals transferor nino" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "abc",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text() shouldBe "Error: You cannot enter your own details"
    }
  }

  "Calling Transfer Submit page with error in gender field" should {
    "display form error message (gender missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("gender-error").text() shouldBe "Error: Select your partner’s gender"
    }

    "display form error message (gender code is invalid)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "X",
        "nino" -> Ninos.nino1
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("gender-error").text() shouldBe "Error: Select your partner’s gender"
    }
  }

  "Calling Transfer Submit page with error in NINO field" should {
    "display form error message (NINO missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M"
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text shouldBe "Error: Enter your partner’s National Insurance number"
    }

    "display form error message (NINO is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> ""
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text shouldBe "Error: Enter your partner’s National Insurance number"
    }

    "display form error message (NINO is invalid)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> "ZZ"
      )
      val result = transferAllowanceController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text shouldBe "Error: Enter a real National Insurance number"
    }
  }
}
