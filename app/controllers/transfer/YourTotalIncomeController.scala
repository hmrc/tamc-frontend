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
import forms.YourTotalIncomeForm
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.*
import services.CachingService
import uk.gov.hmrc.time.CurrentTaxYear
import utils.{LoggerHelper, TransferErrorHandler}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourTotalIncomeController @Inject() (
  errorHandler: TransferErrorHandler,
  authenticate: StandardAuthJourney,
  cachingService: CachingService,
  cc: MessagesControllerComponents,
  totalIncomePage: views.html.total_income_question,
  invalidIncomePage: views.html.invalid_income,
  totalIncomeForm: YourTotalIncomeForm
)(implicit ec: ExecutionContext)
    extends BaseController(cc)
    with LoggerHelper
    with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  def yourTotalIncome: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    val form: Form[Boolean] = totalIncomeForm.apply()
    cachingService.get(CACHE_TOTAL_INCOME).map {
      case Some(savedData) =>
        val filledForm = form.fill(savedData)
        Ok(totalIncomePage(filledForm))

      case None => Ok(totalIncomePage(form))
    }
  }

  def yourTotalIncomeAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async {
    implicit request =>
      totalIncomeForm
        .apply()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(totalIncomePage(formWithErrors))),
          totalIncomeValid => {
            cachingService.put(CACHE_TOTAL_INCOME, totalIncomeValid)
            if (totalIncomeValid)
              Future.successful(Redirect(controllers.transfer.routes.PartnersDetailsController.transfer()))
            else
              Future.successful(Redirect(controllers.transfer.routes.YourTotalIncomeController.invalidIncome()))
          }
        ) recover errorHandler.handleError
  }

  def invalidIncome: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    Ok(invalidIncomePage())
  }

}
