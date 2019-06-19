/*
 * Copyright 2019 HM Revenue & Customs
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

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus.{configValueReader, toFicusConfig}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}

//object ApplicationGlobal {
//
//  val auditConnector: ApplicationAuditConnector.type = ApplicationAuditConnector
//  val loggingFilter: MarriageAllowanceLoggingFilter.type = MarriageAllowanceLoggingFilter
//  val frontendAuditFilter: MarriageAllowanceAuditFilter.type = MarriageAllowanceAuditFilter
//  val templateRenderer: LocalTemplateRenderer.type = config.LocalTemplateRenderer
//  val formPartialRetriever: TamcFormPartialRetriever.type = TamcFormPartialRetriever
//
//  def microserviceMetricsConfig(implicit app: Application) = app.configuration.getConfig("microservice.metrics")
//
//  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
//    views.html.templates.error_template(pageTitle, heading, message)
//}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs: Config = current.configuration.underlying.as[Config]("controllers")
}

//object MarriageAllowanceLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
//  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
//}

//object MarriageAllowanceAuditFilter extends FrontendAuditFilter with AppName with MicroserviceFilterSupport {
//  override lazy val maskedFormFields: Seq[String] = Seq.empty[String]
//  override lazy val applicationPort: Option[Int] = None
//  override lazy val auditConnector = ApplicationAuditConnector
//
//  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
//
//  override protected def appNameConfiguration: Configuration = current.configuration
//}
