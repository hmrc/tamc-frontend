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
import forms.EmailForm.emailForm
import helpers.FakePertaxAuthAction
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
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.RequestBuilder._
import utils.{ControllerBaseTest, MockAuthenticatedAction}
import views.html.coc.email

import scala.concurrent.Future

class ConfirmEmailControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  val failedFuture: Future[Nothing] = Future.failed(new RuntimeException("test"))

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: ConfirmEmailController = app.injector.instanceOf[ConfirmEmailController]

  val emailView: email = inject[views.html.coc.email]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "confirmEmail" should {
    "display the confirm email page" when {
      "an email is recovered from the cache" in {
        val email = EmailAddress("test@test.com")
        when(mockUpdateRelationshipService.getEmailAddress(any()))
          .thenReturn(Future.successful(Some(email)))

        val result = controller.confirmEmail(request)
        val populatedForm = emailForm.fill(EmailAddress(email))

        status(result) shouldBe OK
        result rendersTheSameViewAs emailView(populatedForm)
      }

      "no email is recovered from the cache" in {
        when(mockUpdateRelationshipService.getEmailAddress(any())).thenReturn(Future.successful(None))

        val result = controller.confirmEmail(request)

        status(result) shouldBe OK
        result rendersTheSameViewAs emailView(emailForm)
      }

      "fail to get data from cache" in {
        when(mockUpdateRelationshipService.getEmailAddress(any())).thenReturn(failedFuture)

        val result = controller.confirmEmail(request)

        status(result) shouldBe OK
        result rendersTheSameViewAs emailView(emailForm)
      }
    }
  }

  "confirmYourEmailActionUpdate" should {
    "redirect to the confirmUpdate page" in {
      val emailAddress = EmailAddress("example@example.com")
      when(mockUpdateRelationshipService.saveEmailAddress(ArgumentMatchers.eq(emailAddress))(any())).
        thenReturn(Future.successful(emailAddress))

      val request = buildFakePostRequest("transferor-email" -> emailAddress)
      val result = controller.confirmYourEmailActionUpdate()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.ConfirmChangeController.confirmUpdate().url)
    }

    "return a bad request" when {
      "a form error has occurred" in {
        val request = buildFakePostRequest("transferor-email" -> "")
        val result = controller.confirmYourEmailActionUpdate()(request)

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

}
