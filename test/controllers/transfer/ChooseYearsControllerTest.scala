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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services.CacheService.CACHE_CHOOSE_YEARS
import services.{CachingService, TimeService, TransferService}
import test_utils.data.RecipientRecordData
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, MockAuthenticatedAction, TransferErrorHandler}

import java.time.LocalDate
import scala.concurrent.Future

class ChooseYearsControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val currentTaxYear: Int = time.TaxYear.current.startYear
  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockErrorHandler: TransferErrorHandler = mock[TransferErrorHandler]
  val mockTimeService: TimeService = mock[TimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[CachingService].toInstance(mockCachingService),
      bind[TransferErrorHandler].toInstance(mockErrorHandler),
      bind[TimeService].toInstance(mockTimeService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def controller: ChooseYearsController =
    app.injector.instanceOf[ChooseYearsController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCachingService)
    reset(mockTimeService)
    reset(mockTransferService)

    when(mockTimeService.getCurrentDate).thenReturn(LocalDate.now())
    when(mockTimeService.getCurrentTaxYear).thenReturn(currentTaxYear)
    when(mockTimeService.getStartDateForTaxYear(any())).thenReturn(time.TaxYear.current.starts)
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
        val cachedData = Some("currentTaxYear")
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(cachedData))

        val result = controller.chooseYears()(request)
        status(result) shouldBe OK
      }
    }
    "return OK with empty form" when {
      "no cached data is available" in {
        when(mockCachingService.get[String](any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.chooseYears()(request)
        status(result) mustBe OK
      }
    }
    }

  "chooseYearsAction" should {
    "return bad request" when {
      "an empty form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value" -> "")
        val result = controller.chooseYearsAction()(request)
        status(result) shouldBe BAD_REQUEST
      }

      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value" -> "invalidOption")
        val result = controller.chooseYearsAction()(request)
        status(result).shouldBe(BAD_REQUEST)
      }
    }

    "redirect the user" when {
      "currentTaxYear is selected" in {
        val currentTaxYear = ApplyForEligibleYears.CurrentTaxYear.toString
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value[]" -> currentTaxYear)

        when(mockCachingService.put[String](ArgumentMatchers.eq(CACHE_CHOOSE_YEARS), ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(currentTaxYear))

        val result = controller.chooseYearsAction()(request)
        status(result).shouldBe(SEE_OTHER)
        redirectLocation(result).shouldBe(Some(controllers.transfer.routes.TransferAllowanceController.transfer().url))
      }

      "previousTaxYears is selected" in {
        val previousTaxYears = ApplyForEligibleYears.PreviousTaxYears.toString
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value[]" -> previousTaxYears)

        when(mockCachingService.put[String](ArgumentMatchers.eq(CACHE_CHOOSE_YEARS), ArgumentMatchers.eq(previousTaxYears))(any(), any()))
          .thenReturn(Future.successful(previousTaxYears))

        val result = controller.chooseYearsAction()(request)
        status(result).shouldBe(SEE_OTHER)
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ApplyByPostController.applyByPost().url)
      }

      "there is an unexpected value" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("value[0]" -> ApplyForEligibleYears.PreviousTaxYears.toString)

        when(mockCachingService.put[String](ArgumentMatchers.eq(CACHE_CHOOSE_YEARS), ArgumentMatchers.eq("previousTaxYears"))(any(), any()))
          .thenReturn(Future.successful("UnexpectedValue"))

        val result = controller.chooseYearsAction()(request)
        status(result).shouldBe(SEE_OTHER)
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ChooseYearsController.chooseYears().url)
      }
      }
    }
}
