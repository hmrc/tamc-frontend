/*
 * Copyright 2016 HM Revenue & Customs
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

import scala.concurrent.Future
import uk.gov.hmrc.play.frontend.controller.FrontendController
import actions.AuthorisedActions
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearIncomeCheckForm.incomeCheckForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import utils.TamcBreadcrumb
import actions.JourneyEnforcers
import connectors.ApplicationAuditConnector
import actions.MarriageAllowanceRegime
import connectors.ApplicationAuthConnector
import details.CitizenDetailsService
import config.ApplicationConfig
import forms.MultiYearDateOfBirthForm._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import services.EligibilityCalculatorService

object MultiYearPtaEligibilityController extends MultiYearPtaEligibilityController {
  override val auditConnector = ApplicationAuditConnector
  override lazy val maAuthRegime = MarriageAllowanceRegime
  override val authConnector = ApplicationAuthConnector
  override val citizenDetailsService = CitizenDetailsService
  override val ivUpliftUrl = ApplicationConfig.ivUpliftUrl
}

trait MultiYearPtaEligibilityController extends FrontendController with AuthorisedActions with TamcBreadcrumb with JourneyEnforcers {

  val eligibilityCalculatorService = EligibilityCalculatorService
  val authConnector: ApplicationAuthConnector
  val auditConnector: AuditConnector

  def howItWorks() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            setGdsAwarePtaJourney(
              request = request,
              response = Ok(views.html.multiyear.pta.how_it_works()))
          }

  }

  def eligibilityCheck() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.eligibility_check(eligibilityCheckForm = eligibilityForm))
          }
  }

  def eligibilityCheckAction() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            eligibilityForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.eligibility_check(formWithErrors)),
              eligibilityInput => {
                eligibilityInput.married match {
                  case true => Redirect(controllers.routes.MultiYearPtaEligibilityController.dateOfBirthCheck())
                  case _ => Ok(views.html.multiyear.pta.eligibility_non_eligible_finish(ApplicationConfig.ptaFinishedUrl))
                }
              })
          }
  }

  def dateOfBirthCheck() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.date_of_birth_check(dateofBirthCheckForm = dateOfBirthForm))
          }
  }

  def dateOfBirthCheckAction() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            dateOfBirthForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.date_of_birth_check(formWithErrors)),
              dateOfBirthInput => {
                dateOfBirthInput.dateOfBirth match {
                  case _ => Redirect(controllers.routes.MultiYearPtaEligibilityController.lowerEarnerCheck())
                }
              })
          }
  }

  def lowerEarnerCheck() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.lower_earner(lowerEarnerForm))
          }
  }

  def lowerEarnerCheckAction() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            lowerEarnerForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.lower_earner(formWithErrors)),
              lowerEarnerInput => {
                lowerEarnerInput.lowerEarner match {
                  case _ => Redirect(controllers.routes.MultiYearPtaEligibilityController.partnersIncomeCheck())
                }
              })
          }
  }

  def partnersIncomeCheck() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.partners_income_question(partnersIncomeForm))
          }
  }

  def partnersIncomeCheckAction() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            partnersIncomeForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.partners_income_question(formWithErrors)),
              incomeCheckInput => {
                Redirect(controllers.routes.TransferController.transfer())
              })
          }
  }

}
