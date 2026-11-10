/*
 * Copyright 2026 HM Revenue & Customs
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
import errors.CacheMapNoFound
import forms.coc.MakeChangesDecisionForm
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UpdateRelationshipService
import utils.{LoggerHelper, UpdateRelationshipErrorHandler}
import views.helpers.LanguageUtilsImpl
import views.html.coc.cancellationSubmitted

import scala.concurrent.{ExecutionContext, Future}

class FinishedChangeController @Inject() (
  authenticate: StandardAuthJourney,
  updateRelationshipService: UpdateRelationshipService,
  cc: MessagesControllerComponents,
  finished: views.html.coc.finished,
  cancellationSubmitted: views.html.coc.cancellationSubmitted,
  languageUtilsImpl: LanguageUtilsImpl,
  errorHandler: UpdateRelationshipErrorHandler
)(implicit ec: ExecutionContext)
    extends BaseController(cc)
    with LoggerHelper {

  def finishUpdate: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    updateRelationshipService.removeCache
      .map(_ => Ok(finished()))
      .recover(errorHandler.handleError)
  }

  def finishIncomeChange: Action[AnyContent] = finishChange(MakeChangesDecisionForm.IncomeChange)

  def finishDivorceChange: Action[AnyContent] = finishChange(MakeChangesDecisionForm.Divorce)

  private def finishChange(cancellationReason: String): Action[AnyContent] =
    authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
      updateRelationshipService.getCancellationConfirmation
        .flatMap {
          case Some(confirmation) if confirmation.reason == cancellationReason =>
            val endDate = languageUtilsImpl().ukDateTransformer(confirmation.marriageAllowanceEndDate)
            updateRelationshipService.removeCache
              .map(_ => Ok(cancellationSubmitted(endDate)))

          case _ => Future.failed(CacheMapNoFound())
        }
        .recover(errorHandler.handleError)
    }

}
