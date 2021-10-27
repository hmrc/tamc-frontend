/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import controllers.actions.UnauthenticatedActionTransformer
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Injecting
import play.api.inject.bind
import play.api.mvc.Results.Ok
import play.mvc.Controller
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientConfidenceLevel, NoActiveSession}
import utils.ControllerBaseTest
import utils.RetrivalHelper._

import scala.concurrent.Future

class UnauthenticatedActionTransformerTest extends ControllerBaseTest with Injecting {

  type AuthRetrievals = ConfidenceLevel ~ Option[String] ~ Option[Credentials]
  val retrievals: Retrieval[AuthRetrievals] = Retrievals.confidenceLevel and Retrievals.saUtr and Retrievals.credentials
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authAction: UnauthenticatedActionTransformer = inject[UnauthenticatedActionTransformer]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector)
    ).build()


  class FakeController(authReturn: Future[AuthRetrievals]) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction(
      implicit request => Ok.withHeaders("isAuthenticated" -> request.isAuthenticated.toString))

    when(mockAuthConnector.authorise(ArgumentMatchers.eq(ConfidenceLevel.L200), ArgumentMatchers.eq(retrievals))(any(), any()))
      .thenReturn(authReturn)
  }

  "UnauthenticatedActionTransformer" should {
    "return a success" when {
      "details are successfully retrieved and credentials have been defined" in new FakeController(withCredRetrieval) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("isAuthenticated") shouldBe "true"
      }

      "NoActiveSession is returned from auth connector" in new FakeController(Future.failed(NoActiveSessionException)) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("isAuthenticated") shouldBe "false"
      }

      "InsufficientConfidenceLevel is returned from auth connector" in new FakeController(Future.failed(InsufficientConfidenceLevel())) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("isAuthenticated") shouldBe "false"
      }
    }
  }

  object NoActiveSessionException extends NoActiveSession("")

  val withCredRetrieval: AuthRetrievals = ConfidenceLevel.L200 ~  None ~ Some(Credentials("", ""))
}
