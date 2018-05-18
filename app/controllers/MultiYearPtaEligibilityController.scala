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
import com.google.inject.Inject
import config.ApplicationConfig
import connectors.ApplicationAuthConnector
import details.CitizenDetailsService
import forms.MultiYearDateOfBirthForm._
import forms.MultiYearDoYouLiveInScotlandForm.doYouLiveInScotlandForm
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import play.api.i18n.{I18nSupport, MessagesApi}
import services.EligibilityCalculatorService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.TamcBreadcrumb

import scala.concurrent.Future

class MultiYearPtaEligibilityController @Inject() (
                                                    val messagesApi: MessagesApi,
                                                    val auditConnector: AuditConnector,
                                                    val citizenDetailsService: CitizenDetailsService,
                                                    val maAuthRegime: MarriageAllowanceRegime,
                                                    val authConnector: ApplicationAuthConnector,
                                                    val ivUpliftUrl: String
                                                  ) extends BaseController with AuthorisedActions with TamcBreadcrumb with JourneyEnforcers with I18nSupport {
  val eligibilityCalculatorService: EligibilityCalculatorService.type = EligibilityCalculatorService

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
                  case _ => Redirect(controllers.routes.MultiYearPtaEligibilityController.doYouLiveInScotland())
                }
              })
          }
  }

  def doYouLiveInScotland() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            Ok(views.html.multiyear.pta.do_you_live_in_scotland(doYouLiveInScotlandForm = doYouLiveInScotlandForm))
          }
  }

  def doYouLiveInScotlandAction() = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          Future {
            doYouLiveInScotlandForm.bindFromRequest.fold(
              formWithErrors =>
                BadRequest(views.html.multiyear.pta.do_you_live_in_scotland(formWithErrors)),
              doYouLiveInScotlandInput => {
                doYouLiveInScotlandInput.doYouLiveInScotland match {
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
