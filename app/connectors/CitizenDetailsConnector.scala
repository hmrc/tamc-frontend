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

package connectors

import details.PersonDetails
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.config.ServicesConfig
import utils.WSHttp

import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet }

object CitizenDetailsConnector extends CitizenDetailsConnector with ServicesConfig {
  override def httpGet: HttpGet = WSHttp

  override def citizenDetailsUrl: String = baseUrl("citizen-details")
}

trait CitizenDetailsConnector {

  def httpGet: HttpGet
  def citizenDetailsUrl: String

  def citizenDetailsFromNino(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetails] =
    httpGet.GET[PersonDetails](s"$citizenDetailsUrl/citizen-details/$nino/designatory-details")

}
