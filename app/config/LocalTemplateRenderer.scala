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

package config

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future
import scala.concurrent.duration._
import uk.gov.hmrc.http.HeaderCarrier

object LocalTemplateRenderer extends TemplateRenderer with ServicesConfig {
  override lazy val templateServiceBaseUrl = baseUrl("frontend-template-provider")
  override val refreshAfter: Duration = 10 minutes

  private implicit val hc = HeaderCarrier()
  import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

  override def fetchTemplate(path: String): Future[String] =  {
    WSHttp.GET(path).map(_.body)
  }
}
