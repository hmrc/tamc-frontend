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

package controllers.transfer

import config.ApplicationConfig
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import helpers.FakePertaxAuthAction
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CacheService.CACHE_MARRIAGE_DATE
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, EmailAddress, MockAuthenticatedAction}

import java.time.LocalDate

class DateOfMarriageControllerTest extends ControllerBaseTest {

  val currentTaxYear: Int = time.TaxYear.current.startYear
  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]
  val notificationRecord: NotificationRecord = NotificationRecord(EmailAddress("test@test.com"))
  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[CachingService].toInstance(mockCachingService),
      bind[TimeService].toInstance(mockTimeService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def controller: DateOfMarriageController =
    app.injector.instanceOf[DateOfMarriageController]

  when(mockTimeService.getCurrentDate) thenReturn LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) thenReturn currentTaxYear


  "dateOfMarriage" should {
    "return success" in {
      val result = controller.dateOfMarriage()(request)
      status(result) shouldBe OK
    }
  }

  "dateOfMarriageAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val result = controller.dateOfMarriageAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect the user" when {
      "a valid form is submitted" in {
      val dateOfMarriageInput = DateOfMarriageFormInput(LocalDate.now().minusYears(2))
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.year"  -> dateOfMarriageInput.dateOfMarriage.getYear.toString,
        "dateOfMarriage.month" -> s"0${dateOfMarriageInput.dateOfMarriage.getMonthValue.toString}",
        "dateOfMarriage.day"   -> dateOfMarriageInput.dateOfMarriage.getDayOfMonth.toString
      )
      val registrationFormInput =
        RegistrationFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1), dateOfMarriageInput.dateOfMarriage)

      when(mockCachingService.put[DateOfMarriageFormInput](ArgumentMatchers.eq(CACHE_MARRIAGE_DATE), ArgumentMatchers.eq(dateOfMarriageInput))(any(), any()))
        .thenReturn(dateOfMarriageInput)

      when(mockTransferService.getRecipientDetailsFormData()(any(), any()))
        .thenReturn(RecipientDetailsFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1)))
      when(
        mockTransferService.isRecipientEligible(
          ArgumentMatchers.eq(Nino(Ninos.nino1)),
          ArgumentMatchers.eq(registrationFormInput)
        )(any(), any(), any())
      )
        .thenReturn(true)
      val result = controller.dateOfMarriageAction()(request)
      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.transfer.routes.ChooseYearsController.chooseYears().url)
    }

      "the dateOfMarriage is within the current tax year" in {
        val dateOfMarriageInput = DateOfMarriageFormInput(LocalDate.now().minusDays(1))
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "dateOfMarriage.year"  -> dateOfMarriageInput.dateOfMarriage.getYear.toString,
          "dateOfMarriage.month" -> s"0${dateOfMarriageInput.dateOfMarriage.getMonthValue.toString}",
          "dateOfMarriage.day"   -> dateOfMarriageInput.dateOfMarriage.getDayOfMonth.toString
        )
        val registrationFormInput =
          RegistrationFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1), dateOfMarriageInput.dateOfMarriage)

        when(mockCachingService.put[DateOfMarriageFormInput](ArgumentMatchers.eq(CACHE_MARRIAGE_DATE), ArgumentMatchers.eq(dateOfMarriageInput))(any(), any()))
          .thenReturn(dateOfMarriageInput)

        when(mockTransferService.getRecipientDetailsFormData()(any(), any()))
          .thenReturn(RecipientDetailsFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1)))
        when(
          mockTransferService.isRecipientEligible(
            ArgumentMatchers.eq(Nino(Ninos.nino1)),
            ArgumentMatchers.eq(registrationFormInput)
          )(any(), any(), any())
        )
          .thenReturn(true)
        val result = controller.dateOfMarriageAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.EligibleYearsController.eligibleYears().url)
      }

    }
  }
}
