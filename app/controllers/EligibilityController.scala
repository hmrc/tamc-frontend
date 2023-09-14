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

package controllers

import config.ApplicationConfig
import controllers.actions.UnauthenticatedActionTransformer
import forms.EligibilityCalculatorForm.calculatorForm
import models.Country
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EligibilityCalculatorService

import javax.inject.Inject

class EligibilityController @Inject()(
  unauthenticatedAction: UnauthenticatedActionTransformer,
  eligibilityCalculatorService: EligibilityCalculatorService,
  appConfig: ApplicationConfig,
  cc: MessagesControllerComponents,
  calculatorView: views.html.calculator,
  ptaCalculatorView: views.html.pta.calculator,
  howItWorksView: views.html.multiyear.how_it_works) extends BaseController(cc) {

  def eligibilityCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksView())
  }

//  def eligibilityCheckAction(): Action[AnyContent] = {
//
//    def finishUrl(isLoggedIn: Boolean): String =
//      if (isLoggedIn) appConfig.ptaFinishedUrl else appConfig.gdsFinishedUrl
//
//    unauthenticatedAction {
//      implicit request =>
//        eligibilityForm.bindFromRequest().fold(
//          formWithErrors =>
//            BadRequest(eligibilityCheckView(formWithErrors)),
//          eligibilityInput => {
//            if (eligibilityInput.married) {
//              Redirect(controllers.routes.EligibilityController.dateOfBirthCheck)
//            } else {
//              Ok(eligibilityNonEligibleFinishView(finishUrl(request.isAuthenticated)))
//            }
//          })
//    }
//  }

  def dateOfBirthCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksView())
  }

//  def dateOfBirthCheckAction(): Action[AnyContent] = unauthenticatedAction {
//    implicit request =>
//      dateOfBirthForm.bindFromRequest().fold(
//        formWithErrors =>
//          BadRequest(dateOfBirthCheckView(formWithErrors)),
//        _ => {
//          Redirect(controllers.routes.EligibilityController.doYouLiveInScotland)
//        })
//  }

  def doYouLiveInScotland(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksView())
  }

//  def doYouLiveInScotlandAction(): Action[AnyContent] = unauthenticatedAction {
//    implicit request =>
//      doYouLiveInScotlandForm.bindFromRequest().fold(
//        formWithErrors =>
//          BadRequest(doYouLiveInScotlandView(formWithErrors)),
//        doYouLiveInScotlandInput => {
//          Redirect(controllers.routes.EligibilityController.lowerEarnerCheck)
//            .withSession(request.session + (appConfig.SCOTTISH_RESIDENT -> doYouLiveInScotlandInput.doYouLiveInScotland.toString))
//        })
//  }

  def lowerEarnerCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksView())
  }

//  def lowerEarnerCheckAction(): Action[AnyContent] = unauthenticatedAction {
//    implicit request =>
//      multiYearLowerEarnerForm.lowerEarnerForm.bindFromRequest().fold(
//        formWithErrors =>
//          BadRequest(lowerEarnerView(formWithErrors)),
//        _ => {
//          Redirect(controllers.routes.EligibilityController.partnersIncomeCheck)
//        })
//  }

  def partnersIncomeCheck(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksView())
  }

//  def partnersIncomeCheckAction(): Action[AnyContent] = unauthenticatedAction {
//    implicit request =>
//      multiYearPartnersIncomeQuestionForm.partnersIncomeForm.bindFromRequest().fold(
//        formWithErrors =>
//          BadRequest(partnersIncomeQuestionView(formWithErrors, isScottishResident(request))),
//        _ => {
//          Redirect(controllers.routes.EligibilityController.doYouWantToApply)
//        })
//  }

  def doYouWantToApply(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(howItWorksView())
  }

//  def doYouWantToApplyAction(): Action[AnyContent] = {
//
//    def finishUrl(isLoggedIn: Boolean): String =
//      if (isLoggedIn) appConfig.ptaFinishedUrl else appConfig.gdsFinishedUrl
//
//    unauthenticatedAction {
//      implicit request =>
//        doYouWantToApplyForm.bindFromRequest().fold(
//          formWithErrors =>
//            BadRequest(doYouWantToApplyView(formWithErrors)),
//          doYouWantToApplyInput => {
//            if (doYouWantToApplyInput.doYouWantToApply) {
//              Redirect(controllers.routes.TransferController.transfer)
//            } else {
//              Redirect(Call("GET", finishUrl(request.isAuthenticated)))
//            }
//          })
//    }
//  }

  def gdsCalculator(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(calculatorView(calculatorForm = calculatorForm))
  }

  def gdsCalculatorAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      calculatorForm.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(calculatorView(calculatorForm = formWithErrors)),
        calculatorInput =>
          Ok(calculatorView(
            calculatorForm = calculatorForm.fill(calculatorInput),
            calculationResult = Some(eligibilityCalculatorService.calculate(calculatorInput.transferorIncome,
              calculatorInput.recipientIncome, Country.fromString(calculatorInput.country), appConfig.currentTaxYear())))))
  }

  def ptaCalculator(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      Ok(ptaCalculatorView(calculatorForm = calculatorForm))
  }

  def ptaCalculatorAction(): Action[AnyContent] = unauthenticatedAction {
    implicit request =>
      calculatorForm.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(ptaCalculatorView(calculatorForm = formWithErrors)),
        calculatorInput =>
          Ok(ptaCalculatorView(
            calculatorForm = calculatorForm.fill(calculatorInput),
            calculationResult = Some(eligibilityCalculatorService.calculate(calculatorInput.transferorIncome,
              calculatorInput.recipientIncome, Country.fromString(calculatorInput.country), appConfig.currentTaxYear())))))
  }

}
