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

package controllers.actions

import com.google.inject.Inject
import config.ApplicationConfig
import models.auth._
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionRefiner @Inject()(
                                            val authConnector: AuthConnector
                                          )(implicit ec: ExecutionContext)
  extends ActionRefiner[Request, AuthenticatedUserRequest] with ActionBuilder[AuthenticatedUserRequest] with AuthorisedFunctions {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedUserRequest[A]]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L200)
      .retrieve(Retrievals.credentials and Retrievals.nino and Retrievals.confidenceLevel and Retrievals.saUtr) {
      case credentials ~ Some(nino) ~ confidenceLevel ~ saUtr =>
        val authState = if (credentials.isDefined) PermanentlyAuthenticated else TemporarilyAuthenticated
        Future.successful(
          Right(
            AuthenticatedUserRequest(request, authState, Some(confidenceLevel), saUtr.isDefined, credentials.map(_.providerType), Nino(nino))
          )
        )
      case _ =>
        throw new Exception("Nino not found")
    } recover {
      case _: BearerTokenExpired | _: InvalidBearerToken =>
        Left(Redirect("/sign-in"))
      case _: InsufficientConfidenceLevel =>
        Left(Redirect(ApplicationConfig.ivUpliftUrl))
      case _: NoActiveSession =>
        Left(Redirect(ApplicationConfig.ivLoginUrl))
    }
  }
}
