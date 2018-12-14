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

import com.google.inject.Inject
import config.{ApplicationConfig, TamcContext}
import play.api.i18n.MessagesApi

class AuthorisationController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         unauthenticatedAction: UnauthenticatedActionTransformer
                                       )(implicit tamcContext: TamcContext) extends BaseController {

  val logoutUrl: String = ApplicationConfig.logoutUrl
  val logoutCallbackUrl: String = ApplicationConfig.logoutCallbackUrl

  def notAuthorised = unauthenticatedAction {
    implicit request =>
      Ok(views.html.errors.other_ways())
  }

  def logout = unauthenticatedAction {
    implicit request =>
      Redirect(logoutUrl).withSession("postLogoutPage" -> logoutCallbackUrl)
  }
  def sessionTimeout = unauthenticatedAction {
    implicit request =>
      Ok(views.html.errors.session_timeout())
  }
}
