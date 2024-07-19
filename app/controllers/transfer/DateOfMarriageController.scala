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
import forms.DateOfMarriageForm
import models._
import play.api.i18n.Lang
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService._
import services.{CachingService, TimeService, TransferService}
import utils.LoggerHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DateOfMarriageController @Inject()(
  errorHandlerHelper: ErrorHandlerHelper,
  authenticate: StandardAuthJourney,
  registrationService: TransferService,
  cachingService: CachingService,
  timeService: TimeService,
  cc: MessagesControllerComponents,
  dateOfMarriageV: views.html.date_of_marriage,
  dateOfMarriageForm: DateOfMarriageForm)(implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def dateOfMarriage: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    Ok(dateOfMarriageV(marriageForm = dateOfMarriageForm.dateOfMarriageForm(today = timeService.getCurrentDate)))
  }

  def dateOfMarriageWithCy: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails {
    Redirect(controllers.transfer.routes.DateOfMarriageController.dateOfMarriage()).withLang(Lang("cy"))
  }

  def dateOfMarriageWithEn: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails {
    Redirect(controllers.transfer.routes.DateOfMarriageController.dateOfMarriage()).withLang(Lang("en"))
  }

  def dateOfMarriageAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async{ implicit request =>
    dateOfMarriageForm.dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(dateOfMarriageV(formWithErrors))),
      marriageData => {
        cachingService.put[DateOfMarriageFormInput](CACHE_MARRIAGE_DATE, marriageData)

        registrationService.getRecipientDetailsFormData() flatMap {
          case RecipientDetailsFormInput(name, lastName, gender, nino) =>
            val dataToSend = new RegistrationFormInput(name, lastName, gender, nino, marriageData.dateOfMarriage)
            registrationService.isRecipientEligible(request.nino, dataToSend) map { _ =>
              Redirect(controllers.routes.TransferController.eligibleYears())
            }
        }
      }
    ) recover errorHandlerHelper.handleError
  }
}
