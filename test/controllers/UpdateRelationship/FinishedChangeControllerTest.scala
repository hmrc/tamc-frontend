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
import helpers.FakePertaxAuthAction
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.Injecting
import services.*
import utils.{ControllerBaseTest, MockAuthenticatedAction}

import scala.concurrent.Future
import models.CancellationConfirmation
import views.html.coc.cancellationSubmitted

import java.time.LocalDate

class FinishedChangeControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  lazy val controller: FinishedChangeController             = app.injector.instanceOf[FinishedChangeController]
  lazy val cancellationSubmittedView: cancellationSubmitted = inject[cancellationSubmitted]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "finishUpdate" should {
    "render the cancellation confirmation with the saved end date" in {
      val confirmation = CancellationConfirmation(
        marriageAllowanceEndDate = LocalDate.of(2027, 4, 5)
      )

      when(mockUpdateRelationshipService.getCancellationConfirmation(any()))
        .thenReturn(Future.successful(Some(confirmation)))

      val result = controller.finishUpdate()(request)

      status(result) shouldBe OK

      val doc = contentAsString(result).replace('\u00A0', ' ')

      doc should include("Marriage Allowance cancellation submitted")
      doc should include(
        "We will cancel your Marriage Allowance from 5 April 2027. Until then your Marriage Allowance will show as ‘Active’"
      )
      doc should include("Before you go")
      doc should include("Take a short survey")
    }
  }
}
