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

package controllers.transfer

import errors._
import models.auth.UserRequest
import org.apache.commons.lang3.exception.ExceptionUtils
import play.Logger
import play.api.i18n.Messages
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Results._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer


object HandleError {
  def apply(implicit hc: HeaderCarrier, request: UserRequest[_], messages: Messages, templateRenderer: TemplateRenderer, formPartialRetriever: FormPartialRetriever): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>
        val message: String = s"An exception occurred during processing of URI [${request.uri}] reason [$throwable,${throwable.getMessage}] SID [${utils.getSid(request)}] stackTrace [${ExceptionUtils.getStackTrace(throwable)}]"

        def handle(logger: String => Unit, result: Result): Result = {
          logger(message)
          result
        }

        throwable match {
          case _: TransferorNotFound               => handle(Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: RecipientNotFound                => handle(Logger.warn, InternalServerError(views.html.errors.recipient_not_found()))
          case _: TransferorDeceased               => handle(Logger.warn, Redirect(controllers.routes.TransferController.cannotUseService()))
          case _: RecipientDeceased                => handle(Logger.warn, Redirect(controllers.routes.TransferController.cannotUseService()))
          case _: CacheMissingTransferor           => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheTransferorInRelationship    => handle(Logger.warn, Ok(views.html.transferor_status()))
          case _: CacheMissingRecipient            => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheRecipientInRelationship     => handle(Logger.warn, InternalServerError(views.html.errors.recipient_relationship_exists()))
          case _: CacheMissingEmail                => handle(Logger.warn, Redirect(controllers.routes.TransferController.confirmYourEmail()))
          case _: CannotCreateRelationship         => handle(Logger.warn, InternalServerError(views.html.errors.relationship_cannot_create()))
          case _: CacheRelationshipAlreadyCreated  => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheCreateRequestNotSent        => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: NoTaxYearsSelected               => handle(Logger.info, Ok(views.html.errors.no_year_selected()))
          case _: NoTaxYearsAvailable              => handle(Logger.info, Ok(views.html.errors.no_eligible_years()))
          case _: NoTaxYearsForTransferor          => handle(Logger.info, InternalServerError(views.html.errors.no_tax_year_transferor()))
          case _: RelationshipMightBeCreated       => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _                                   => handle(Logger.error, InternalServerError(views.html.errors.try_later()))
        }
    }
}
