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
import forms.LowerEarnerForm
import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.CacheService.*
import services.CachingService
import uk.gov.hmrc.time.CurrentTaxYear
import utils.{LoggerHelper, TransferErrorHandler}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LowerEarnerController @Inject() (
  errorHandler: TransferErrorHandler,
  authenticate: StandardAuthJourney,
  cachingService: CachingService,
  cc: MessagesControllerComponents,
  lowerEarnerView: views.html.lower_earner_question,
  partnerMustApplyView: views.html.partner_must_apply,
  lowerEarnerForm: LowerEarnerForm
)(implicit ec: ExecutionContext)
    extends BaseController(cc)
    with LoggerHelper
    with CurrentTaxYear {

  override def now: () => LocalDate = () => LocalDate.now()

  def isLowerEarner: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    val form: Form[Boolean] = lowerEarnerForm.apply()
    cachingService.get(CACHE_IS_LOWER_EARNER).map {
      case Some(savedData) =>
        val filledForm = form.fill(savedData)
        Ok(lowerEarnerView(filledForm))

      case None => Ok(lowerEarnerView(form))
    }
  }

  def isLowerEarnerAction: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    lowerEarnerForm
      .apply()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(lowerEarnerView(formWithErrors))),
        isLowerEarner => {
          cachingService.put(CACHE_IS_LOWER_EARNER, isLowerEarner)
          if (isLowerEarner)
            Future.successful(Redirect(controllers.transfer.routes.YourTotalIncomeController.yourTotalIncome()))
          else
            Future.successful(Redirect(controllers.transfer.routes.LowerEarnerController.partnerMustApply()))
        }
      ) recover errorHandler.handleError
  }

  def partnerMustApply: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails { implicit request =>
    Ok(partnerMustApplyView())
  }

}
