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
import controllers.ControllerViewTestHelper
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import helpers.FakePertaxAuthAction
import models.*
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Injecting}
import services.{CachingService, TransferService}
import test_utils.data.RecipientRecordData
import utils.{ControllerBaseTest, EmailAddress, MockAuthenticatedAction}
import views.html.errors.no_eligible_years

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class EligibleYearsControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockClock: Clock                       = mock[Clock]
  val mockTransferService: TransferService   = mock[TransferService]
  val mockCachingService: CachingService     = mock[CachingService]
  val notificationRecord: NotificationRecord = NotificationRecord(EmailAddress("test@test.com"))
  val applicationConfig: ApplicationConfig   = instanceOf[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[CachingService].toInstance(mockCachingService),
      bind[Clock].toInstance(mockClock),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def controller: EligibleYearsController =
    app.injector.instanceOf[EligibleYearsController]

  override def beforeEach(): Unit = {
    reset(mockClock)

    when(mockClock.instant()).thenReturn(Instant.parse("2016-04-05T00:00:00.000Z"))
    when(mockClock.getZone).thenReturn(ZoneOffset.UTC)
  }

  def noTaxYearAvailableView: no_eligible_years = inject[views.html.errors.no_eligible_years]

  "eligibleYears" should {
    "return a success" when {
      "there are available tax years including current year" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                previousYears = List(TaxYear(2015)),
                registrationInput = RecipientRecordData.recipientRecord.data,
                availableTaxYears = RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.eligibleYears()(request)
        status(result) shouldBe OK

        val h1: Elements = Jsoup.parse(contentAsString(result)).getElementsByTag("h1")

        h1.eq(0).text() shouldBe "You are applying for the current tax year onwards, from 6 April 2015"
      }

      "there are available tax years including current year for TY 23/24" in {
        when(mockClock.instant()).thenReturn(Instant.parse("2024-04-05T23:59:59.999Z"))
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                previousYears = List(TaxYear(2023)),
                registrationInput = RecipientRecordData.recipientRecord.data,
                availableTaxYears = RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.eligibleYears()(request)
        val h1: Elements = Jsoup.parse(contentAsString(result)).getElementsByTag("h1")

        h1.eq(0).text() shouldBe "You are applying for the current tax year onwards, from 6 April 2023"
      }

      "there are available tax years including current year for TY 24/25" in {
        when(mockClock.instant()).thenReturn(Instant.parse("2024-04-06T00:00:00.000Z"))
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                previousYears = List(TaxYear(2023)),
                registrationInput = RecipientRecordData.recipientRecord.data,
                availableTaxYears = RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        val result = controller.eligibleYears()(request)
        val h1: Elements = Jsoup.parse(contentAsString(result)).getElementsByTag("h1")

        h1.eq(0).text() shouldBe "You are applying for the current tax year onwards, from 6 April 2024"
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
        status(result)           shouldBe SEE_OTHER
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
    "redirect the user" when {
      "extra years is not empty and current year is available" in {
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
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
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(2015)))(any()))
          .thenReturn(Future.successful(List(2015)))

        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail().url)
        verify(mockTransferService, times(1)).saveSelectedYears(ArgumentMatchers.eq(List(2015)))(any())
      }

      "extra years is not empty and current year is unavailable" in {
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
        def messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe OK
        result `rendersTheSameViewAs` noTaxYearAvailableView()(messages, authRequest)
      }

      "extra years is empty and current year is available" in {
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            Future.successful(
              CurrentAndPreviousYearsEligibility(
                currentYearAvailable = true,
                previousYears = Nil,
                RecipientRecordData.recipientRecord.data,
                RecipientRecordData.recipientRecord.availableTaxYears
              )
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(2015)))(any()))
          .thenReturn(Future.successful(List(2015)))
        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          controllers.transfer.routes.ConfirmEmailController.confirmYourEmail().url
        )
      }

      "extra years is empty and current year is unavailable" in {
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
        def messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe OK
        result `rendersTheSameViewAs` noTaxYearAvailableView()(messages, authRequest)
      }
    }
  }
}
