/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.actions.AuthRetrievals
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.Helpers._
import play.mvc.Controller
import test_utils.TestData.Ninos
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import utils.ControllerBaseTest
import utils.RetrivalHelper._

import scala.concurrent.Future

class AuthRetrievalsTest extends ControllerBaseTest {

  type Retrievals = Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String]
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val retrievals: Retrieval[Retrievals] = Retrievals.credentials and Retrievals.nino and Retrievals.confidenceLevel and Retrievals.saUtr

  val applicationConfig: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
    ).configure(
    "metrics.jvm" -> false
    ).build()

  val authAction = app.injector.instanceOf[AuthRetrievals]

  class FakeController extends Controller {
    def onPageLoad(): Action[AnyContent] = authAction {
      implicit request => Ok(request.nino.nino)
    }
  }


  "AuthRetrievals" should {
    "throw exception" when {
      "no nino is found" in new FakeController {

        when(mockAuthConnector.authorise[Retrievals](any(), any())(any(), any()))
          .thenReturn(Future.successful(noNinoRetrieval))

        intercept[Exception] {
          await(onPageLoad()(request))
        }.getMessage shouldBe "Nino not found"
      }
    }

    "return success" when {
      "nino is returned" in new FakeController {

        when(mockAuthConnector.authorise[Retrievals](any(), any())(any(), any()))
          .thenReturn(Future.successful(withNinoRetrieval))

        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        contentAsString(result) shouldBe Ninos.nino1
      }
    }
  }

  val noNinoRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] =
    None ~ None ~ ConfidenceLevel.L200 ~ None

  val withNinoRetrieval: Option[Credentials] ~ Option[String] ~ ConfidenceLevel ~ Option[String] =
    Some(Credentials("", "")) ~ Some(Ninos.nino1) ~ ConfidenceLevel.L200 ~ None

}
