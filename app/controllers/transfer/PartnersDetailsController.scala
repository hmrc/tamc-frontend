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

package controllers.transfer

import controllers.BaseController
import controllers.auth.StandardAuthJourney
import forms.RecipientDetailsForm
import models.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.*
import services.{CachingService, TimeService}
import utils.LoggerHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartnersDetailsController @Inject()(
                                           authenticate: StandardAuthJourney,
                                           cachingService: CachingService,
                                           timeService: TimeService,
                                           cc: MessagesControllerComponents,
                                           partnersDetailsView: views.html.multiyear.transfer.partners_details,
                                           recipientDetailsForm: RecipientDetailsForm
                                            )(implicit ec: ExecutionContext)
  extends BaseController(cc)
    with LoggerHelper {

  def transfer: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    val form = recipientDetailsForm.recipientDetailsForm(timeService.getCurrentDate, request.nino)
    cachingService.get(CACHE_RECIPIENT_DETAILS).map {
      case Some(savedData) =>
        Ok(partnersDetailsView(form.fill(savedData)))
      case None =>
        Ok(partnersDetailsView(form))
    }
  }

  def transferAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    recipientDetailsForm
      .recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(partnersDetailsView(formWithErrors))),
        recipientData =>
          cachingService.put(CACHE_RECIPIENT_DETAILS, recipientData).map { _ =>
            Redirect(controllers.transfer.routes.DateOfMarriageController.dateOfMarriage())
          }
      )
  }
}