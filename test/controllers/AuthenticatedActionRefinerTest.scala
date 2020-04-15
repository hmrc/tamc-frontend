/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.actions.AuthenticatedActionRefiner
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.{Action, AnyContent, Controller, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import test_utils.TestData.Ninos
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import utils.RetrivalHelper._
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientConfidenceLevel, NoActiveSession}
import utils.ControllerBaseTest

import scala.concurrent.Future

class AuthenticatedActionRefinerTest extends ControllerBaseTest {

  type AuthRetrievals = Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authAction = new AuthenticatedActionRefiner(mockAuthConnector)
  val retrievals: Retrieval[AuthRetrievals] = Retrievals.credentials and Retrievals.nino and Retrievals.confidenceLevel and Retrievals.saUtr

  class FakeController(authReturn: Future[AuthRetrievals]) extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction {
      implicit request => Ok(request.nino.nino)
    }
    when(mockAuthConnector.authorise(ArgumentMatchers.eq(ConfidenceLevel.L200), ArgumentMatchers.eq(retrievals))(any(), any()))
      .thenReturn(authReturn)
  }

  "AuthenticatedActionRefiner" should {
    "redirect the user" when {
      "there is insufficient confidence level" in new FakeController(Future.failed(new InsufficientConfidenceLevel)) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(ApplicationConfig.ivUpliftUrl)
      }

      "there is no active session" in new FakeController(Future.failed(NoActiveSessionException)) {
        val request: Request[AnyContent] = FakeRequest("GET", "/tamc")
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some("/gg/sign-in?continue=%2Ftamc")
      }
    }

    "throw exception" when {
      "no nino is found" in new FakeController(Future.successful(noNinoRetrieval)) {
        intercept[Exception] {
          await(onPageLoad()(request))
        }.getMessage shouldBe "Nino not found"
      }
    }

    "return success" when {
      "nino is returned" in new FakeController(Future.successful(withNinoRetrieval)) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        contentAsString(result) shouldBe Ninos.nino1
      }
    }
  }

  object NoActiveSessionException extends NoActiveSession("")

  val noNinoRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] =
    None ~ None ~ ConfidenceLevel.L200 ~ None

  val withNinoRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] =
    Some(Credentials("", "")) ~ Some(Ninos.nino1) ~ ConfidenceLevel.L200 ~ None

}
