/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.EarlierYearForm.earlierYearsForm
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import services.TransferService
import utils.{LoggerHelper, TransferErrorHandler}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PreviousYearsController @Inject()(
  errorHandler: TransferErrorHandler,
  authenticate: StandardAuthJourney,
  registrationService: TransferService,
  cc: MessagesControllerComponents,
  singleYearSelect: views.html.multiyear.transfer.single_year_select)(implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def previousYears: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    registrationService.getCurrentAndPreviousYearsEligibility.map {
      case CurrentAndPreviousYearsEligibility(_, previousYears, registrationInput, _) =>
        Ok(singleYearSelect(earlierYearsForm(), registrationInput, previousYears))
    } recover errorHandler.handleError
  }
}
