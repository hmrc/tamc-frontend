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

import actions.{AuthorisedActions, JourneyEnforcers, MarriageAllowanceRegime}
import config.ApplicationConfig._
import config.ApplicationConfig
import connectors.{ApplicationAuditConnector, ApplicationAuthConnector}
import details.CitizenDetailsService
import forms.MultiYearDateOfBirthForm._
import forms.MultiYearDoYouLiveInScotlandForm.doYouLiveInScotlandForm
import forms.MultiYearDoYouWantToApplyForm.doYouWantToApplyForm
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import play.api.mvc.{Action, AnyContent, Call}
import _root_.services.EligibilityCalculatorService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.{TamcBreadcrumb, isScottishResident}

import scala.concurrent.Future


object MultiYearPtaEligibilityController extends MultiYearPtaEligibilityController {
  override val auditConnector: ApplicationAuditConnector.type = ApplicationAuditConnector
  override lazy val maAuthRegime: MarriageAllowanceRegime.type = MarriageAllowanceRegime
  override val authConnector: ApplicationAuthConnector.type = ApplicationAuthConnector
  override val citizenDetailsService: CitizenDetailsService.type = CitizenDetailsService
  override val ivUpliftUrl: String = ApplicationConfig.ivUpliftUrl
}

trait MultiYearPtaEligibilityController extends BaseController with AuthorisedActions with TamcBreadcrumb with JourneyEnforcers {
  val authConnector: ApplicationAuthConnector
  val auditConnector: AuditConnector
  val eligibilityCalculatorService: EligibilityCalculatorService.type = EligibilityCalculatorService

  def howItWorks(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            setGdsAwarePtaJourney(
              request = request,
              response = Ok(views.html.multiyear.pta.how_it_works()))
          }

  }

  def eligibilityCheck(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.eligibility_check(eligibilityCheckForm = eligibilityForm))
          }
  }

  def eligibilityCheckAction(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            eligibilityForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.eligibility_check(formWithErrors)),
              eligibilityInput => {
                if(eligibilityInput.married) {
                  Redirect(controllers.routes.EligibilityController.dateOfBirthCheck())
                } else {
                  Ok(views.html.multiyear.pta.eligibility_non_eligible_finish(ptaFinishedUrl))
                }
              })
          }
  }

  def doYouWantToApply(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.do_you_want_to_apply(doYouWantToApplyForm = doYouWantToApplyForm))
          }
  }

  def doYouWantToApplyAction(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            doYouWantToApplyForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.do_you_want_to_apply(formWithErrors)),
              doYouWantToApplyInput => {
                if (doYouWantToApplyInput.doYouWantToApply) {
                  Redirect(controllers.routes.TransferController.transfer())
                } else {
                  Redirect(Call("GET", ptaFinishedUrl))
                }
              })
          }
  }

  def dateOfBirthCheck(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.date_of_birth_check(dateofBirthCheckForm = dateOfBirthForm))
          }
  }

  def dateOfBirthCheckAction(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            dateOfBirthForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.date_of_birth_check(formWithErrors)),
              dateOfBirthInput => {
                Redirect(controllers.routes.EligibilityController.doYouLiveInScotland())
              })
          }
  }

  def doYouLiveInScotland(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.do_you_live_in_scotland(doYouLiveInScotlandForm = doYouLiveInScotlandForm))
              .withSession(request.session - SCOTTISH_RESIDENT)
          }
  }

  def doYouLiveInScotlandAction(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            doYouLiveInScotlandForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.do_you_live_in_scotland(formWithErrors)),
              doYouLiveInScotlandInput => {
                Redirect(controllers.routes.EligibilityController.lowerEarnerCheck())
                  .withSession(request.session + (SCOTTISH_RESIDENT -> doYouLiveInScotlandInput.doYouLiveInScotland.toString))
              })
          }
  }

  def lowerEarnerCheck(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.lower_earner(lowerEarnerForm))
          }
  }

  def lowerEarnerCheckAction(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            lowerEarnerForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.lower_earner(formWithErrors)),
              lowerEarnerInput => {
                Redirect(controllers.routes.MultiYearPtaEligibilityController.partnersIncomeCheck())
              })
          }
  }

  def partnersIncomeCheck(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.partners_income_question(partnersIncomeForm, isScottishResident(request)))
          }
  }

  def partnersIncomeCheckAction(): Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            partnersIncomeForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.partners_income_question(formWithErrors, isScottishResident(request))),
              incomeCheckInput => {
                Redirect(controllers.routes.MultiYearPtaEligibilityController.doYouWantToApply())
              })
          }
  }
}
