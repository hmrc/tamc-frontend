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
import play.api.mvc._
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

final case class UserRequest[A](request: Request[A], isLoggedIn: Boolean) extends WrappedRequest[A](request)

class UnauthenticatedActionTransformer @Inject()(
                                               val authConnector: AuthConnector
                                             )(implicit ec: ExecutionContext) extends ActionTransformer[Request, UserRequest] with ActionBuilder[UserRequest] with AuthorisedFunctions {

  override protected def transform[A](request: Request[A]): Future[UserRequest[A]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    val isLoggedIn: Future[Boolean] = authorised() {
      Future.successful(true)
    }.recover {
      case _: NoActiveSession =>
        false
    }

    isLoggedIn.map(UserRequest(request, _))
  }
}
