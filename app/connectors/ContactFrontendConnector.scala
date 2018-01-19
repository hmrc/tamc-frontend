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

import config.WSHttp
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.http.{ BadGatewayException, HeaderCarrier, HttpGet }

trait ContactFrontendConnector extends ServicesConfig {

  import scala.concurrent.ExecutionContext.Implicits.global

  val http: HttpGet = WSHttp
  lazy val serviceBase = s"${baseUrl("contact-frontend")}/contact"

  def getHelpPartial(implicit hc: HeaderCarrier): Future[String] = {

    val url = s"$serviceBase/problem_reports"

    http.GET(url) map { r =>
      r.body
    } recover {
      case e: BadGatewayException =>
        // $COVERAGE-OFF$
        Logger.error(s"[ContactFrontendConnector] ${e.message}", e)
        ""
        // $COVERAGE-ON
    }
  }

}

object ContactFrontendConnector extends ContactFrontendConnector {}
