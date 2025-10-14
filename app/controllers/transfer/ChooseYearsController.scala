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
import forms.ChooseYearForm
import models.ApplyForEligibleYears
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.CACHE_CHOOSE_YEARS
import services.{CachingService, TimeService}
import utils.{LoggerHelper, TransferErrorHandler}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseYearsController @Inject()(
                                         authenticate: StandardAuthJourney,
                                         cachingService: CachingService,
                                         timeService: TimeService,
                                         cc: MessagesControllerComponents,
                                         chooseYearsView: views.html.multiyear.transfer.choose_eligible_years,
                                         formProvider: ChooseYearForm,
                                         errorHandler: TransferErrorHandler
                                     )(implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  val form: Form[Seq[String]]   = formProvider()
  def currentTaxYear: LocalDate = timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear)

  def chooseYears: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    cachingService.get[String](CACHE_CHOOSE_YEARS).map {
      case Some(data) =>
        val selectedYears: Seq[String] = data.split(",").toSeq
        Ok(chooseYearsView(form.fill(selectedYears), currentTaxYear))
      case None       =>
        Ok(chooseYearsView(form, currentTaxYear))
    } recover errorHandler.handleError
  }

  def chooseYearsAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(chooseYearsView(formWithErrors, currentTaxYear))),
        selectedYears => {
          val cacheString = selectedYears.mkString(",")

          cachingService.put[String](CACHE_CHOOSE_YEARS, cacheString).map { (returnedValue: String) =>
            val currentYearStr = ApplyForEligibleYears.CurrentTaxYear.toString

            if (returnedValue == cacheString) {
              if (selectedYears.exists(_ != currentYearStr)) {
                Redirect(controllers.transfer.routes.ApplyByPostController.applyByPost())
              } else {
                Redirect(controllers.transfer.routes.PartnersDetailsController.transfer())
              }
            } else {
              logger
                .warn(s"[chooseYearsAction] - Unexpected value returned from cachingService.put: $returnedValue")
              Redirect(controllers.transfer.routes.ChooseYearsController.chooseYears())
            }
          }
        }
      ) recover errorHandler.handleError
  }
}
