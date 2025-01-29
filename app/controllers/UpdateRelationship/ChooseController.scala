/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.coc.CheckClaimOrCancelDecisionForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UpdateRelationshipService
import utils.LoggerHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ChooseController @Inject()(authenticate: StandardAuthJourney,
                                 updateRelationshipService: UpdateRelationshipService,
                                 cc: MessagesControllerComponents,
                                 decisionV: views.html.coc.decision)
                                (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {


  def decision: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      updateRelationshipService.getCheckClaimOrCancelDecision map { claimOrCancelDecision =>
        Ok(decisionV(CheckClaimOrCancelDecisionForm.form().fill(claimOrCancelDecision)))
      } recover {
        case NonFatal(_) => Ok(decisionV(CheckClaimOrCancelDecisionForm.form()))
      }
  }

  def submitDecision: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>

      CheckClaimOrCancelDecisionForm.form().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(decisionV(formWithErrors)))
        }, {
          case Some(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim) =>
            updateRelationshipService.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim) map { _ =>
              Redirect(controllers.UpdateRelationship.routes.ClaimsController.claims())
            }
          case Some(CheckClaimOrCancelDecisionForm.StopMarriageAllowance) =>
            updateRelationshipService.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.StopMarriageAllowance) map { _ =>
              Redirect(controllers.UpdateRelationship.routes.MakeChangesController.makeChange())
            }
          case _ =>
            Future.successful(Redirect(controllers.UpdateRelationship.routes.ChooseController.decision()))
        })
  }

}
