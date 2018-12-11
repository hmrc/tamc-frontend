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

import config.WSHttp
import connectors.ApplicationAuthConnector
import play.api.Play
import play.api.mvc.{Action, AnyContent, Controller, Request}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{CorePost, HeaderCarrier}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object TestAuthController extends Controller with AuthorisedFunctions {

  implicit def hc(implicit request: Request[AnyContent]): HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

  //http://localhost:9900/marriage-allowance-application/abc
  // /marriage-allowance-application/do-you-want-to-apply
  // http://localhost:9948/mdtp/registration?origin=ma&confidenceLevel=100&completionURL=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fabc&failureURL=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fnot-authorised
//TODO check its not used (completionUrl on uplift) and delete
  def afterEligibility(): Action[AnyContent] = Action.async {
    implicit request =>

      authorised(ConfidenceLevel.L100).retrieve(Retrievals.credentials) {
        credentials =>
          Future.successful(Ok(s"You're in ${credentials.isDefined}"))
      }.recover {
        case _: NoActiveSession =>
          Redirect("http://localhost:9948/mdtp/registration?origin=ma&confidenceLevel=100&completionURL=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fafter-eligibility&failureURL=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fabc")
//          Ok("no session :(")
        case _: InsufficientConfidenceLevel =>
          Ok("nee confidence level mate")
      }
  }

  def eligibility() = Action.async {
    implicit request =>

      val isLoggedIn: Future[Boolean] = authorised() {
        Future.successful(true)
      }.recover {
        case _: NoActiveSession =>
          false
      }

      isLoggedIn.map {
        loggedIn =>

          Ok(s"logged in status is: $loggedIn")
      }
  }

  override val authConnector: AuthConnector =
    new PlayAuthConnector with ServicesConfig {

      override def http: CorePost = WSHttp

      override val serviceUrl: String = baseUrl("auth")

    }
}
