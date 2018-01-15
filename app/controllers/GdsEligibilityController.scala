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

import actions.{JourneyEnforcers, UnauthorisedActions}
import connectors.ApplicationAuditConnector
import forms.EligibilityCalculatorForm.calculatorForm
import services.EligibilityCalculatorService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TamcBreadcrumb

object GdsEligibilityController extends GdsEligibilityController {
  override val auditConnector = ApplicationAuditConnector
}

trait GdsEligibilityController extends BaseController with UnauthorisedActions with TamcBreadcrumb with JourneyEnforcers {

  val eligibilityCalculatorService = EligibilityCalculatorService
  val auditConnector: AuditConnector

  def calculator() = unauthorisedAction {
    implicit request =>
      Ok(views.html.calculator(calculatorForm = calculatorForm))
  }

  def calculatorAction() = unauthorisedAction {
    implicit request =>
      calculatorForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.calculator(calculatorForm = formWithErrors)),
        calculatorInput =>
          Ok(views.html.calculator(
            calculatorForm = calculatorForm.fill(calculatorInput),
            calculationResult = Some(eligibilityCalculatorService.calculate(calculatorInput)))))
  }
 }
