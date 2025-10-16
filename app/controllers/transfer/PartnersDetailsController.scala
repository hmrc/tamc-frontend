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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.CacheService.*
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.time.TaxYear.current
import utils.{LoggerHelper, TransferErrorHandler}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartnersDetailsController @Inject()(
                                           errorHandler: TransferErrorHandler,
                                           authenticate: StandardAuthJourney,
                                           cachingService: CachingService,
                                           registrationService: TransferService,
                                           timeService: TimeService,
                                           cc: MessagesControllerComponents,
                                           partnersDetailsView: views.html.multiyear.transfer.partners_details,
                                           recipientDetailsForm: RecipientDetailsForm
                                         )(implicit ec: ExecutionContext)
  extends BaseController(cc)
    with LoggerHelper {

  private def marriageDateInCurrentTaxYear()(implicit request: Request[?]): Future[Boolean] =
    cachingService.get[DateOfMarriageFormInput](CACHE_MARRIAGE_DATE).map { marriageData =>
      marriageData.exists(marriageFormInput => current.contains(marriageFormInput.dateOfMarriage))
    }

  def transfer: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    val form = recipientDetailsForm.recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino)
    for {
      domCurrentTaxYear <- marriageDateInCurrentTaxYear()
      cachedRecipient   <- cachingService.get[RecipientDetailsFormInput](CACHE_RECIPIENT_DETAILS)
    } yield {
      val filledForm = cachedRecipient.map(form.fill).getOrElse(form)
      Ok(partnersDetailsView(filledForm, domCurrentTaxYear))
    }
  }

  def transferAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    marriageDateInCurrentTaxYear().flatMap { domCurrentTaxYear =>
      recipientDetailsForm
        .recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(partnersDetailsView(formWithErrors, domCurrentTaxYear))),
          recipientData =>
            for {
              _ <- cachingService.put[RecipientDetailsFormInput](CACHE_RECIPIENT_DETAILS, recipientData)
              marriageOpt <- cachingService.get[DateOfMarriageFormInput](CACHE_MARRIAGE_DATE)
              marriage <- marriageOpt match {
                case Some(dom) => Future.successful(dom)
                case None => Future.failed(new RuntimeException("Failed to retrieve marriage date"))
              }
              recipient <- registrationService.getRecipientDetailsFormData()

              dataToSend = new RegistrationFormInput(
                recipient.name,
                recipient.lastName,
                recipient.gender,
                recipient.nino,
                marriage.dateOfMarriage
              )
              _ <- registrationService.isRecipientEligible(request.nino, dataToSend)
            } yield Redirect(controllers.transfer.routes.EligibleYearsController.eligibleYears())
        ) recover errorHandler.handleError
    }
  }
}
