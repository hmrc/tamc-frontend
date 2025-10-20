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
import forms.DateOfMarriageForm
import models.DateOfMarriageFormInput
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.*
import services.{CachingService, TimeService}
import uk.gov.hmrc.time.CurrentTaxYear
import utils.{LoggerHelper, TransferErrorHandler}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DateOfMarriageController @Inject() (
                                            errorHandler: TransferErrorHandler,
                                            authenticate: StandardAuthJourney,
                                            cachingService: CachingService,
                                            timeService: TimeService,
                                            cc: MessagesControllerComponents,
                                            dateOfMarriageV: views.html.date_of_marriage,
                                            dateOfMarriageForm: DateOfMarriageForm
                                          )(implicit ec: ExecutionContext)
  extends BaseController(cc)
    with LoggerHelper
    with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  def dateOfMarriage: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    val form: Form[DateOfMarriageFormInput] = dateOfMarriageForm.dateOfMarriageForm(today = timeService.getCurrentDate)
    cachingService.get(CACHE_MARRIAGE_DATE).map {
      case Some(savedData) =>
        val date = savedData.dateOfMarriage
        val filledForm = form.bind(
          Map(
            "dateOfMarriage.day" -> date.getDayOfMonth.toString,
            "dateOfMarriage.month" -> date.getMonthValue.toString,
            "dateOfMarriage.year" -> date.getYear.toString,
          )
        )
        Ok(dateOfMarriageV(filledForm))

      case None => Ok(dateOfMarriageV(form))
    }
  }

  def dateOfMarriageAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      dateOfMarriageForm
        .dateOfMarriageForm(today = timeService.getCurrentDate)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(dateOfMarriageV(formWithErrors))),
          marriageData => {
            cachingService.put[DateOfMarriageFormInput](CACHE_MARRIAGE_DATE, marriageData)
            if (current.contains(marriageData.dateOfMarriage))
              Future.successful(Redirect(controllers.transfer.routes.PartnersDetailsController.transfer()))
            else
              Future.successful(Redirect(controllers.transfer.routes.ChooseYearsController.chooseYears()))
          }
        ) recover errorHandler.handleError
  }
}
