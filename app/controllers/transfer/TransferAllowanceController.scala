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
import services.{CachingService, TimeService, TransferService}
import utils.{LoggerHelper, TransferErrorHandler}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransferAllowanceController @Inject() (
                                              errorHandler: TransferErrorHandler,
                                              authenticate: StandardAuthJourney,
                                              cachingService: CachingService,
                                              registrationService: TransferService,
                                              timeService: TimeService,
                                              cc: MessagesControllerComponents,
                                              transferV: views.html.multiyear.transfer.transfer,
                                              recipientDetailsForm: RecipientDetailsForm
                                            )(implicit ec: ExecutionContext)
  extends BaseController(cc)
    with LoggerHelper {

  def transfer: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    Ok(
      transferV(recipientDetailsForm.recipientDetailsForm(timeService.getCurrentDate, request.nino))
    )
  }

  def transferAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    recipientDetailsForm
      .recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(transferV(formWithErrors))),
        recipientData =>
          for {
            _ <- cachingService.put[RecipientDetailsFormInput](CACHE_RECIPIENT_DETAILS, recipientData)
            marriage <- cachingService.get[DateOfMarriageFormInput](CACHE_MARRIAGE_DATE)
            recipient <- registrationService.getRecipientDetailsFormData()

            dataToSend = new RegistrationFormInput(
              recipient.name,
              recipient.lastName,
              recipient.gender,
              recipient.nino,
              marriage.get.dateOfMarriage
            )
            _ <- registrationService.isRecipientEligible(request.nino, dataToSend)
          } yield {
            Redirect(controllers.transfer.routes.EligibleYearsController.eligibleYears())
          }
      ) recover errorHandler.handleError
  }
}
