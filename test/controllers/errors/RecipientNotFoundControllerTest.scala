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

package controllers.errors

import controllers.ControllerViewTestHelper
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import helpers.FakePertaxAuthAction
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import play.api.test.Injecting
import utils.{ControllerBaseTest, MockAuthenticatedAction}
import views.html.errors.recipient_not_found

class RecipientNotFoundControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  lazy val controller: RecipientNotFoundController = app.injector.instanceOf[RecipientNotFoundController]

  val recipientNotFoundView: recipient_not_found = inject[views.html.errors.recipient_not_found]

  "transferorNotFoundError" should {
    "display the Transferor Not Found page" in {
      val result = controller.recipientNotFoundError(request)
      status(result) shouldBe OK

      result `rendersTheSameViewAs` recipientNotFoundView()
    }
  }
}
