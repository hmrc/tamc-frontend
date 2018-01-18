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

package details

import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts

class NoNinoException extends RuntimeException

case class TamcUser(authContext: AuthContext, personDetails: Option[PersonDetails] = None) {

  private def extractNino: Option[Nino] = {
    authContext.principal.accounts.paye.map { _.nino }
  }

  def nino = extractNino.getOrElse(throw new NoNinoException)
  def name: Option[String] = personDetails match {
    case Some(personDetails) => personDetails.person.shortName
    case _                   => authContext.principal.name
  }
}

object TamcUser {
  implicit def implicitUser2TamcUser(implicit authContext: AuthContext, personDetails: PersonDetails): TamcUser = TamcUser(authContext, Some(personDetails))
  implicit def implicitUser2Option(implicit user: TamcUser): Option[TamcUser] = Some(user)
}
