/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

class HowItWorksController @Inject()(
                                      appConfig: ApplicationConfig,
                                      unauthenticatedAction: UnauthenticatedActionTransformer,
                                      howItWorksView: views.html.multiyear.how_it_works,
                                      cc: MessagesControllerComponents) extends BaseController(cc) {

  def home: Action[AnyContent] = unauthenticatedAction {
    Redirect(controllers.routes.HowItWorksController.howItWorks())
  }

  def howItWorks: Action[AnyContent] = unauthenticatedAction {
    implicit request => request.headers.get("Referer") match {
      case Some(referrer) if referrer.contains(appConfig.gdsStartUrl) => Redirect (controllers.transfer.routes.PartnersDetailsController.transfer())
      case Some(referrer) if referrer.contains(appConfig.gdsContinueUrl) => Redirect (controllers.transfer.routes.PartnersDetailsController.transfer())
      case _ => Ok(howItWorksView())
    }
  }
}
