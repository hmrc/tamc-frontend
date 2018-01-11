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

import java.io.File

import com.typesafe.config.Config
import connectors.ApplicationAuditConnector
import net.ceedubs.ficus.Ficus.configValueReader
import net.ceedubs.ficus.Ficus.toFicusConfig
import play.api.Application
import play.api.Configuration
import play.api.Mode.Mode
import play.api.Play
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.config.ControllerConfig
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{ FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport }

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = ApplicationAuditConnector
  override val loggingFilter = MarriageAllowanceLoggingFilter
  override val frontendAuditFilter = MarriageAllowanceAuditFilter
  implicit val templateRenderer = config.LocalTemplateRenderer
  implicit val formPartialRetriever = TamcFormPartialRetriever

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def microserviceMetricsConfig(implicit app: Application) = app.configuration.getConfig("microservice.metrics")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.templates.error_template(pageTitle, heading, message)
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object MarriageAllowanceLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MarriageAllowanceAuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {
  override lazy val maskedFormFields: Seq[String] = Seq.empty[String]
  override lazy val applicationPort: Option[Int] = None
  override lazy val auditConnector = ApplicationAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}
