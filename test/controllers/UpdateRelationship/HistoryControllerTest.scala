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

package controllers.UpdateRelationship

import controllers.ControllerViewTestHelper
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import errors._
import helpers.FakePertaxAuthAction
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Injecting}
import services._
import utils.{ControllerBaseTest, CreateRelationshipRecordsHelper, MockAuthenticatedAction}
import viewModels._
import views.html.coc.history_summary
import views.html.errors.{citizen_not_found, transferor_not_found, try_later}

import scala.concurrent.Future

class HistoryControllerTest extends ControllerBaseTest with ControllerViewTestHelper with CreateRelationshipRecordsHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val historySummaryViewModelImpl: HistorySummaryViewModelImpl = instanceOf[HistorySummaryViewModelImpl]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: HistoryController = app.injector.instanceOf[HistoryController]

  val tryLaterView: try_later = inject[views.html.errors.try_later]
  val historySummaryView: history_summary = inject[views.html.coc.history_summary]
  val transferNotFoundView: transferor_not_found = inject[views.html.errors.transferor_not_found]
  val citizenNotFoundView: citizen_not_found = inject[views.html.errors.citizen_not_found]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "history" should {
    "display the history summary page with a status of OK" in {
      val relationshipRecords = createRelationshipRecords()
      val historySummaryViewModel = historySummaryViewModelImpl(relationshipRecords.primaryRecord.role,
        relationshipRecords.hasMarriageAllowanceBeenCancelled,
        relationshipRecords.loggedInUserInfo)
      when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
        .thenReturn(Future.successful(relationshipRecords))
      when(mockUpdateRelationshipService.saveRelationshipRecords(ArgumentMatchers.eq(relationshipRecords))(any(), any()))
        .thenReturn(Future.successful(relationshipRecords))

      val result = controller.history()(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs historySummaryView(historySummaryViewModel)
    }
  }

  "History" should {
    "redirect to how-it-works" when {
      "there is no active (primary) record" in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(NoPrimaryRecordError()))
        val result = controller.history()(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HowItWorksController.howItWorks().url)
      }
    }

    "redirect to transfer-controller" when {
      "there is no active (primary) record for a govuk user who has already logged in" in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(NoPrimaryRecordError()))

        val request: Request[AnyContent] = FakeRequest("GET", "/marriage-allowance-application/history")
          .withHeaders("Referer" -> "https://www.gov.uk/")
        val result = controller.history()(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.TransferAllowanceController.transfer().url)
      }

      "there is no active (primary) record for a govuk user who is currently logging in" in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(NoPrimaryRecordError()))

        val request: Request[AnyContent] = FakeRequest("GET", "/marriage-allowance-application/history")
          .withHeaders("Referer" -> "https://www.access.service.gov.uk/")
        val result = controller.history()(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.TransferAllowanceController.transfer().url)
      }
    }

    "display an error page" when {
      "a TransferorNotFound error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(TransferorNotFound()))

        val result = controller.history()(request)
        status(result) shouldBe OK
        result rendersTheSameViewAs transferNotFoundView()
      }

      "a BadFetchRequest error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(BadFetchRequest()))

        val result = controller.history()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs tryLaterView()
      }

      "a CitizenNotFound error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(CitizenNotFound()))

        val result = controller.history()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs citizenNotFoundView()
      }

      "a MultipleActiveRecordError error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(MultipleActiveRecordError()))

        val result = controller.history()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs tryLaterView()
      }
    }
  }

}
