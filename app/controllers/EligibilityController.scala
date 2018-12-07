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

import config.TamcContext
import config.ApplicationConfig.{SCOTTISH_RESIDENT, gdsFinishedUrl, ptaFinishedUrl}
import forms.MultiYearDateOfBirthForm.dateOfBirthForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import forms.MultiYearDoYouLiveInScotlandForm.doYouLiveInScotlandForm
import forms.MultiYearDoYouWantToApplyForm.doYouWantToApplyForm
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call}
import utils.TamcBreadcrumb
import views.html.multiyear.eligibility_check
import utils.isScottishResident

class EligibilityController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       unauthenticatedAction: UnauthenticatedActionTransformer
                                     )(implicit tamcContext: TamcContext) extends BaseController with I18nSupport with TamcBreadcrumb {

  def howItWorks: Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(views.html.multiyear.how_it_works())
  }

  def home: Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Redirect(controllers.routes.EligibilityController.eligibilityCheck())
  }

  def eligibilityCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(eligibility_check(eligibilityCheckForm = eligibilityForm))
  }

  def eligibilityCheckAction(): Action[AnyContent] = {

      def finishUrl(isLoggedIn: Boolean): String =
        if (isLoggedIn) ptaFinishedUrl else "https://www.gov.uk/marriage-allowance-guide"

    unauthenticatedAction {
      implicit request =>
        eligibilityForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(views.html.multiyear.eligibility_check(formWithErrors)),
          eligibilityInput => {
            if (eligibilityInput.married) {
              Redirect(controllers.routes.EligibilityController.dateOfBirthCheck())
            } else {
              Ok(views.html.multiyear.eligibility_non_eligible_finish(finishUrl(request.isLoggedIn)))
            }
          })
    }
  }

  def dateOfBirthCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(views.html.multiyear.date_of_birth_check(dateofBirthCheckForm = dateOfBirthForm))
  }

  def dateOfBirthCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      dateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.date_of_birth_check(formWithErrors)),
        _ => {
          Redirect(controllers.routes.EligibilityController.doYouLiveInScotland())
        })
  }

  def doYouLiveInScotland(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(views.html.multiyear.do_you_live_in_scotland(doYouLiveInScotlandForm = doYouLiveInScotlandForm))
        .withSession(request.session - SCOTTISH_RESIDENT)
  }

  def doYouLiveInScotlandAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      doYouLiveInScotlandForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.do_you_live_in_scotland(formWithErrors)),
        doYouLiveInScotlandInput => {
          Redirect(controllers.routes.EligibilityController.lowerEarnerCheck())
            .withSession(request.session + (SCOTTISH_RESIDENT -> doYouLiveInScotlandInput.doYouLiveInScotland.toString))
        })
  }

  def lowerEarnerCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(views.html.multiyear.lower_earner(lowerEarnerForm))
  }

  def lowerEarnerCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      lowerEarnerForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.lower_earner(formWithErrors)),
        _ => {
          Redirect(controllers.routes.EligibilityController.partnersIncomeCheck())
        })
  }

  def partnersIncomeCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(views.html.multiyear.partners_income_question(partnersIncomeForm, isScottishResident(request)))
  }

  def partnersIncomeCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      partnersIncomeForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.partners_income_question(formWithErrors, isScottishResident(request))),
        _ => {
          Redirect(controllers.routes.EligibilityController.doYouWantToApply())
        })
  }

  def doYouWantToApply(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(views.html.multiyear.do_you_want_to_apply(doYouWantToApplyForm = doYouWantToApplyForm))
  }

  def doYouWantToApplyAction(): Action[AnyContent] = {

    def finishUrl(isLoggedIn: Boolean): String =
      if (isLoggedIn) ptaFinishedUrl else gdsFinishedUrl

    unauthenticatedAction {
      implicit request =>
        doYouWantToApplyForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(views.html.multiyear.do_you_want_to_apply(formWithErrors)),
          doYouWantToApplyInput => {
            if (doYouWantToApplyInput.doYouWantToApply) {
                Redirect(controllers.routes.NewTransferController.transfer())
            } else {
              Redirect(Call("GET", finishUrl(request.isLoggedIn)))
            }
          })
    }
  }


}
