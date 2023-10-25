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

package connectors

import com.google.inject.ImplementedBy
import config.ApplicationConfig
import play.api.Logging
import play.api.http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, UpstreamErrorResponse}
import uk.gov.hmrc.play.partials.HtmlPartial
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}
import connectors.httpParsers.PertaxAuthenticationHttpParser._
import models.pertaxAuth.PertaxAuthResponseModel

class PertaxAuthConnectorImpl @Inject()(http: HttpClient, appConfig: ApplicationConfig)(
                                   implicit ec: ExecutionContext
) extends PertaxAuthConnector with Logging {

  override def authorise()(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]] = {
    val authUrl = appConfig.pertaxAuthBaseUrl + s"/pertax/authorise"

    http.POSTEmpty[Either[UpstreamErrorResponse, PertaxAuthResponseModel]](
      url = authUrl,
      headers = Seq(HeaderNames.ACCEPT -> "application/vnd.hmrc.2.0+json")
    )
  }

  override def loadPartial(partialContextUrl: String)(implicit hc: HeaderCarrier): Future[HtmlPartial] = {
    val partialUrl = appConfig.pertaxAuthBaseUrl + s"/pertax/partials/$partialContextUrl"

    http.GET[HtmlPartial](partialUrl).map {
      case partialSuccess: HtmlPartial.Success => partialSuccess
      case partialFailure: HtmlPartial.Failure =>
        logger.error(s"[PertaxAuthConnector][loadPartial] Failed to load Partial from partial url '$partialUrl'. " +
          s"Partial info: $partialFailure")
        partialFailure
    }.recover {
      case exception: HttpException => HtmlPartial.Failure(Some(exception.responseCode))
      case _ => HtmlPartial.Failure(None)
    }
  }

}

@ImplementedBy(classOf[PertaxAuthConnectorImpl])
trait PertaxAuthConnector {
  def authorise()(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]

  def loadPartial(partialContextUrl: String)(implicit hc: HeaderCarrier): Future[HtmlPartial]
}
