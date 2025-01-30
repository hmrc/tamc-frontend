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

package controllers.UpdateRelationship

import com.google.inject.Inject
import controllers.BaseController
import controllers.auth.StandardAuthJourney
import forms.EmailForm.emailForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UpdateRelationshipService
import utils.{UpdateRelationshipErrorHandler, LoggerHelper}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ConfirmEmailController @Inject()(authenticate: StandardAuthJourney,
                                       updateRelationshipService: UpdateRelationshipService,
                                       cc: MessagesControllerComponents,
                                       emailV: views.html.coc.email,
                                       errorHandler: UpdateRelationshipErrorHandler)
                                      (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def confirmEmail: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      lazy val emptyEmailView = emailV(emailForm)
      updateRelationshipService.getEmailAddress map {
        case Some(email) => Ok(emailV(emailForm.fill(email)))
        case None => Ok(emptyEmailView)
      } recover {
        case NonFatal(_) => Ok(emptyEmailView)
      }
  }

  def confirmYourEmailActionUpdate: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      emailForm.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(emailV(formWithErrors)))
        },
        email =>
          updateRelationshipService.saveEmailAddress(email) map {
            _ => Redirect(controllers.UpdateRelationship.routes.ConfirmChangeController.confirmUpdate())
          }
      ) recover errorHandler.handleError
  }

}
