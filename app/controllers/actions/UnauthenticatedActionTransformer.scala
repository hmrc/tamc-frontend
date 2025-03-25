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

package controllers.actions

import com.google.inject.Inject
import models.auth._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class UnauthenticatedActionTransformer @Inject()(
                                                  val authConnector: AuthConnector,
                                                  val parser: BodyParsers.Default
                                                )(implicit val executionContext: ExecutionContext)
  extends ActionTransformer[Request, UserRequest]
    with ActionBuilder[UserRequest, AnyContent] with AuthorisedFunctions {

  override protected def transform[A](request: Request[A]): Future[UserRequest[A]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.confidenceLevel and Retrievals.saUtr and Retrievals.credentials) {
      case cl ~ saUtr ~ credentials =>
        Future.successful(UserRequest(request, Some(cl), isAuthenticated = true, credentials.map(_.providerType), isSA = saUtr.isDefined))
    } recover {
      case _: NoActiveSession | _: InsufficientConfidenceLevel =>
        UserRequest(request, None, isAuthenticated = false, None, isSA = false)
    }
  }
}
