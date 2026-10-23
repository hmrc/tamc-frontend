/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.coc.*
import helpers.FakePertaxAuthAction
import models.{RelationshipRecords, Role}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Injecting}
import services.*
import utils.RequestBuilder.*
import utils.{ControllerBaseTest, CreateRelationshipRecordsHelper, MockAuthenticatedAction}
import views.html.coc.reason_for_change

import scala.concurrent.Future

class MakeChangesControllerTest
    extends ControllerBaseTest
    with ControllerViewTestHelper
    with CreateRelationshipRecordsHelper
    with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  lazy val controller: MakeChangesController = app.injector.instanceOf[MakeChangesController]

  val reasonForChangeView: reason_for_change   = inject[views.html.coc.reason_for_change]
  val relationshipRecords: RelationshipRecords = createRelationshipRecords()
  val role: Role                               = relationshipRecords.primaryRecord.role

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
    when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(
      Future.successful(relationshipRecords)
    )
  }

  "makeChange" should {
    "display the make change page" when {
      Seq(
        MakeChangesDecisionForm.Divorce,
        MakeChangesDecisionForm.Bereavement,
        MakeChangesDecisionForm.IncomeChange,
        MakeChangesDecisionForm.WrongSetup,
        MakeChangesDecisionForm.AnotherReason
      ).foreach { answer =>
        s"there is valid data in the cache for $answer answer should be checked" in {
          when(mockUpdateRelationshipService.getMakeChangesDecision(any()))
            .thenReturn(Future.successful(Some(answer)))

          val result = controller.makeChange()(request)

          status(result)        shouldBe OK
          result `rendersTheSameViewAs` reasonForChangeView(
            MakeChangesDecisionForm.form(role).fill(Some(answer)),
            role
          )
          Jsoup
            .parse(contentAsString(result))
            .getElementById(s"stopMAChoice-${answer.toLowerCase}")
            .hasAttr("checked") shouldBe true
        }
      }

      "there is no data in the cache" in {
        when(mockUpdateRelationshipService.getMakeChangesDecision(any())).thenReturn(Future.successful(None))

        val result = controller.makeChange()(request)

        status(result) shouldBe OK
        result `rendersTheSameViewAs` reasonForChangeView(MakeChangesDecisionForm.form(role), role)
      }

      "a non fatal error has occurred when trying to get cached data" in {
        when(mockUpdateRelationshipService.getMakeChangesDecision(any())).thenReturn(failedFuture)

        val result = controller.makeChange()(request)

        status(result) shouldBe OK
        result `rendersTheSameViewAs` reasonForChangeView(MakeChangesDecisionForm.form(role), role)
      }
    }
  }

  "submitMakeChange" should {
    "return the makeChange page with Bad Request status" when {
      "when form with errors is submitted" in {
        val userAnswer = "Bad Request"
        buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)

        val result = controller.submitMakeChange()(request)

        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to the divorce enter year page" when {
      "a user selects the Divorce option" in {
        val userAnswer = MakeChangesDecisionForm.Divorce
        val request    = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)
        when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any()))
          .thenReturn(Future.successful("Divorce"))

        val result = controller.submitMakeChange()(request)

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          controllers.UpdateRelationship.routes.DivorceController.divorceEnterYear().url
        )
      }

      "redirect to the make change page" when {
        "there is an unexpected error" in {
          val request =
            FakeRequest().withFormUrlEncodedBody(MakeChangesDecisionForm.StopMAChoice -> "Test").withMethod("POST")
          val result  = controller.submitMakeChange()(request)

          redirectLocation(result) shouldBe Some(
            controllers.UpdateRelationship.routes.MakeChangesController.makeChange().url
          )
        }
      }
    }

    Seq(MakeChangesDecisionForm.IncomeChange, MakeChangesDecisionForm.WrongSetup, MakeChangesDecisionForm.AnotherReason)
      .foreach { answer =>
        "redirect to the cancel page" when {
          s"a transferor selects $answer" in {
            val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> answer)
            when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(answer))(any()))
              .thenReturn(Future.successful(answer))

            val result = controller.submitMakeChange()(request)

            status(result)           shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(
              controllers.UpdateRelationship.routes.StopAllowanceController.cancel().url
            )
          }
          s"a recipient selects $answer" in {
            val relationshipRecords = createRelationshipRecords("Recipient")
            val request             = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> answer)
            when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(answer))(any()))
              .thenReturn(Future.successful(answer))
            when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
              .thenReturn(Future.successful(relationshipRecords))

            val result = controller.submitMakeChange()(request)

            status(result)           shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(
              controllers.UpdateRelationship.routes.StopAllowanceController.stopAllowance().url
            )
          }
        }
      }

    "redirect to the bereavement page" when {
      "a user selects the bereavement option" in {
        val userAnswer = MakeChangesDecisionForm.Bereavement
        val request    = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)
        when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any()))
          .thenReturn(Future.successful(userAnswer))

        val result = controller.submitMakeChange()(request)

        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(
          controllers.UpdateRelationship.routes.BereavementController.bereavement().url
        )
      }
    }
  }

}
