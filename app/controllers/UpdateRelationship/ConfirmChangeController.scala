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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UpdateRelationshipService
import utils.{UpdateRelationshipErrorHandler, LoggerHelper}
import viewModels.ConfirmUpdateViewModelImpl

import scala.concurrent.ExecutionContext

class ConfirmChangeController @Inject()(authenticate: StandardAuthJourney,
                                        updateRelationshipService: UpdateRelationshipService,
                                        cc: MessagesControllerComponents,
                                        confirmUpdateV: views.html.coc.confirmUpdate,
                                        confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl,
                                        errorHandler: UpdateRelationshipErrorHandler)
                                       (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def confirmUpdate: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      updateRelationshipService.getConfirmationUpdateAnswers map { confirmationUpdateAnswers =>
        Ok(confirmUpdateV(confirmUpdateViewModelImpl(confirmationUpdateAnswers)))
      } recover errorHandler.handleError
  }

  def submitConfirmUpdate: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      updateRelationshipService.updateRelationship(request.nino) map {
        _ => Redirect(controllers.UpdateRelationship.routes.FinishedChangeController.finishUpdate())
      } recover errorHandler.handleError
  }

}
