/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class LocalTemplateRenderer @Inject()(httpClient: HttpClient, appConfig: ApplicationConfig)(implicit ec: ExecutionContext) extends TemplateRenderer {

  override lazy val templateServiceBaseUrl: String = appConfig.templateServiceURL
  override val refreshAfter: Duration = appConfig.templateRefreshAfter

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fetchTemplate(path: String): Future[String] = {
    httpClient.GET(path).map(_.body)
  }

}
