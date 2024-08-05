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

import controllers.actions.{AuthRetrievals, UnauthenticatedActionTransformer}
import controllers.auth.PertaxAuthAction
import controllers.transfer.ConfirmEmailController
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

import scala.concurrent.duration._
import scala.language.postfixOps


class ConfirmEmailTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  val confirmEmailController: ConfirmEmailController = app.injector.instanceOf[ConfirmEmailController]

  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    )
    .build()


  "Calling Confirm email page with error in email field" should {
    "display form error message (transferor email missing from request)" in {
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter your email address"
      document.getElementsByClass("govuk-back-link").attr("href") shouldBe controllers.transfer.routes.EligibleYearsController.eligibleYears().url
    }

    "display form error message (transferor email is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter your email address"
    }

    "display form error message (transferor email contains only spaces)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "  ")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter your email address"
    }

    "display form error message (transferor email contains more than 100 characters)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("transferor-email" -> s"${"a" * 90}@bbbb.ccccc")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter no more than 100 characters"
    }

    "display form error message (transferor email is invalid)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "example")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }

    "display form error message (transferor email has consequent dots)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("transferor-email" -> "ex..ample@example.com")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }

    "display form error message (transferor email has symbols). Please note, this email actually is valid" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "check[example.com")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }

    "display form error message (transferor email does not include TLD)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "example@example")
      val result = confirmEmailController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }
  }
}
