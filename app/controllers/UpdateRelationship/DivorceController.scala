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
import forms.coc.DivorceSelectYearForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UpdateRelationshipService
import utils.{UpdateRelationshipErrorHandler, LoggerHelper}
import viewModels.DivorceEndExplanationViewModelImpl

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DivorceController @Inject()(authenticate: StandardAuthJourney,
                                  updateRelationshipService: UpdateRelationshipService,
                                  cc: MessagesControllerComponents,
                                  divorceSelectYearV: views.html.coc.divorce_select_year,
                                  divorceEndExplanationV: views.html.coc.divorce_end_explanation,
                                  divorceSelectYearForm: DivorceSelectYearForm,
                                  divorceEndExplanationViewModelImpl: DivorceEndExplanationViewModelImpl,
                                  errorHandler: UpdateRelationshipErrorHandler)
                                 (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def divorceEnterYear: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      updateRelationshipService.getDivorceDate map { optionalDivorceDate =>
        optionalDivorceDate.fold(Ok(divorceSelectYearV(divorceSelectYearForm.form))) { divorceDate =>
          Ok(divorceSelectYearV(divorceSelectYearForm.form.fill(divorceDate)))
        }
      } recover {
        case NonFatal(_) =>
          Ok(divorceSelectYearV(divorceSelectYearForm.form))
      }
  }

  def submitDivorceEnterYear: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      divorceSelectYearForm.form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(divorceSelectYearV(formWithErrors)))
        }, {
          divorceDate =>
            updateRelationshipService.saveDivorceDate(divorceDate) map { _ =>
              Redirect(controllers.UpdateRelationship.routes.DivorceController.divorceEndExplanation())
            }
        }
      ) recover errorHandler.handleError
  }

  def divorceEndExplanation: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      (for {
        (role, divorceDate) <- updateRelationshipService.getDataForDivorceExplanation
        datesForDivorce = updateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate)
        _ <- updateRelationshipService.saveMarriageAllowanceEndingDates(datesForDivorce)
      } yield {
        val viewModel = divorceEndExplanationViewModelImpl(role, divorceDate, datesForDivorce)
        Ok(divorceEndExplanationV(viewModel))
      }) recover errorHandler.handleError
  }

}
