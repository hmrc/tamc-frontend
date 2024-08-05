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

package utils

import com.google.inject.Inject
import config.ApplicationConfig
import controllers.BaseController
import errors._
import models.auth.BaseUserRequest
import play.api.mvc.{MessagesControllerComponents, Result}

class UpdateRelationshipErrorHandler @Inject()(cc: MessagesControllerComponents,
                                               tryLater: views.html.errors.try_later,
                                               citizenNotFound: views.html.errors.citizen_not_found,
                                               transferorNotFound: views.html.errors.transferor_not_found,
                                               recipientNotFound: views.html.errors.recipient_not_found,
                                               appConfig: ApplicationConfig) extends BaseController(cc) with LoggerHelper {

  def handleError(implicit request: BaseUserRequest[_]): PartialFunction[Throwable, Result] = {
    val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

    def handle(throwable: Throwable, logger: (String, Throwable) => Unit, result: Result): Result = {
      logger(message, throwable)
      result
    }

    val pf: PartialFunction[Throwable, Result] = {
      case _: NoPrimaryRecordError => request.headers.get("Referer") match {
        case Some(referrer) if referrer.contains(appConfig.gdsStartUrl) => Redirect(controllers.routes.TransferController.transfer())
        case Some(referrer) if referrer.contains(appConfig.gdsContinueUrl) => Redirect(controllers.routes.TransferController.transfer())
        case _ => Redirect(controllers.routes.HowItWorksController.howItWorks())
      }
      case t: CacheRelationshipAlreadyUpdated => handle(t, warn, Redirect(controllers.UpdateRelationship.routes.FinishedChangeController.finishUpdate()))
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
