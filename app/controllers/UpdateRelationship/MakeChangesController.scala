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
import forms.coc.MakeChangesDecisionForm
import models._
import play.api.mvc._
import services.UpdateRelationshipService
import utils.LoggerHelper

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MakeChangesController @Inject()(authenticate: StandardAuthJourney,
                                      updateRelationshipService: UpdateRelationshipService,
                                      cc: MessagesControllerComponents,
                                      reasonForChange: views.html.coc.reason_for_change)
                                     (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def makeChange(): Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      updateRelationshipService.getMakeChangesDecision map { makeChangesData =>
        Ok(reasonForChange(MakeChangesDecisionForm.form().fill(makeChangesData)))
      } recover {
        case NonFatal(_) => Ok(reasonForChange(MakeChangesDecisionForm.form()))
      }
  }

  def submitMakeChange(): Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      MakeChangesDecisionForm.form().bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(reasonForChange(formWithErrors)))
        }, {
          case Some(MakeChangesDecisionForm.Divorce) =>
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Divorce) map { _ =>
              Redirect(controllers.UpdateRelationship.routes.DivorceController.divorceEnterYear())
            }

          case Some(MakeChangesDecisionForm.Cancel) =>
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Cancel) flatMap { _ =>
              noLongerWantMarriageAllowanceRedirect
            }

          case Some(MakeChangesDecisionForm.Bereavement) =>
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Bereavement) map { _ =>
              Redirect(controllers.UpdateRelationship.routes.BereavementController.bereavement())
            }

          case _ => Future.successful(Redirect(controllers.UpdateRelationship.routes.MakeChangesController.makeChange()))
        })
  }

  private def noLongerWantMarriageAllowanceRedirect(implicit request: Request[?]): Future[Result] = {
    updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
      if (relationshipRecords.primaryRecord.role == Recipient) {
        Redirect(controllers.UpdateRelationship.routes.StopAllowanceController.stopAllowance())
      } else {
        Redirect(controllers.UpdateRelationship.routes.StopAllowanceController.cancel())
      }
    }
  }

}
