/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import controllers.actions.UnauthenticatedActionTransformer
import forms.EligibilityCalculatorForm.calculatorForm
import forms.MultiYearDateOfBirthForm.dateOfBirthForm
import forms.MultiYearDoYouLiveInScotlandForm.doYouLiveInScotlandForm
import forms.MultiYearDoYouWantToApplyForm.doYouWantToApplyForm
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import javax.inject.Inject
import models.Country
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import services.EligibilityCalculatorService
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.isScottishResident
import views.html.multiyear.eligibility_check

class EligibilityController @Inject()(
  unauthenticatedAction: UnauthenticatedActionTransformer,
  eligibilityCalculatorService: EligibilityCalculatorService,
  appConfig: ApplicationConfig,
  cc: MessagesControllerComponents,
  howItWorksV: views.html.multiyear.how_it_works,
  eligibilityCheckV: views.html.multiyear.eligibility_check,
  eligibilityNonEligibleFinish: views.html.multiyear.eligibility_non_eligible_finish,
  dateOfBirthCheckV: views.html.multiyear.date_of_birth_check,
  doYouLiveInScotlandV: views.html.multiyear.do_you_live_in_scotland,
  lowerEarner: views.html.multiyear.lower_earner,
  partnersIncomeQuestion: views.html.multiyear.partners_income_question,
  doYouWantToApplyV: views.html.multiyear.do_you_want_to_apply,
  calculator: views.html.calculator)(implicit templateRenderer: TemplateRenderer, formPartialRetriever: FormPartialRetriever) extends BaseController(cc) {

  def howItWorks: Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksV())
  }

  def home: Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Redirect(controllers.routes.EligibilityController.eligibilityCheck())
  }

  def eligibilityCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(eligibilityCheckV(eligibilityCheckForm = eligibilityForm))
  }

  def eligibilityCheckAction(): Action[AnyContent] = {

    def finishUrl(isLoggedIn: Boolean): String =
      if (isLoggedIn) appConfig.ptaFinishedUrl else appConfig.gdsFinishedUrl

    unauthenticatedAction {
      implicit request =>
        eligibilityForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(eligibilityCheckV(formWithErrors)),
          eligibilityInput => {
            if (eligibilityInput.married) {
              Redirect(controllers.routes.EligibilityController.dateOfBirthCheck())
            } else {
              Ok(eligibilityNonEligibleFinish(finishUrl(request.isAuthenticated)))
            }
          })
    }
  }

  def dateOfBirthCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(dateOfBirthCheckV(dateOfBirthForm))
  }

  def dateOfBirthCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      dateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(dateOfBirthCheckV(formWithErrors)),
        _ => {
          Redirect(controllers.routes.EligibilityController.doYouLiveInScotland())
        })
  }

  def doYouLiveInScotland(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(doYouLiveInScotlandV(doYouLiveInScotlandForm = doYouLiveInScotlandForm))
        .withSession(request.session - appConfig.SCOTTISH_RESIDENT)
  }

  def doYouLiveInScotlandAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      doYouLiveInScotlandForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(doYouLiveInScotlandV(formWithErrors)),
        doYouLiveInScotlandInput => {
          Redirect(controllers.routes.EligibilityController.lowerEarnerCheck())
            .withSession(request.session + (appConfig.SCOTTISH_RESIDENT -> doYouLiveInScotlandInput.doYouLiveInScotland.toString))
        })
  }

  def lowerEarnerCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(lowerEarner(lowerEarnerForm))
  }

  def lowerEarnerCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      lowerEarnerForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(lowerEarner(formWithErrors)),
        _ => {
          Redirect(controllers.routes.EligibilityController.partnersIncomeCheck())
        })
  }

  def partnersIncomeCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(partnersIncomeQuestion(partnersIncomeForm, isScottishResident(request)))
  }

  def partnersIncomeCheckAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      partnersIncomeForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(partnersIncomeQuestion(formWithErrors, isScottishResident(request))),
        _ => {
          Redirect(controllers.routes.EligibilityController.doYouWantToApply())
        })
  }

  def doYouWantToApply(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(doYouWantToApplyV(doYouWantToApplyForm = doYouWantToApplyForm))
  }

  def doYouWantToApplyAction(): Action[AnyContent] = {

    def finishUrl(isLoggedIn: Boolean): String =
      if (isLoggedIn) appConfig.ptaFinishedUrl else appConfig.gdsFinishedUrl

    unauthenticatedAction {
      implicit request =>
        doYouWantToApplyForm.bindFromRequest.fold(
          formWithErrors =>
            BadRequest(doYouWantToApplyV(formWithErrors)),
          doYouWantToApplyInput => {
            if (doYouWantToApplyInput.doYouWantToApply) {
              Redirect(controllers.routes.TransferController.transfer())
            } else {
              Redirect(Call("GET", finishUrl(request.isAuthenticated)))
            }
          })
    }
  }

  def gdsCalculator(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(calculator(calculatorForm = calculatorForm))
  }

  def gdsCalculatorAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      calculatorForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(calculator(calculatorForm = formWithErrors)),
        calculatorInput =>
          Ok(calculator(
            calculatorForm = calculatorForm.fill(calculatorInput),
            calculationResult = Some(eligibilityCalculatorService.calculate(calculatorInput.transferorIncome,
              calculatorInput.recipientIncome, Country.fromString(calculatorInput.country))))))
  }

  def ptaCalculator(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(calculator(calculatorForm = calculatorForm))
  }

  def ptaCalculatorAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      calculatorForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(calculator(calculatorForm = formWithErrors)),
        calculatorInput =>
          Ok(calculator(
            calculatorForm = calculatorForm.fill(calculatorInput),
            calculationResult = Some(eligibilityCalculatorService.calculate(calculatorInput.transferorIncome,
              calculatorInput.recipientIncome, Country.fromString(calculatorInput.country))))))
  }

}
