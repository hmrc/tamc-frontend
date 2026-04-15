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
import errors.*
import models.*
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.TransferService
import uk.gov.hmrc.time.CurrentTaxYear
import utils.{LoggerHelper, TransferErrorHandler}

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EligibleYearsController @Inject()(
                                         errorHandler: TransferErrorHandler,
                                         authenticate: StandardAuthJourney,
                                         transferService: TransferService,
                                         clock: Clock,
                                         cc: MessagesControllerComponents,
                                         eligibleYearsV: views.html.multiyear.transfer.eligible_years
                                       )(implicit ec: ExecutionContext)
  extends BaseController(cc)
    with LoggerHelper
    with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now(clock)

  def eligibleYears: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    transferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility map {
      case CurrentAndPreviousYearsEligibility(false, Nil, _, _) =>
        throw new NoTaxYearsAvailable
      case CurrentAndPreviousYearsEligibility(false, previousYears, _, _) if previousYears.nonEmpty =>
        Redirect(controllers.transfer.routes.ApplyByPostController.applyByPost())
      case CurrentAndPreviousYearsEligibility(_, _, registrationInput, _) =>
        Ok(
          eligibleYearsV(
            registrationInput.name,
            current.starts
          )
        )
    } recover errorHandler.handleError
  }

  def eligibleYearsAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    transferService.getCurrentAndPreviousYearsEligibility.flatMap {
      case CurrentAndPreviousYearsEligibility(currentYearAvailable, previousYears, registrationInput, _) =>
        if (currentYearAvailable) {
          val selectedYears: List[Int] = List(current.starts.getYear)
          transferService.saveSelectedYears(selectedYears) map { _ =>
            Redirect(controllers.transfer.routes.ConfirmEmailController.confirmYourEmail())
          }
        } else {
          throw new NoTaxYearsAvailable
        }
    } recover errorHandler.handleError
  }
}
