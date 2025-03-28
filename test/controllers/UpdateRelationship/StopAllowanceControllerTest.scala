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
import org.mockito.ArgumentMatchers
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
import views.html.coc.{cancel, stopAllowance}

import java.time.LocalDate
import scala.concurrent.Future

class StopAllowanceControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: StopAllowanceController = app.injector.instanceOf[StopAllowanceController]

  val stopAllowanceView: stopAllowance = inject[views.html.coc.stopAllowance]
  val cancelView: cancel = inject[views.html.coc.cancel]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "stopAllowance" should {
    "display the stop allowance page" in {
      val result = controller.stopAllowance(request)
      status(result) shouldBe OK

      result `rendersTheSameViewAs` stopAllowanceView()
    }
  }

  "cancel" should {
    "display the cancel page" in {
      val nowDate = LocalDate.now
      val marriageAllowanceEndingDates = MarriageAllowanceEndingDates(nowDate, nowDate)
      when(mockUpdateRelationshipService.getMAEndingDatesForCancellation).thenReturn(marriageAllowanceEndingDates)
      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(ArgumentMatchers.eq(marriageAllowanceEndingDates))(any())).
        thenReturn(Future.successful(marriageAllowanceEndingDates))

      val result = controller.cancel(request)
      status(result) shouldBe OK

      result `rendersTheSameViewAs` cancelView(marriageAllowanceEndingDates)
    }
  }

}
