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
import models.{ApplyForEligibleYears, CurrentAndPreviousYearsEligibility, TaxYear}
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers, Injecting}
import services.CacheService.CACHE_CHOOSE_YEARS
import services.{CachingService, TransferService}
import test_utils.data.RecipientRecordData
import utils.{ControllerBaseTest, MockAuthenticatedAction, TransferErrorHandler}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class ChooseYearsControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockTransferService: TransferService   = mock[TransferService]
  val mockCachingService: CachingService     = mock[CachingService]
  val mockErrorHandler: TransferErrorHandler = mock[TransferErrorHandler]
  val mockClock: Clock                       = mock[Clock]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[CachingService].toInstance(mockCachingService),
      bind[TransferErrorHandler].toInstance(mockErrorHandler),
      bind[Clock].toInstance(mockClock),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def controller: ChooseYearsController =
    app.injector.instanceOf[ChooseYearsController]

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

  "chooseYears" should {
    "return OK with pre-filled form" when {
      "cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(Some("currentTaxYear")))

        val result = controller.chooseYears()(FakeRequest())
        status(result) mustBe OK
      }
    }
    "return OK with empty form" when {
      "no cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.chooseYears()(FakeRequest())
        status(result) mustBe OK
      }
    }
    "return OK with empty form for 2024/25 TY" when {
      "no cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(None))
        when(mockClock.instant())
          .thenReturn(Instant.parse("2024-04-06T00:00:00.000Z"))

        val result = controller.chooseYears()(FakeRequest())
        val labels: Elements =
          Jsoup.parse(contentAsString(result)).getElementsByAttribute("for")

        labels.eq(0).text() mustBe "Previous tax years, on or before 6 April 2024"
        labels.eq(1).text() mustBe "Current tax year onwards, from 6 April 2024"
      }
    }
    "return OK with empty form for 2023/24 TY" when {
      "no cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(None))
        when(mockClock.instant())
          .thenReturn(Instant.parse("2024-04-05T23:59:59.999Z"))

        val result = controller.chooseYears()(FakeRequest())
        val labels: Elements =
          Jsoup.parse(contentAsString(result)).getElementsByAttribute("for")

        labels.eq(0).text() mustBe "Previous tax years, on or before 6 April 2023"
        labels.eq(1).text() mustBe "Current tax year onwards, from 6 April 2023"
      }
    }
  }

  "chooseYearsAction" should {
    "return bad request" when {
      "an empty form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value" -> "")
        val result = controller.chooseYearsAction()(request)
        status(result) mustBe BAD_REQUEST
      }

      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value" -> "invalidOption")
        val result = controller.chooseYearsAction()(request)
        status(result).mustBe(BAD_REQUEST)
      }
    }

    "redirect the user" when {
      "currentTaxYear is selected" in {
        val currentTaxYear = ApplyForEligibleYears.CurrentTaxYear.toString
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value[]" -> currentTaxYear)

        when(mockCachingService.put[String](ArgumentMatchers.eq(CACHE_CHOOSE_YEARS), ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(currentTaxYear))

        val result = controller.chooseYearsAction()(request)
        status(result).mustBe(SEE_OTHER)
        redirectLocation(result).mustBe(Some(controllers.transfer.routes.PartnersDetailsController.transfer().url))
      }

      "previousTaxYears is selected" in {
        val previousTaxYears = ApplyForEligibleYears.PreviousTaxYears.toString
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value[]" -> previousTaxYears)

        when(mockCachingService.put[String](ArgumentMatchers.eq(CACHE_CHOOSE_YEARS), ArgumentMatchers.eq(previousTaxYears))(any(), any()))
          .thenReturn(Future.successful(previousTaxYears))

        val result = controller.chooseYearsAction()(request)
        status(result).mustBe(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.transfer.routes.ApplyByPostController.applyByPost().url)
      }

      "there is an unexpected value" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value[0]" -> ApplyForEligibleYears.PreviousTaxYears.toString)

        when(mockCachingService.put[String](ArgumentMatchers.eq(CACHE_CHOOSE_YEARS), ArgumentMatchers.eq("previousTaxYears"))(any(), any()))
          .thenReturn(Future.successful("UnexpectedValue"))

        val result = controller.chooseYearsAction()(request)
        status(result).mustBe(SEE_OTHER)
        redirectLocation(result) mustBe Some(controllers.transfer.routes.ChooseYearsController.chooseYears().url)
      }
    }
  }
}
