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
import forms.coc._
import helpers.FakePertaxAuthAction
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
import utils.{ControllerBaseTest, MockAuthenticatedAction}
import views.html.coc.decision

import scala.concurrent.Future

class ChooseControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  val failedFuture: Future[Nothing] = Future.failed(new RuntimeException("test"))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: ChooseController = app.injector.instanceOf[ChooseController]

  val decisionView: decision = inject[views.html.coc.decision]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "decision" should {
    "display the decision page with cached data" in {
      val cacheData = Some("checkMarriageAllowanceClaim")
      val validFormWithData = CheckClaimOrCancelDecisionForm.form().fill(cacheData)
      when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any())).thenReturn(Future.successful(cacheData))

      val result = controller.decision(request)

      status(result) shouldBe OK
      result rendersTheSameViewAs decisionView(validFormWithData)
    }

    "display a decision page without cached data" when {
      "there is no data return from the cache" in {

        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any())).thenReturn(Future.successful(None))
        val validForm = CheckClaimOrCancelDecisionForm.form()
        val result = controller.decision(request)

        status(result) shouldBe OK
        result rendersTheSameViewAs decisionView(validForm)
      }

      "a non fatal error has occurred when trying to get cached data" in {

        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any()))
          .thenReturn(failedFuture)
        val result = controller.decision(request)
        val validForm = CheckClaimOrCancelDecisionForm.form()

        status(result) shouldBe OK
        result rendersTheSameViewAs decisionView(validForm)
      }
    }
  }

  "submitDecision" should {
    "redirect to the claims page" when {
      "a user selects the checkMarriageAllowanceClaim option" in {
        val userAnswer = CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim
        val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> userAnswer).withMethod("POST")
        when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any()))
          .thenReturn(Future.successful(userAnswer))

        val result = controller.submitDecision(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.ClaimsController.claims().url)
      }
    }
  }

  "submitDecision" should {
    "redirect to the make change page" when {
      "a user selects the stopMarriageAllowance option" in {
        val userAnswer = CheckClaimOrCancelDecisionForm.StopMarriageAllowance
        val request = FakeRequest().withFormUrlEncodedBody(
          CheckClaimOrCancelDecisionForm.DecisionChoice -> userAnswer
        ).withMethod("POST")

        when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any()))
          .thenReturn(Future.successful(CheckClaimOrCancelDecisionForm.StopMarriageAllowance))

        val result = controller.submitDecision(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.MakeChangesController.makeChange().url)
      }
    }

    "return a bad request" when {
      "the form submission has a blank value POST method" in {
        val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> "").withMethod("POST")
        val result = controller.submitDecision(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to the decision page" when {
      "there is an unexpected error" in {
        val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> "Test").withMethod("POST")
        val result = controller.submitDecision(request)
        redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.ChooseController.decision().url)
      }
    }
  }

}
