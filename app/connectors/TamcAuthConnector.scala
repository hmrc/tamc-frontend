/*
 * Copyright 2021 HM Revenue & Customs
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

import com.google.inject.Inject
import config.HttpClient
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.play.config.ServicesConfig

class TamcAuthConnector @Inject()(
                                   override val runModeConfiguration: Configuration,
                                   val http: HttpClient,
                                   environment: Environment
                                 ) extends PlayAuthConnector with ServicesConfig {

  override val serviceUrl: String = baseUrl("auth")

  override protected def mode: Mode = environment.mode
}
