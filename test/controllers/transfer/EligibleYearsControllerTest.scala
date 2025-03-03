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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{CachingService, TimeService, TransferService}
import test_utils.data.RecipientRecordData
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, EmailAddress, MockAuthenticatedAction}

import java.time.LocalDate
import scala.concurrent.Future

class EligibleYearsControllerTest extends ControllerBaseTest {

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

  def controller: EligibleYearsController =
    app.injector.instanceOf[EligibleYearsController]

  when(mockTimeService.getCurrentDate) `thenReturn` LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) `thenReturn` currentTaxYear

  "eligibleYears" should {
    "return a success" when {
      "there are available tax years including current year" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTimeService.getStartDateForTaxYear(any())).thenReturn(time.TaxYear.current.starts)
        val result = controller.eligibleYears()(request)
        status(result) shouldBe OK
      }
    }

    "redirect the user " when {
      "there are available tax years not including current year" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.eligibleYears()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ApplyByPostController.applyByPost().url)
      }
    }

    "throw an exception and recover user to error page" when {
      "available tax years is empty" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = false,
                Nil,
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.eligibleYears()(request)
        status(result) shouldBe OK
      }
    }
  }

  "eligibleYearsAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "abc")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect the user" when {
      "extra years is not empty and applyForCurrentYear is true" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "true")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(currentTaxYear)))(any()))
          .thenReturn(Future.successful(List(currentTaxYear)))
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail().url)
        verify(mockTransferService, times(1)).saveSelectedYears(ArgumentMatchers.eq(List(currentTaxYear)))(any())
      }

      "extra years is not empty and applyForCurrentYear is false" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "false")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(Nil))(any())).thenReturn(Future.successful(Nil))
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.DoNotApplyController.doNotApply().url)
        verify(mockTransferService, times(1)).saveSelectedYears(ArgumentMatchers.eq(Nil))(any())
      }

      "extra years is empty and current year is unavailable" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "false")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = false,
                Nil,
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(Nil))(any())).thenReturn(Future.successful(Nil))
        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.DoNotApplyController.doNotApply().url)
      }

      "extra years is empty, current year is available but applyForCurrentYear is true" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "true")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                Nil,
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(currentTaxYear)))(any()))
          .thenReturn(Future.successful(List(currentTaxYear)))
        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail().url)
      }

      "extra years is empty, current year is available and applyForCurrentYear is false" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "false")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                Nil,
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(Nil))(any())).thenReturn(Future.successful(Nil))
        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.DoNotApplyController.doNotApply().url)
      }
    }
  }
}
