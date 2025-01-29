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
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, EmailAddress, MockAuthenticatedAction}
import services.CacheService._

import java.time.LocalDate

class TransferAllowanceControllerTest extends ControllerBaseTest {

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

  def controller: TransferAllowanceController =
    app.injector.instanceOf[TransferAllowanceController]

  when(mockTimeService.getCurrentDate) thenReturn LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) thenReturn currentTaxYear

  "transfer" should {
    "return success" in {
      val result = controller.transfer()(request)
      status(result) shouldBe OK
    }
  }

  "transferAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val recipientDetails: RecipientDetailsFormInput =
          RecipientDetailsFormInput("Test", "User", Gender("M"), Nino(Ninos.nino2))
        when(mockCachingService.put[RecipientDetailsFormInput](ArgumentMatchers.eq(CACHE_RECIPIENT_DETAILS), ArgumentMatchers.eq(recipientDetails))(any(), any()))
          .thenReturn(recipientDetails)
        val result = controller.transferAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }
    "redirect the user" when {
      "a valid form is submitted" in {
        val recipientDetails: RecipientDetailsFormInput =
          RecipientDetailsFormInput("Test", "User", Gender("M"), Nino(Ninos.nino2))
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "name"      -> "Test",
          "last-name" -> "User",
          "gender"    -> "M",
          "nino"      -> Ninos.nino2
        )
        when(mockCachingService.put[RecipientDetailsFormInput](ArgumentMatchers.eq(CACHE_RECIPIENT_DETAILS), ArgumentMatchers.eq(recipientDetails))(any(), any()))
          .thenReturn(recipientDetails)
        val result = controller.transferAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.DateOfMarriageController.dateOfMarriage().url)
      }
    }
  }
}
