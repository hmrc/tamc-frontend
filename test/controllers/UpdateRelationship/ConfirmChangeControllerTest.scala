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
import helpers.FakePertaxAuthAction
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.Injecting
import services._
import utils.{ControllerBaseTest, MockAuthenticatedAction}
import viewModels._
import views.html.coc.confirmUpdate
import views.html.errors.try_later

import java.time.LocalDate
import scala.concurrent.Future

class ConfirmChangeControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl = instanceOf[ConfirmUpdateViewModelImpl]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: ConfirmChangeController = app.injector.instanceOf[ConfirmChangeController]

  val tryLaterView: try_later = inject[views.html.errors.try_later]
  val confirmUpdateView: confirmUpdate = inject[views.html.coc.confirmUpdate]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "confirmUpdate" should {
    "display the confirmUpdate page" in {

      val loggedInUser = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some("first"), Some("surname"))))
      val divorceDate = LocalDate.now().minusDays(1)
      val emailAddress = "email@email.com"
      val maEndingDate = LocalDate.now().plusDays(1)
      val paEffectiveDate = LocalDate.now().plusDays(2)

      val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

      val confirmUpdateAnswers = ConfirmationUpdateAnswers(loggedInUser, Some(divorceDate), emailAddress, maEndingDates)

      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
        .thenReturn(Future.successful(confirmUpdateAnswers))

      val result = controller.confirmUpdate()(request)

      status(result) shouldBe OK
      result rendersTheSameViewAs confirmUpdateView(confirmUpdateViewModelImpl(confirmUpdateAnswers))
    }

    "return an InternalServerError" when {

      "there is an issue accessing the cache" in {

        when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
          .thenReturn(failedFuture)

        val result = controller.confirmUpdate()(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "submitConfirmUpdate" should {
    "redirect to the finish update page" in {
      when(mockUpdateRelationshipService.updateRelationship(any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mock[UpdateRelationshipRequestHolder]))

      val result = controller.submitConfirmUpdate(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.FinishedChangeController.finishUpdate().url)
    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.updateRelationship(any())(any(), any(), any(), any()))
          .thenReturn(failedFuture)

        val result = controller.submitConfirmUpdate(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs tryLaterView()
      }
    }
  }

}
