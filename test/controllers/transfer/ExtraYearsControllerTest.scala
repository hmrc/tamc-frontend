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

class ExtraYearsControllerTest extends ControllerBaseTest {

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

  def controller: ExtraYearsController =
    app.injector.instanceOf[ExtraYearsController]

  when(mockTimeService.getCurrentDate) `thenReturn` LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) `thenReturn` currentTaxYear

  "extraYearsAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.extraYearsAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return success" when {
      "furtherYears is not empty" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "selectedYear"              -> "2015",
          "furtherYears"              -> "2014,2013",
          "yearAvailableForSelection" -> "2014"
        )
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(
          mockTransferService.updateSelectedYears(
            ArgumentMatchers.eq(RecipientRecordData.recipientRecord.availableTaxYears),
            ArgumentMatchers.eq(2015),
            ArgumentMatchers.eq(Some(2014))
          )(any(), any())
        )
          .thenReturn(Future.successful(Nil))
        val result = controller.extraYearsAction()(request)
        status(result) shouldBe OK
      }
    }

    "redirect" when {
      "further years is empty" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "selectedYear"              -> "2015",
          "furtherYears"              -> "",
          "yearAvailableForSelection" -> "2014"
        )
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                false,
                List(TaxYear(2015)),
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(
          mockTransferService.updateSelectedYears(
            ArgumentMatchers.eq(RecipientRecordData.recipientRecord.availableTaxYears),
            ArgumentMatchers.eq(2015),
            ArgumentMatchers.eq(Some(2014))
          )(any(), any())
        )
          .thenReturn(Future.successful(Nil))
        val result = controller.extraYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail().url)
      }
    }
  }
}
