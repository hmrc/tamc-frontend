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

package utils

import com.google.inject.Inject
import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import models.auth._
import play.api.mvc.{Request, Result}
import test_utils.TestData
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
          Some(ConfidenceLevel.L200),
          isSA = false,
          Some("GovernmentGateway"),
          Nino(TestData.Ninos.nino1))
      )
    )
  }
}

class MockUnauthenticatedAction @Inject()(override val authConnector: AuthConnector) extends UnauthenticatedActionTransformer(authConnector) {
  override protected def transform[A](request: Request[A]): Future[MaybeAuthenticatedUserRequest[A]] = {
    Future.successful(MaybeAuthenticatedUserRequest(request, None, isSA = false, isAuthenticated = false, authProvider =  None))
  }
}

class MockPermUnauthenticatedAction @Inject()(override val authConnector: AuthConnector) extends UnauthenticatedActionTransformer(authConnector) {
  override protected def transform[A](request: Request[A]): Future[MaybeAuthenticatedUserRequest[A]] = {
    Future.successful(MaybeAuthenticatedUserRequest(request,  None, isSA = false, isAuthenticated = true, authProvider = None))
  }
}
