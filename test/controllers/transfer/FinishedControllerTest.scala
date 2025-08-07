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
import models.*
import models.auth.AuthenticatedUserRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import services.{TimeService, TransferService}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, EmailAddress, MockAuthenticatedAction, NinoGenerator}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class FinishedControllerTest extends ControllerBaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  val currentTaxYear: Int = time.TaxYear.current.startYear
  val mockTransferService: TransferService = mock[TransferService]
  val mockTimeService: TimeService = mock[TimeService]
  val notificationRecord: NotificationRecord = NotificationRecord(EmailAddress("test@test.com"))
  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[TimeService].toInstance(mockTimeService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def controller: FinishedController =
    app.injector.instanceOf[FinishedController]

  when(mockTimeService.getCurrentDate) `thenReturn` LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) `thenReturn` currentTaxYear

  when(mockTransferService.getRecipientDetailsFormData()(any[AuthenticatedUserRequest[?]], any[ExecutionContext]))
    .thenReturn(Future.successful(RecipientDetailsFormInput("Alex", "Smith", Gender("M"), Nino(nino))))

  "finished" should {
    "return success" when {
      "A notification record is returned" in {
        when(mockTransferService.getFinishedData(any())(any(), any()))
          .thenReturn(Future.successful(notificationRecord))

        val result = controller.finished()(request)
        status(result) shouldBe OK
      }
    }

    "return error" when {
      "error is thrown" in {
        when(mockTransferService.getFinishedData(any())(any(), any()))
          .thenThrow(new IllegalArgumentException("123"))

        val result = controller.finished()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
