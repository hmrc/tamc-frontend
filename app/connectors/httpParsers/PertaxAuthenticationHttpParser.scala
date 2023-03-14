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

package uk.gov.hmrc.nisp.connectors.httpParsers

import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.{HttpReads, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.nisp.models.pertaxAuth.PertaxAuthResponseModel

object PertaxAuthenticationHttpParser extends Logging {
  type PertaxAuthenticationResponse = Either[UpstreamErrorResponse, PertaxAuthResponseModel]

  implicit object PertaxAuthenticationHttpReads extends HttpReads[PertaxAuthenticationResponse] {
    override def read(method: String, url: String, response: HttpResponse): PertaxAuthenticationResponse = {
      response.json.validate[PertaxAuthResponseModel].fold(
        jsErrors => {
          logger.error(
            "[PertaxAuthenticationHttpParser][read] There was an issue processing the JSON returned from Pertax Auth. Check that the versions match." +
            s"\nAssociated Json errors:\n$jsErrors"
          )
          Left(UpstreamErrorResponse.apply(
            "[PertaxAuthenticationHttpParser][read] There was an issue parsing the response from Pertax Auth.",
            INTERNAL_SERVER_ERROR
          ))
        },
        Right(_)
      )
    }
  }

}
