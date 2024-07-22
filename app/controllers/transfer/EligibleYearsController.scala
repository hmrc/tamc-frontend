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
import errors._
import forms.CurrentYearForm.currentYearForm
import models._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{TimeService, TransferService}
import utils.{LoggerHelper, TransferErrorHandler}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EligibleYearsController @Inject()(
                                         errorHandler: TransferErrorHandler,
                                         authenticate: StandardAuthJourney,
                                         registrationService: TransferService,
                                         timeService: TimeService,
                                         cc: MessagesControllerComponents,
                                         previousYearsV: views.html.multiyear.transfer.previous_years,
                                         eligibleYearsV: views.html.multiyear.transfer.eligible_years)(implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def eligibleYears: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    registrationService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility map {
      case CurrentAndPreviousYearsEligibility(false, Nil, _, _) =>
        throw new NoTaxYearsAvailable
      case CurrentAndPreviousYearsEligibility(false, previousYears, registrationInput, _) if previousYears.nonEmpty =>
        Ok(previousYearsV(registrationInput, previousYears, currentYearAvailable = false))
      case CurrentAndPreviousYearsEligibility(_, previousYears, registrationInput, _) =>
        Ok(
          eligibleYearsV(
            currentYearForm(previousYears.nonEmpty),
            previousYears.nonEmpty,
            registrationInput.name,
            Some(registrationInput.dateOfMarriage),
            Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))
          )
        )
    } recover errorHandler.handleError
  }

  def eligibleYearsAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
      case CurrentAndPreviousYearsEligibility(currentYearAvailable, previousYears, registrationInput, _) =>
        currentYearForm(previousYears.nonEmpty).bindFromRequest().fold(
          hasErrors =>
            Future {
              BadRequest(
                eligibleYearsV(
                  hasErrors,
                  previousYears.nonEmpty,
                  registrationInput.name,
                  Some(registrationInput.dateOfMarriage),
                  Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))
                )
              )
            },
          success => {

            val selectedYears = if (success.applyForCurrentYear.contains(true)) {
              List[Int](timeService.getCurrentTaxYear)
            } else {
              List[Int]()
            }

            registrationService.saveSelectedYears(selectedYears) map { _ =>
              if (previousYears.isEmpty && currentYearAvailable && (!success.applyForCurrentYear.contains(true))) {
                throw new NoTaxYearsSelected
              } else if (previousYears.nonEmpty) {
                Ok(previousYearsV(registrationInput, previousYears, currentYearAvailable))
              } else {
                Redirect(controllers.routes.TransferController.confirmYourEmail())
              }
            }
          }
        )
    } recover errorHandler.handleError
  }
}

