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

class EligibilityCalculatorController @Inject()(
  unauthenticatedAction: UnauthenticatedActionTransformer,
  eligibilityCalculatorService: EligibilityCalculatorService,
  appConfig: ApplicationConfig,
  cc: MessagesControllerComponents,
  calculatorView: views.html.calculator,
  ptaCalculatorView: views.html.pta.calculator) extends BaseController(cc) {

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
