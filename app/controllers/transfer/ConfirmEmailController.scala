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

package controllers.transfer

import controllers.BaseController
import controllers.auth.StandardAuthJourney
import forms.EmailForm.emailForm
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.CACHE_NOTIFICATION_RECORD
import services.{CachingService, TransferService}
import utils.{LoggerHelper, TransferErrorHandler}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmEmailController @Inject()(
                                        errorHandler: TransferErrorHandler,
                                        authenticate: StandardAuthJourney,
                                        registrationService: TransferService,
                                        cachingService: CachingService,
                                        cc: MessagesControllerComponents,
                                        email: views.html.multiyear.transfer.email)
                                      (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def confirmYourEmail: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    cachingService.get[NotificationRecord](CACHE_NOTIFICATION_RECORD) map {
      case Some(NotificationRecord(transferorEmail)) =>
        Ok(email(emailForm.fill(transferorEmail)))
      case None => Ok(email(emailForm))
    }
  }

  def confirmYourEmailAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    emailForm.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(email(formWithErrors))),
      transferorEmail =>
        registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map { _ =>
          Redirect(controllers.transfer.routes.ConfirmController.confirm())
        }
    ) recover errorHandler.handleError
  }
}
