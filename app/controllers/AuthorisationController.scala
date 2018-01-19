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

package controllers

import actions.UnauthorisedActions
import config.ApplicationConfig
import connectors.ApplicationAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TamcBreadcrumb

object AuthorisationController extends AuthorisationController {
  override val logoutUrl = ApplicationConfig.logoutUrl
  override val logoutCallbackUrl = ApplicationConfig.logoutCallbackUrl
  override val auditConnector = ApplicationAuditConnector
}

trait AuthorisationController extends BaseController with UnauthorisedActions with TamcBreadcrumb {
  val logoutUrl: String
  val logoutCallbackUrl: String
  val auditConnector: AuditConnector

  def notAuthorised = unauthorisedAction {
    implicit request =>
      Ok(views.html.errors.other_ways())
  }

  def logout = unauthorisedAction {
    implicit request =>
      Redirect(logoutUrl).withSession("postLogoutPage" -> logoutCallbackUrl)
  }
  def sessionTimeout = unauthorisedAction {
    implicit request =>
      Ok(views.html.errors.session_timeout())
  }
}
