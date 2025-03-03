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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.Injecting
import services._
import utils.{ControllerBaseTest, CreateRelationshipRecordsHelper, MockAuthenticatedAction}
import viewModels._
import views.html.coc.claims
import views.html.errors.try_later

import scala.concurrent.Future

class ClaimsControllerTest extends ControllerBaseTest with ControllerViewTestHelper with CreateRelationshipRecordsHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val claimsViewModelImpl: ClaimsViewModelImpl = instanceOf[ClaimsViewModelImpl]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: ClaimsController = app.injector.instanceOf[ClaimsController]

  val tryLaterView: try_later = inject[views.html.errors.try_later]
  val claimsView: claims = inject[views.html.coc.claims]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "claims" should {
    "display the claims page" in {
      val relationshipRecords = createRelationshipRecords()
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.successful(relationshipRecords))

      val claimsViewModel = claimsViewModelImpl(relationshipRecords.primaryRecord, relationshipRecords.nonPrimaryRecords)
      val result = controller.claims(request)

      status(result) shouldBe OK
      result `rendersTheSameViewAs` claimsView(claimsViewModel)
    }

    "display an error page" when {
      "there is no cached data found" in {
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.failed(CacheMissingRelationshipRecords()))

        val result = controller.claims(request)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        result `rendersTheSameViewAs` tryLaterView()
      }
    }
  }

}
