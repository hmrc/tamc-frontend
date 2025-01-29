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

import scala.concurrent.{ExecutionContext, Future}

class StopAllowanceController @Inject()(authenticate: StandardAuthJourney,
                                        updateRelationshipService: UpdateRelationshipService,
                                        cc: MessagesControllerComponents,
                                        stopAllowanceV: views.html.coc.stopAllowance,
                                        cancelV: views.html.coc.cancel,
                                        errorHandler: UpdateRelationshipErrorHandler)
                                       (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def stopAllowance: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      Future.successful(Ok(stopAllowanceV()))
  }

  def cancel: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      val cancelDates = updateRelationshipService.getMAEndingDatesForCancellation
      updateRelationshipService.saveMarriageAllowanceEndingDates(cancelDates) map { _ =>
        Ok(cancelV(cancelDates))
      } recover errorHandler.handleError
  }

}
