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

package test_utils

import com.google.inject.Inject
import controllers.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import models.auth._
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.auth.core.{AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MockAuthenticatedAction @Inject()(override val authConnector: AuthConnector) extends AuthenticatedActionRefiner(authConnector) {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedUserRequest[A]]] = {
    Future.successful(
      Right(
        AuthenticatedUserRequest(
          request,
          PermanentlyAuthenticated,
          Some(ConfidenceLevel.L200),
          false,
          Some("GovernmentGateway"),
          Nino(TestData.Ninos.nino1))
      )
    )
  }
}

class MockTemporaryAuthenticatedAction @Inject()(override val authConnector: AuthConnector) extends AuthenticatedActionRefiner(authConnector) {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedUserRequest[A]]] = {
    Future.successful(
      Right(
        AuthenticatedUserRequest(
          request,
          TemporarilyAuthenticated,
          Some(ConfidenceLevel.L200),
          false,
          Some("GovernmentGateway"),
          Nino(TestData.Ninos.nino1))
      )
    )
  }
}

class MockUnauthenticatedAction @Inject()(override val authConnector: AuthConnector) extends UnauthenticatedActionTransformer(authConnector) {

  override protected def transform[A](request: Request[A]): Future[RequestWithAuthState[A]] = {
    Future.successful(RequestWithAuthState(request, Unauthenticated, None, isSA = false, None))
  }
}
