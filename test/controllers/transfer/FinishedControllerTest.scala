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

package controllers.transfer

import config.ApplicationConfig
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import helpers.FakePertaxAuthAction
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, MockAuthenticatedAction}

import java.time.LocalDate

class FinishedControllerTest extends ControllerBaseTest {

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

  def controller: FinishedController =
    app.injector.instanceOf[FinishedController]

  when(mockTimeService.getCurrentDate) thenReturn LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) thenReturn currentTaxYear

  "finished" should {
    "return success" when {
      "A notification record is returned and cache is called" in {
        reset(mockCachingService)
        verify(mockCachingService, times(0)).clear()(any())

        when(mockTransferService.getFinishedData(any())(any(), any(), any()))
          .thenReturn(notificationRecord)

        val result = controller.finished()(request)
        status(result) shouldBe OK

        verify(mockCachingService, times(1)).clear()(any())
      }
    }

    "return error" when {
      "error is thrown" in {
        reset(mockCachingService)
        verify(mockCachingService, times(0)).clear()(any())

        when(mockTransferService.getFinishedData(any())(any(), any(), any()))
          .thenThrow(new IllegalArgumentException("123"))

        controller.finished()(request)

        verify(mockCachingService, times(0)).clear()(any())
      }
    }
  }
}
