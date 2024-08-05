/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.UpdateRelationship

import controllers.ControllerViewTestHelper
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import errors._
import forms.coc._
import helpers.FakePertaxAuthAction
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services._
import utils.RequestBuilder._
import utils.{ControllerBaseTest, MockAuthenticatedAction}
import viewModels._
import views.html.coc.{divorce_end_explanation, divorce_select_year}
import views.html.errors.try_later

import java.time.LocalDate
import scala.concurrent.Future

class DivorceControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockTimeService: TimeService = mock[TimeService]
  val divorceSelectYearForm: DivorceSelectYearForm = instanceOf[DivorceSelectYearForm]
  val divorceEndExplanationViewModelImpl: DivorceEndExplanationViewModelImpl = instanceOf[DivorceEndExplanationViewModelImpl]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[TimeService].toInstance(mockTimeService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: DivorceController = app.injector.instanceOf[DivorceController]

  val tryLaterView: try_later = inject[views.html.errors.try_later]
  val divorceSelectYearView: divorce_select_year = inject[views.html.coc.divorce_select_year]
  val divorceEndExplanationView: divorce_end_explanation = inject[views.html.coc.divorce_end_explanation]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
    reset(mockTimeService)
  }

  "divorceEnterYear" should {
    "display the enter a divorce year page with a status of OK" when {
      "there is data in the cache" in {
        val divorceDateInThePast = LocalDate.now().minusDays(1)
        when(mockUpdateRelationshipService.getDivorceDate(any())).thenReturn(Future.successful(Some(divorceDateInThePast)))

        val result = controller.divorceEnterYear(request)

        status(result) shouldBe OK
        result rendersTheSameViewAs divorceSelectYearView(divorceSelectYearForm.form.fill(divorceDateInThePast))
      }

      "there is no data in the cache" in {
        when(mockUpdateRelationshipService.getDivorceDate(any()))
          .thenReturn(Future.successful(None))

        val result = controller.divorceEnterYear()(request)

        status(result) shouldBe OK
        result rendersTheSameViewAs divorceSelectYearView(divorceSelectYearForm.form)
      }
    }
  }

  "submitDivorceEnterYear" should {
    "redirect to the divorce end explanation page" when {
      "the user enters a valid divorce date in the past" in {
        val divorceDateInThePast = LocalDate.now().minusDays(1)
        val request = buildFakePostRequest("dateOfDivorce.year" -> divorceDateInThePast.getYear.toString,
          "dateOfDivorce.month" -> divorceDateInThePast.getMonthValue.toString,
          "dateOfDivorce.day" -> divorceDateInThePast.getDayOfMonth.toString)

        when(mockUpdateRelationshipService.saveDivorceDate(ArgumentMatchers.eq(divorceDateInThePast))(any()))
          .thenReturn(Future.successful(divorceDateInThePast))

        when(mockTimeService.getCurrentDate)
          .thenReturn(LocalDate.now())

        val result = controller.submitDivorceEnterYear()(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.DivorceController.divorceEndExplanation().url)
      }
    }

    "return a bad request" when {
      "an invalid date is submitted" in {
        val invalidRequest = FakeRequest().withFormUrlEncodedBody(
          "dateOfDivorce.year" -> "year",
          "dateOfDivorce.month" -> "month",
          "dateOfDivorce.day" -> "day"
        ).withMethod("POST")

        val result = controller.submitDivorceEnterYear(invalidRequest)

        status(result) shouldBe BAD_REQUEST
      }
    }

    "display an error page" when {
      "there is an issue saving to the cache" in {
        when(mockUpdateRelationshipService.saveDivorceDate(any())(any())).thenReturn(failedFuture)

        val divorceDateInThePast = LocalDate.now().minusDays(1)
        val request = FakeRequest().withFormUrlEncodedBody("dateOfDivorce.year" -> divorceDateInThePast.getYear.toString,
          "dateOfDivorce.month" -> divorceDateInThePast.getMonthValue.toString,
          "dateOfDivorce.day" -> divorceDateInThePast.getDayOfMonth.toString).withMethod("POST")

        when(mockTimeService.getCurrentDate)
          .thenReturn(LocalDate.now())

        val result = controller.submitDivorceEnterYear(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs tryLaterView()
      }
    }
  }

  "divorceEndExplanation" should {
    "display the divorceEndExplanation page" in {
      val role = Transferor
      val now = LocalDate.now()
      val divorceDate = now.minusDays(1)
      val maEndingDate = now.plusDays(1)
      val paEffectiveDate = now.plusDays(2)
      val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((role, divorceDate)))
      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate))
        .thenReturn(Future.successful(maEndingDates))
      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(ArgumentMatchers.eq(maEndingDates))(any()))
        .thenReturn(Future.successful(maEndingDates))

      val viewModel = divorceEndExplanationViewModelImpl(role, divorceDate, maEndingDates)
      val result = controller.divorceEndExplanation()(request)

      status(result) shouldBe OK
      result rendersTheSameViewAs divorceEndExplanationView(viewModel)
    }

    "display an error page" when {
      "an error occurs retrieving divorce data" in {
        when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
          .thenReturn(Future.failed(CacheMissingRelationshipRecords()))

        val result = controller.divorceEndExplanation()(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs tryLaterView()
      }

      "an error has occurred whilst saving cache data" in {
        val role = Transferor
        val divorceDate = LocalDate.now().minusDays(1)
        val maEndingDate = LocalDate.now().plusDays(1)
        val paEffectiveDate = LocalDate.now().plusDays(2)
        val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

        when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
          .thenReturn(Future.successful((role, divorceDate)))
        when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate))
          .thenReturn(Future.successful(maEndingDates))
        when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(ArgumentMatchers.eq(maEndingDates))(any()))
          .thenReturn(failedFuture)

        val result = controller.divorceEndExplanation()(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs tryLaterView()
      }
    }
  }

}
