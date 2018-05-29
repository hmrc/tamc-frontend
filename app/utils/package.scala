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

import java.net.URLEncoder

import play.api.mvc.Request
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.frontend.auth.AuthContext
import config.ApplicationConfig._

package object utils {

  def getUserNino(authContext: AuthContext): Nino =
    authContext.principal.accounts.paye.get.nino

  def encodeQueryStringValue(value: String): String =
    URLEncoder.encode(value, "UTF8")

  def normaliseNino(nino: String): String =
    nino.replaceAll(" ", "").toUpperCase()

  private def normalise(nino: Nino): Nino =
    Nino(normaliseNino(nino.nino))

  def areEqual(source: Nino, target: Nino): Boolean =
    normalise(source) == normalise(target)

  def getSid(request: Request[_]): String =
    request.session.get(SessionKeys.sessionId).getOrElse("")

  def isPtaJourneyUseOnlyOnAuthorisedPage(implicit request: Request[_]): Boolean =
    request.cookies.get(TAMC_JOURNEY).exists { _.value == TAMC_JOURNEY_PTA } || request.cookies.get(TAMC_JOURNEY).isEmpty

  def isScottishResident(request: Request[_]): Boolean =
    request.session.get(SCOTTISH_RESIDENT).map(_.toBoolean).fold(false)(identity)
}
