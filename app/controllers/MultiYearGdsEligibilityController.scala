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
import forms.MultiYearDateOfBirthForm._
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import services.EligibilityCalculatorService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TamcBreadcrumb

object MultiYearGdsEligibilityController extends MultiYearGdsEligibilityController {
  override val auditConnector = ApplicationAuditConnector
}

trait MultiYearGdsEligibilityController extends BaseController with UnauthorisedActions with TamcBreadcrumb with JourneyEnforcers {

  val eligibilityCalculatorService = EligibilityCalculatorService
  val auditConnector: AuditConnector

  def home = unauthorisedAction {
    implicit request =>
      Redirect(controllers.routes.MultiYearGdsEligibilityController.eligibilityCheck())
  }

  def eligibilityCheck() = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(views.html.multiyear.gds.eligibility_check(eligibilityCheckForm = eligibilityForm)))
  }

  def eligibilityCheckAction() = journeyEnforcedAction {
    implicit request =>
      eligibilityForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.eligibility_check(formWithErrors)),
        eligibilityInput => {
          eligibilityInput.married match {
            case true => Redirect(controllers.routes.MultiYearGdsEligibilityController.dateOfBirthCheck())
            case _    => Ok(views.html.multiyear.gds.eligibility_non_eligible_finish())
          }
        })
  }

  def dateOfBirthCheck() = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(views.html.multiyear.gds.date_of_birth_check(dateofBirthCheckForm = dateOfBirthForm)))
  }

  def dateOfBirthCheckAction() = journeyEnforcedAction {
    implicit request =>
      dateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.date_of_birth_check(formWithErrors)),
        dateOfBirthInput => {
          dateOfBirthInput.dateOfBirth match {
            case _ => Redirect(controllers.routes.MultiYearGdsEligibilityController.lowerEarnerCheck())
          }
        })
  }

  def lowerEarnerCheck() = journeyEnforcedAction {
    implicit request =>
      Ok(views.html.multiyear.gds.lower_earner(lowerEarnerFormInput = lowerEarnerForm))
  }

  def lowerEarnerCheckAction() = journeyEnforcedAction {
    implicit request =>
      lowerEarnerForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.lower_earner(formWithErrors)),
        lowerEarnerInput => {
          lowerEarnerInput.lowerEarner match {
            case _ => Redirect(controllers.routes.MultiYearGdsEligibilityController.partnersIncomeCheck())
          }
        })
  }

  def partnersIncomeCheck() = journeyEnforcedAction {
    implicit request =>
      Ok(views.html.multiyear.gds.partners_income_question(partnersIncomeForm))
  }

  def partnersIncomeCheckAction() = journeyEnforcedAction {
    implicit request =>
      partnersIncomeForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.partners_income_question(formWithErrors)),
        partnersIncomeInput => {
          partnersIncomeInput.partnersIncomeQuestion match {
            case _ => Redirect(controllers.routes.UpdateRelationshipController.history())
          }
        })
  }
}
