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

package controllers

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.actions.UnauthenticatedActionTransformer
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class AuthorisationController @Inject()(
  unauthenticatedAction: UnauthenticatedActionTransformer,
  appConfig: ApplicationConfig,
  cc: MessagesControllerComponents,
  otherWaysView: views.html.errors.other_ways,
  sessionTimeoutView: views.html.errors.session_timeout)(implicit templateRenderer: TemplateRenderer, formPartialRetriever: FormPartialRetriever) extends BaseController(cc) {

  val logoutUrl: String = appConfig.logoutUrl
  val logoutCallbackUrl: String = appConfig.logoutCallbackUrl

  def notAuthorised = unauthenticatedAction {
    implicit request =>
      Ok(otherWaysView())
  }

  def logout = unauthenticatedAction {
    implicit request =>
      Redirect(logoutUrl).withSession("postLogoutPage" -> logoutCallbackUrl)
  }

  def sessionTimeout = unauthenticatedAction {
    implicit request =>
      Ok(sessionTimeoutView())
  }
}
