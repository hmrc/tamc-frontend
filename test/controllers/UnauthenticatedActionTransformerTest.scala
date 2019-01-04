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

import models.auth.{PermanentlyAuthenticated, TemporarilyAuthenticated, Unauthenticated}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{Controller, Result}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel, InsufficientConfidenceLevel, NoActiveSession}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UnauthenticatedActionTransformerTest extends ControllerBaseSpec {

  type AuthRetrievals = ConfidenceLevel ~ Option[String] ~ Option[Credentials]
  val retrievals: Retrieval[AuthRetrievals] = Retrievals.confidenceLevel and Retrievals.saUtr and Retrievals.credentials
  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val authAction = new UnauthenticatedActionTransformer(mockAuthConnector)

  class FakeController(authReturn: Future[AuthRetrievals]) extends Controller {
    def onPageLoad() = authAction ( implicit request ⇒ Ok.withHeaders("authState" → request.authState.toString))
    when(mockAuthConnector.authorise(ArgumentMatchers.eq(ConfidenceLevel.L100), ArgumentMatchers.eq(retrievals))(any(), any()))
      .thenReturn(authReturn)
  }

  "UnauthenticatedActionTransformer" should {
    "return a success" when {
      "details are successfully retrieved and credentials have been defined" in new FakeController(withCredRetrieval) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("authState") shouldBe PermanentlyAuthenticated.toString
      }

      "details are successfully retrieved and credentials are not defined" in new FakeController(withoutCredRetrieval) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("authState") shouldBe TemporarilyAuthenticated.toString
      }

      "NoActiveSession is returned from auth connector" in new FakeController(Future.failed(NoActiveSessionException)) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("authState") shouldBe Unauthenticated.toString
      }

      "InsufficientConfidenceLevel is returned from auth connector" in new FakeController(Future.failed(InsufficientConfidenceLevel())) {
        val result: Future[Result] = onPageLoad()(request)
        status(result) shouldBe OK
        result.header.headers("authState") shouldBe Unauthenticated.toString
      }
    }
  }

  object NoActiveSessionException extends NoActiveSession("")
  val withCredRetrieval: AuthRetrievals = new ~(new ~(ConfidenceLevel.L100, None), Some(Credentials("", "")))
  val withoutCredRetrieval: AuthRetrievals = new ~(new ~(ConfidenceLevel.L100, None), None)
}
