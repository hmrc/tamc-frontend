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

package utils

import controllers.BaseController
import errors._
import models.auth.BaseUserRequest
import org.apache.commons.lang3.exception.ExceptionUtils
import play.api.mvc.{MessagesControllerComponents, Result}
import play.twirl.api.Html

import javax.inject.Inject

class TransferErrorHandler @Inject()(
                                     cc: MessagesControllerComponents,
                                     transferorNotFound: views.html.errors.transferor_not_found,
                                     recipientNotFound: views.html.errors.recipient_not_found,
                                     transferorStatus: views.html.transferor_status,
                                     noYearSelected: views.html.errors.no_year_selected,
                                     noEligibleYears: views.html.errors.no_eligible_years,
                                     noTaxYearTransferor: views.html.errors.no_tax_year_transferor,
                                     relationshipCannotCreate: views.html.errors.relationship_cannot_create,
                                     recipientRelationshipExists: views.html.errors.recipient_relationship_exists,
                                     tryLater: views.html.errors.try_later) extends BaseController(cc) with LoggerHelper {

  def handleError(implicit request: BaseUserRequest[_]): PartialFunction[Throwable, Result] = {
    def message(throwable: Throwable): String  =
      s"An exception occurred during processing of URI [${request.uri}] reason [$throwable,${throwable.getMessage}] SID [${utils
        .getSid(request)}] stackTrace [${ExceptionUtils.getStackTrace(throwable)}]"

    def handle(throwable: Throwable, logger: String => Unit, result: Result): Result = {
      logger(message(throwable))
      result
    }

    def handleWithException(ex: Throwable, view: Html): Result = {
      error(ex.getMessage(), ex)
      InternalServerError(view)
    }

    val pf: PartialFunction[Throwable, Result] = {
      case t: TransferorNotFound => handle(t, warn, Ok(transferorNotFound()))
      case t: RecipientNotFound  => handle(t, warn, Ok(recipientNotFound()))
      case t: TransferorDeceased =>
        handle(t, warn, Redirect(controllers.transfer.routes.CannotUseServiceController.cannotUseService()))
      case t: RecipientDeceased =>
        handle(t, warn, Redirect(controllers.transfer.routes.CannotUseServiceController.cannotUseService()))
      case t: CacheMissingTransferor =>
        handle(t, warn, Redirect(controllers.UpdateRelationship.routes.HistoryController.history()))
      case t: CacheTransferorInRelationship => handle(t, warn, Ok(transferorStatus()))
      case t: CacheMissingRecipient =>
        handle(t, warn, Redirect(controllers.UpdateRelationship.routes.HistoryController.history()))
      case t: CacheMissingEmail =>
        handle(t, warn, Redirect(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail()))
      case t: CacheRelationshipAlreadyCreated =>
        handle(t, warn, Redirect(controllers.UpdateRelationship.routes.HistoryController.history()))
      case t: CacheCreateRequestNotSent =>
        handle(t, warn, Redirect(controllers.UpdateRelationship.routes.HistoryController.history()))
      case t: NoTaxYearsSelected      => handle(t, info, Ok(noYearSelected()))
      case t: NoTaxYearsAvailable     => handle(t, info, Ok(noEligibleYears()))
      case t: NoTaxYearsForTransferor => handle(t, info, Ok(noTaxYearTransferor()))
      case t: RelationshipMightBeCreated =>
        handle(t, warn, Redirect(controllers.UpdateRelationship.routes.HistoryController.history()))
      case ex: CannotCreateRelationship => handleWithException(ex, relationshipCannotCreate())
      case ex: CacheRecipientInRelationship =>
        handleWithException(ex, recipientRelationshipExists())
      case ex => handleWithException(ex, tryLater())
    }
    pf
  }
}
