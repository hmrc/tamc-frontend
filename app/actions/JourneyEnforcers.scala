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

package actions

import config.{TamcContextImpl, ApplicationConfig}
import play.api.mvc.Cookie
import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.play.frontend.auth.AuthContext

trait JourneyEnforcers {
  implicit val context: config.TamcContext = TamcContextImpl

  def setGdsAwarePtaJourney(request: Request[_], response: Result): Result =
    request.cookies.get(ApplicationConfig.TAMC_JOURNEY) match {
      case Some(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_GDS, _, _, _, _, _)) =>
        response.withCookies(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_GDS))
      case _ =>
        response.withCookies(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_PTA))
    }

  def setPtaAwareGdsJourney(request: Request[_], response: Result): Result =
    request.cookies.get(ApplicationConfig.TAMC_JOURNEY) match {
      case Some(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_PTA, _, _, _, _, _)) =>
        response.withCookies(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_PTA))
      case _ =>
        response.withCookies(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_GDS))
    }

  def checkGdsAwarePtaJourney(hasSeenJourney: Result, hasNotSeenJourney: Result)(implicit request: Request[_], user: AuthContext): Result =
    isGdsOrPtaJourney(request) match {
      case true  => hasSeenJourney
      case false => hasNotSeenJourney
    }

  def isGdsOrPtaJourney(request: Request[_]): Boolean =
    request.cookies.get(ApplicationConfig.TAMC_JOURNEY) match {
      case Some(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_PTA, _, _, _, _, _)) =>
        true
      case Some(Cookie(ApplicationConfig.TAMC_JOURNEY, ApplicationConfig.TAMC_JOURNEY_GDS, _, _, _, _, _)) =>
        true
      case _ =>
        false
    }

  def getJourneyName()(implicit request: Request[_]): String =
    if (isPtaJourney) {
      ApplicationConfig.TAMC_JOURNEY_PTA
    } else {
      ApplicationConfig.TAMC_JOURNEY_GDS
    }

  def isPtaJourney(implicit request: Request[_]): Boolean =
    request.cookies.get(ApplicationConfig.TAMC_JOURNEY).exists { _.value == ApplicationConfig.TAMC_JOURNEY_PTA }

  def isGdsJourney(implicit request: Request[_]): Boolean =
    request.cookies.get(ApplicationConfig.TAMC_JOURNEY).exists { _.value == ApplicationConfig.TAMC_JOURNEY_GDS }
}
