/*
 * Copyright 2019 HM Revenue & Customs
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
import models.auth.{PermanentlyAuthenticated, TemporarilyAuthenticated}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.{Controller, Result}
import play.api.test.Helpers._
import test_utils.TestData.Ninos
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientConfidenceLevel, NoActiveSession}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthenticatedActionRefinerTest extends ControllerBaseSpec {

  type AuthRetrievals = Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authAction = new AuthenticatedActionRefiner(mockAuthConnector)
  val retrievals: Retrieval[AuthRetrievals] = Retrievals.credentials and Retrievals.nino and Retrievals.confidenceLevel and Retrievals.saUtr

  class FakeController(authReturn: Future[AuthRetrievals]) extends Controller {
    def onPageLoad() = authAction { implicit request => Ok.withHeaders("authState" -> request.authState.toString) }

    when(mockAuthConnector.authorise(ArgumentMatchers.eq(ConfidenceLevel.L100), ArgumentMatchers.eq(retrievals))(any(), any()))
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
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(ApplicationConfig.ivLoginUrl)
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
      "nino and credentials are returned" in new FakeController(Future.successful(withNinoRetrieval)) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("authState") shouldBe PermanentlyAuthenticated.toString
      }

      "nino is returned without credentials" in new FakeController(Future.successful(withNinoNoCredRetrieval)) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("authState") shouldBe TemporarilyAuthenticated.toString
      }
    }
  }

  object NoActiveSessionException extends NoActiveSession("")

  val noNinoRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] = {
    new ~(new ~(new ~(None, None), ConfidenceLevel.L100), None)
  }
  val withNinoNoCredRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] = {
    new ~(new ~(new ~(None, Some(Ninos.nino1)), ConfidenceLevel.L100), None)
  }
  val withNinoRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] = {
    new ~(new ~(new ~(Some(Credentials("", "")), Some(Ninos.nino1)), ConfidenceLevel.L100), None)
  }
}
