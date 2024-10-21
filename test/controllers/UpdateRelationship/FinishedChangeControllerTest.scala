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
import helpers.FakePertaxAuthAction
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.Injecting
import services._
import uk.gov.hmrc.http.HttpResponse
import utils.{ControllerBaseTest, EmailAddress, MockAuthenticatedAction}
import views.html.coc.finished

import scala.concurrent.Future

class FinishedChangeControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: FinishedChangeController = app.injector.instanceOf[FinishedChangeController]

  val finishedView: finished = inject[views.html.coc.finished]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "finishUpdate" should {
    "return a success" in {
      val email = "email@email.com"
      when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
        .thenReturn(Future.successful(EmailAddress(email)))
      when(mockUpdateRelationshipService.removeCache(any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val result = controller.finishUpdate()(request)

      status(result) shouldBe OK
      result rendersTheSameViewAs finishedView()
    }
  }

}
