/*
 * Copyright 2018 HM Revenue & Customs
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

import com.google.inject.Inject
import models.auth._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

final case class UserRequest[A](request: Request[A], authState: AuthState) extends WrappedRequest[A](request)

class UnauthenticatedActionTransformer @Inject()(
                                               val authConnector: AuthConnector
                                             )(implicit ec: ExecutionContext) extends ActionTransformer[Request, UserRequest] with ActionBuilder[UserRequest] with AuthorisedFunctions {

  override protected def transform[A](request: Request[A]): Future[UserRequest[A]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200) {
      Future.successful(UserRequest(request, authState = PermanentlyAuthenticated))
    }.recover {
      case _: NoActiveSession ⇒
        UserRequest(request, Unauthenticated)
      case _: InsufficientConfidenceLevel ⇒
        UserRequest(request, TemporarilyAuthenticated)
    }
  }
}
