/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.updateRelationship

import com.google.inject.Inject
import controllers.BaseController
import controllers.actions.AuthenticatedActionRefiner
import forms.ChangeRelationshipForm.{changeRelationshipForm, updateRelationshipForm}
import models.{ChangeRelationship, EndReasonCode, EndRelationshipReason}
import play.Logger
import forms.EmptyForm
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent}
import services._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer


import scala.concurrent.Future

class ChangeOfCircsController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      authenticate: AuthenticatedActionRefiner,
                                      updateRelationshipService: UpdateRelationshipService,
                                      listRelationshipService: ListRelationshipService,
                                      registrationService: TransferService,
                                      cachingService: CachingService,
                                      timeService: TimeService
                                    )(implicit templateRenderer: TemplateRenderer,
                                      formPartialRetriever: FormPartialRetriever) extends BaseController {

  def updateRelationshipAction(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipForm.bindFromRequest.fold(
        formWithErrors => Future {
          Logger.warn("unexpected error in updateRelationshipAction()")
          val form = formWithErrors.fill(ChangeRelationship(formWithErrors.data.get("role"), None, Some(formWithErrors.data.get("historicActiveRecord").forall(_.equals("true")))))
          BadRequest(views.html.coc.reason_for_change(form))
        },
        formData => {
          cachingService.saveRoleRecord(formData.role.get).flatMap { _ =>
            (formData.endReason, formData.role) match {
              case (Some(EndReasonCode.CANCEL), _) => Future.successful {
                Redirect(controllers.routes.UpdateRelationshipController.confirmCancel())
              }
              case (Some(EndReasonCode.REJECT), _) =>
                updateRelationshipService.
                  saveEndRelationshipReason(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = formData.creationTimestamp)) map {
                  _ => Redirect(controllers.routes.UpdateRelationshipController.confirmReject())
                }
              case (Some(EndReasonCode.DIVORCE), _) => Future.successful {
                Ok(views.html.coc.divorce_select_year(changeRelationshipForm.fill(formData)))
              }
              case (Some(EndReasonCode.EARNINGS), _) => Future.successful {
                Ok(views.html.coc.change_in_earnings_recipient())
              }
              case (Some(EndReasonCode.BEREAVEMENT), _) => Future.successful {
                Ok(views.html.coc.bereavement_recipient())
              }
              case (None, _) =>
                throw new Exception("Missing EndReasonCode")
              case _ => Future.successful {
                BadRequest(views.html.coc.reason_for_change(updateRelationshipForm.fill(formData)))
              }
            }
          }
        }
      )
  }

  def changeOfIncome: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.coc.change_in_earnings())
  }

  def bereavement: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.coc.bereavement_transferor())
  }
}
