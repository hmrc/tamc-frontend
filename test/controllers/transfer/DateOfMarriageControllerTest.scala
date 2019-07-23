/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.transfer

import controllers.ControllerBaseSpec
import controllers.actions.AuthenticatedActionRefiner
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class DateOfMarriageControllerTest extends ControllerBaseSpec {
  "onPageLoad" should {
    "return success" in {
      val result = controller().onPageLoad()(request)
      status(result) shouldBe OK
    }
  }

  "onPageLoadWithCy" should {
    "redirect to dateOfMarriage, with a welsh language setting" in {
      val result = controller().onPageLoadWithCy()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.transfer.routes.DateOfMarriageController.onPageLoad().url)
      val resolved = Await.result(result, 5 seconds)
      resolved.header.headers.keys should contain("Set-Cookie")
      resolved.header.headers("Set-Cookie") should include("PLAY_LANG=cy")
    }
  }

  "onPageLoadWithEn" should {
    "redirect to dateOfMarriage, with an english language setting" in {
      val result = controller().onPageLoadWithEn()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.transfer.routes.DateOfMarriageController.onPageLoad().url)
      val resolved = Await.result(result, 5 seconds)
      resolved.header.headers.keys should contain("Set-Cookie")
      resolved.header.headers("Set-Cookie") should include("PLAY_LANG=en")
    }
  }

  "onSubmit" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val result = controller().onSubmit()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect the user" when {
      "a valid form is submitted" in {
        val dateOfMarriageInput = DateOfMarriageFormInput(LocalDate.now().minusDays(1))
        val request = FakeRequest().withFormUrlEncodedBody(
          "dateOfMarriage.year" -> dateOfMarriageInput.dateOfMarriage.getYear.toString,
          "dateOfMarriage.month" -> dateOfMarriageInput.dateOfMarriage.getMonthOfYear.toString,
          "dateOfMarriage.day" -> dateOfMarriageInput.dateOfMarriage.getDayOfMonth.toString
        )
        val registrationFormInput = RegistrationFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1), dateOfMarriageInput.dateOfMarriage)
        when(mockCachingService.saveDateOfMarriage(ArgumentMatchers.eq(dateOfMarriageInput))(any(), any()))
          .thenReturn(dateOfMarriageInput)
        when(mockTransferService.getRecipientDetailsFormData()(any(), any()))
          .thenReturn(RecipientDetailsFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1)))
        when(mockTransferService.isRecipientEligible(ArgumentMatchers.eq(Nino(Ninos.nino1)), ArgumentMatchers.eq(registrationFormInput))(any(), any()))
          .thenReturn(true)
        val result = controller().onSubmit()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.eligibleYears().url)
      }
    }
  }

  val currentTaxYear: Int = time.TaxYear.current.startYear

  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  def controller(authAction: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner]) = new DateOfMarriageController(
    messagesApi,
    authAction,
    mockTransferService,
    mockCachingService,
    mockTimeService
  )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  when(mockTimeService.getCurrentDate) thenReturn LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) thenReturn currentTaxYear
}
