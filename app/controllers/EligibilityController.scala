/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import config.ApplicationConfig.ptaFinishedUrl
import config.{TamcContext, TamcContextImpl}
import forms.MultiYearDateOfBirthForm
import forms.MultiYearDateOfBirthForm.dateOfBirthForm
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import utils.TamcBreadcrumb
import views.html.multiyear.eligibility_check

class EligibilityController @Inject() (
                                        override val messagesApi: MessagesApi,
                                        unauthenticatedAction: UnauthenticatedActionTransformer
                                      ) extends BaseController with I18nSupport with TamcBreadcrumb {

  private implicit val tamcContext: TamcContext = TamcContextImpl

  def eligibilityCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(eligibility_check(eligibilityCheckForm = eligibilityForm))
  }

  def eligibilityCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      eligibilityForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.eligibility_check(formWithErrors)),
        eligibilityInput => {
          if (eligibilityInput.married) {
            Redirect(controllers.routes.EligibilityController.dateOfBirthCheck())
          } else {
            ???
//            Ok(views.html.multiyear.pta.eligibility_non_eligible_finish(ptaFinishedUrl))
          }
        })
  }

  def dateOfBirthCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
        Ok(views.html.multiyear.date_of_birth_check(dateofBirthCheckForm = MultiYearDateOfBirthForm.dateOfBirthForm))
  }

  def dateOfBirthCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      dateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.date_of_birth_check(formWithErrors)),
        _ => {
          Redirect(controllers.routes.MultiYearPtaEligibilityController.doYouLiveInScotland())
        })
  }


}
