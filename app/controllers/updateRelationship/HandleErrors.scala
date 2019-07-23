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

import errors.{
  BadFetchRequest, CacheMissingUpdateRecord, CacheRelationshipAlreadyUpdated, CacheUpdateRequestNotSent,
  CannotUpdateRelationship, CitizenNotFound, RecipientNotFound, TransferorNotFound
}
import models.auth.UserRequest
import play.Logger
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

object HandleErrors {

  def handleError(
    implicit hc: HeaderCarrier, request: UserRequest[_], templateRenderer: TemplateRenderer, messages: Messages, formPartialRetriever: FormPartialRetriever
  ): PartialFunction[Throwable, Result] = PartialFunction[Throwable, Result] { throwable: Throwable =>

    val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

    def handle(logger: (String, Throwable) => Unit, result: Result): Result = {
      logger(message, throwable)
      result
    }

    throwable match {
      case _: CacheRelationshipAlreadyUpdated => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.finishUpdate()))
      case _: CacheMissingUpdateRecord => handle(Logger.warn, InternalServerError(views.html.errors.try_later()))
      case _: CacheUpdateRequestNotSent => handle(Logger.warn, InternalServerError(views.html.errors.try_later()))
      case _: CannotUpdateRelationship => handle(Logger.warn, InternalServerError(views.html.errors.try_later()))
      case _: CitizenNotFound => handle(Logger.warn, InternalServerError(views.html.errors.citizen_not_found()))
      case _: BadFetchRequest => handle(Logger.warn, InternalServerError(views.html.errors.bad_request()))
      case _: TransferorNotFound => handle(Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
      case _: RecipientNotFound => handle(Logger.warn, InternalServerError(views.html.errors.recipient_not_found()))
      case _ => handle(Logger.error, InternalServerError(views.html.errors.try_later()))
    }
  }
}

