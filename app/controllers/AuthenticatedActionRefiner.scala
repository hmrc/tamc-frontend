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
import controllers.TestAuthController.{Ok, Redirect, authorised}
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.domain.Nino

import scala.concurrent.{ExecutionContext, Future}

final case class AuthenticatedUserRequest[A](request: Request[A], isLoggedIn: Boolean, nino: Nino) extends WrappedRequest[A](request)

class AuthenticatedActionRefiner @Inject()(
                                            val authConnector: AuthConnector
                                          )(implicit ec: ExecutionContext) extends ActionRefiner[Request, AuthenticatedUserRequest] with ActionBuilder[AuthenticatedUserRequest] with AuthorisedFunctions {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthenticatedUserRequest[A]]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authorised(ConfidenceLevel.L100).retrieve(Retrievals.credentials and Retrievals.nino) {
      case credentials ~ Some(nino)  =>
        Future.successful(Right(AuthenticatedUserRequest(request, credentials.isDefined, Nino(nino))))
      case _ =>
        Future.failed(new Exception("no required nino found"))
    }.recover {
      case _: InsufficientConfidenceLevel =>
        Left(Ok("redirect perm uplift for gg user "))
      case _: NoActiveSession =>
        Left(Ok("rediredct some tempory uplift"))
    }
  }

  //"http://localhost:9948/mdtp/registration?origin=ma&confidenceLevel=100&completionURL=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fafter-eligibility&failureURL=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fabc"
}
