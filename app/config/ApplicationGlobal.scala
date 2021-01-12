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

package config

import com.typesafe.config.Config
import connectors.ApplicationAuditConnector
import net.ceedubs.ficus.Ficus.{configValueReader, toFicusConfig}
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.api.{Application, Configuration}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.config.{AppName, ControllerConfig}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport}
import play.api.i18n.Lang

object ApplicationGlobal extends DefaultFrontendGlobal {

  override val auditConnector = ApplicationAuditConnector
  override val loggingFilter = MarriageAllowanceLoggingFilter
  override val frontendAuditFilter = MarriageAllowanceAuditFilter
  implicit val templateRenderer = config.LocalTemplateRenderer
  implicit val formPartialRetriever = TamcFormPartialRetriever

  private def lang(implicit request: Request[_]): Lang =
    Lang(request.cookies.get("PLAY_LANG").fold("en")(_.value))

  override def onStart(app: Application) {
    super.onStart(app)
    new ApplicationCrypto(current.configuration.underlying).verifyConfiguration()
  }

  override def microserviceMetricsConfig(implicit app: Application) = app.configuration.getConfig("microservice.metrics")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.templates.error_template(pageTitle, heading, message)

  override def notFoundTemplate(implicit request: Request[_]): Html = {
    implicit val _: Lang = lang
    views.html.templates.page_not_found_template(config.ApplicationConfig, formPartialRetriever)
  }
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = current.configuration.underlying.as[Config]("controllers")
}

object MarriageAllowanceLoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MarriageAllowanceAuditFilter extends FrontendAuditFilter with AppName with MicroserviceFilterSupport {
  override lazy val maskedFormFields: Seq[String] = Seq.empty[String]
  override lazy val applicationPort: Option[Int] = None
  override lazy val auditConnector = ApplicationAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing

  override protected def appNameConfiguration: Configuration = current.configuration
}
