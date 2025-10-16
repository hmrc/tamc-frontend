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
import errors.NoTaxYearsAvailable
import forms.ChooseYearForm
import models.{ApplyForEligibleYears, CurrentAndPreviousYearsEligibility}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.CACHE_CHOOSE_YEARS
import services.{CachingService, TimeService, TransferService}
import utils.{LoggerHelper, TransferErrorHandler}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseYearsController @Inject()(
                                         authenticate: StandardAuthJourney,
                                         cachingService: CachingService,
                                         registrationService: TransferService,
                                         timeService: TimeService,
                                         cc: MessagesControllerComponents,
                                         chooseYearsView: views.html.multiyear.transfer.choose_eligible_years,
                                         formProvider: ChooseYearForm,
                                         errorHandler: TransferErrorHandler
                                     )(implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  private val form: Form[Seq[String]] = formProvider()
  private def currentTaxYear: LocalDate =
    timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear)

  def chooseYears: Action[AnyContent] =
    authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
      registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
        case CurrentAndPreviousYearsEligibility(false, Nil, _, _) =>
          throw new NoTaxYearsAvailable
        case CurrentAndPreviousYearsEligibility(false, previousYears, _, _) if previousYears.nonEmpty =>
          Future.successful(Redirect(controllers.transfer.routes.ApplyByPostController.applyByPost()))
        case CurrentAndPreviousYearsEligibility(_, _, registrationInput, _) =>
          cachingService.get(CACHE_CHOOSE_YEARS).map {
            case Some(cachedString) =>
              val selectedYears = cachedString.split(",").toSeq
              val filledForm = form.fill(selectedYears)
              Ok(
                chooseYearsView(
                  filledForm,
                  registrationInput.name,
                  registrationInput.dateOfMarriage,
                  currentTaxYear
                )
              )
            case None =>
              Ok(
                chooseYearsView(
                  form,
                  registrationInput.name,
                  registrationInput.dateOfMarriage,
                  currentTaxYear
                )
              )
          }
      } recover errorHandler.handleError
    }

  def chooseYearsAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
      case CurrentAndPreviousYearsEligibility(_, _, registrationInput, _) =>
        form.bindFromRequest().fold(
          formWithErrors => {
            Future.successful(BadRequest(chooseYearsView(formWithErrors, registrationInput.name, registrationInput.dateOfMarriage, currentTaxYear)))
          },
          selectedYears => {
            val cacheString = selectedYears.mkString(",")

            cachingService.put(CACHE_CHOOSE_YEARS, cacheString).map { (returnedValue: String) =>
              val currentYearStr = ApplyForEligibleYears.CurrentTaxYear.toString

              if (returnedValue == cacheString) {
                if (selectedYears.exists(_ != currentYearStr)) {
                  Redirect(controllers.transfer.routes.ApplyByPostController.applyByPost())
                } else {
                  Redirect(controllers.transfer.routes.EligibleYearsController.eligibleYears())
                }
              } else {
                logger.warn(s"[chooseYearsAction] - Unexpected value returned from cachingService.put: $returnedValue")
                Redirect(controllers.transfer.routes.ChooseYearsController.chooseYears())
              }
            }
          }
        )
    } recover errorHandler.handleError
  }
}

