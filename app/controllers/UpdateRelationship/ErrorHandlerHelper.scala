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
import config.ApplicationConfig
import controllers.auth.StandardAuthJourney
import errors.{BadFetchRequest, CacheMissingUpdateRecord, CacheRelationshipAlreadyUpdated, CacheUpdateRequestNotSent, CannotUpdateRelationship, CitizenNotFound, MultipleActiveRecordError, NoPrimaryRecordError, RecipientNotFound, TransferorNotFound}
import forms.coc.DivorceSelectYearForm
import models.auth.BaseUserRequest
import play.api.Configuration
import play.api.mvc.{MessagesControllerComponents, Result}
import services.UpdateRelationshipService
import viewModels.{ClaimsViewModelImpl, ConfirmUpdateViewModelImpl, DivorceEndExplanationViewModelImpl, HistorySummaryViewModelImpl}

import scala.concurrent.ExecutionContext

class ErrorHandlerHelper @Inject()(
                                    config: Configuration,
                                    authenticate: StandardAuthJourney,
                                    updateRelationshipService: UpdateRelationshipService,
                                    cc: MessagesControllerComponents,
                                    historySummary: views.html.coc.history_summary,
                                    decisionV: views.html.coc.decision,
                                    claimsV: views.html.coc.claims,
                                    reasonForChange: views.html.coc.reason_for_change,
                                    stopAllowanceV: views.html.coc.stopAllowance,
                                    cancelV: views.html.coc.cancel,
                                    confirmUpdateV: views.html.coc.confirmUpdate,
                                    bereavementV: views.html.coc.bereavement,
                                    divorceSelectYearV: views.html.coc.divorce_select_year,
                                    divorceEndExplanationV: views.html.coc.divorce_end_explanation,
                                    emailV: views.html.coc.email,
                                    finished: views.html.coc.finished,
                                    tryLater: views.html.errors.try_later,
                                    citizenNotFound: views.html.errors.citizen_not_found,
                                    transferorNotFound: views.html.errors.transferor_not_found,
                                    recipientNotFound: views.html.errors.recipient_not_found,
                                    divorceSelectYearForm: DivorceSelectYearForm,
                                    historySummaryViewModelImpl: HistorySummaryViewModelImpl,
                                    claimsViewModelImpl: ClaimsViewModelImpl,
                                    divorceEndExplanationViewModelImpl: DivorceEndExplanationViewModelImpl,
                                    confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl,
                                    appConfig: ApplicationConfig)(implicit ec: ExecutionContext) {

  def handleError(implicit request: BaseUserRequest[_]): PartialFunction[Throwable, Result] = {
    val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

    def handle(throwable: Throwable, logger: (String, Throwable) => Unit, result: Result): Result = {
      logger(message, throwable)
      result
    }

    val pf: PartialFunction[Throwable, Result] = {
      case _: NoPrimaryRecordError => request.headers.get("Referer") match {
        case Some(referrer) if referrer.contains(appConfig.gdsStartUrl)    => Redirect(controllers.routes.TransferController.transfer())
        case Some(referrer) if referrer.contains(appConfig.gdsContinueUrl) => Redirect(controllers.routes.TransferController.transfer())
        case _                                                             => Redirect(controllers.routes.HowItWorksController.howItWorks())
      }
      case t: CacheRelationshipAlreadyUpdated => handle(t, warn, Redirect(controllers.routes.UpdateRelationshipController.finishUpdate()))
      case t: CacheMissingUpdateRecord => handle(t, warn, InternalServerError(tryLater()))
      case t: CacheUpdateRequestNotSent => handle(t, warn, InternalServerError(tryLater()))
      case t: CannotUpdateRelationship => handle(t, warn, InternalServerError(tryLater()))
      case t: MultipleActiveRecordError => handle(t, warn, InternalServerError(tryLater()))
      case t: CitizenNotFound => handle(t, warn, InternalServerError(citizenNotFound()))
      case t: BadFetchRequest => handle(t, warn, InternalServerError(tryLater()))
      case t: TransferorNotFound => handle(t, warn, Ok(transferorNotFound()))
      case t: RecipientNotFound => handle(t, warn, Ok(recipientNotFound()))
      case t => handle(t, error, InternalServerError(tryLater()))
    }
    pf
  }


}
