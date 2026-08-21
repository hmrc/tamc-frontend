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

import controllers.ControllerViewTestHelper
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import helpers.FakePertaxAuthAction
import models.{CurrentAndPreviousYearsEligibility, TaxYear}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers, Injecting}
import services.CacheService.CACHE_IS_LOWER_EARNER
import services.{CachingService, TransferService}
import test_utils.data.RecipientRecordData
import utils.{ControllerBaseTest, MockAuthenticatedAction, TransferErrorHandler}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class YourTotalIncomeControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockTransferService: TransferService   = mock[TransferService]
  val mockCachingService: CachingService     = mock[CachingService]
  val mockErrorHandler: TransferErrorHandler = mock[TransferErrorHandler]
  val mockClock: Clock                       = mock[Clock]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[CachingService].toInstance(mockCachingService),
      bind[TransferErrorHandler].toInstance(mockErrorHandler),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def controller: YourTotalIncomeController =
    app.injector.instanceOf[YourTotalIncomeController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCachingService)
    reset(mockClock)
    reset(mockTransferService)

    when(mockClock.instant()).thenReturn(Instant.now())
    when(mockClock.getZone).thenReturn(ZoneOffset.UTC)
    when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any())).thenReturn(
      Future.successful(
        CurrentAndPreviousYearsEligibility(
          currentYearAvailable = true,
          List(TaxYear(2015)),
          RecipientRecordData.recipientRecord.data,
          RecipientRecordData.recipientRecord.availableTaxYears
        )
      )
    )
  }

  "yourTotalIncome" should {
    "return OK with pre-filled form" when {
      "cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(Some(true)))

        val result = controller.yourTotalIncome()(FakeRequest())
        status(result) mustBe OK
      }
    }
    "return OK with empty form" when {
      "no cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.yourTotalIncome()(FakeRequest())
        status(result) mustBe OK
      }
    }
  }

  "yourTotalIncomeAction" should {
    "return bad request" when {
      "an empty form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yourTotalIncome" -> "")
        val result  = controller.yourTotalIncomeAction()(request)
        status(result) mustBe BAD_REQUEST
      }

      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yourTotalIncome" -> "invalidOption")
        val result  = controller.yourTotalIncomeAction()(request)
        status(result).mustBe(BAD_REQUEST)
      }
    }

    "redirect the user" when {
      "true is selected" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yourTotalIncome" -> "true")

        when(
          mockCachingService
            .put[Boolean](ArgumentMatchers.eq(CACHE_IS_LOWER_EARNER), ArgumentMatchers.eq(true))(any(), any())
        )
          .thenReturn(Future.successful(true))

        val result = controller.yourTotalIncomeAction()(request)
        status(result).mustBe(SEE_OTHER)
        redirectLocation(result).mustBe(
          Some(controllers.transfer.routes.PartnersDetailsController.transfer().url)
        )
      }

      "false is selected" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("yourTotalIncome" -> "false")

        when(
          mockCachingService
            .put[Boolean](ArgumentMatchers.eq(CACHE_IS_LOWER_EARNER), ArgumentMatchers.eq(false))(any(), any())
        )
          .thenReturn(Future.successful(false))

        val result = controller.yourTotalIncomeAction()(request)
        status(result).mustBe(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.transfer.routes.YourTotalIncomeController.invalidIncome().url)
      }
    }
  }
}
