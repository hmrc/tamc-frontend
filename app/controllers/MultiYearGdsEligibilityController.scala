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
import com.google.inject.Inject
import forms.MultiYearDateOfBirthForm._
import forms.MultiYearDoYouLiveInScotlandForm._
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.EligibilityCalculatorService
import utils.TamcBreadcrumb

class MultiYearGdsEligibilityController @Inject() (
                                                  val messagesApi: MessagesApi
                                                  ) extends BaseController with UnauthorisedActions with TamcBreadcrumb with JourneyEnforcers with I18nSupport {

  val eligibilityCalculatorService: EligibilityCalculatorService.type = EligibilityCalculatorService

  def home: Action[AnyContent] = unauthorisedAction {
    implicit request =>
      Redirect(controllers.routes.MultiYearGdsEligibilityController.eligibilityCheck())
  }

  def eligibilityCheck(): Action[AnyContent] = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(views.html.multiyear.gds.eligibility_check(eligibilityCheckForm = eligibilityForm)))
  }

  def eligibilityCheckAction(): Action[AnyContent] = journeyEnforcedAction {
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

  def dateOfBirthCheck(): Action[AnyContent] = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(views.html.multiyear.gds.date_of_birth_check(dateofBirthCheckForm = dateOfBirthForm)))
  }

  def dateOfBirthCheckAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      dateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.date_of_birth_check(formWithErrors)),
        dateOfBirthInput => {
          dateOfBirthInput.dateOfBirth match {
            case _ => Redirect(controllers.routes.MultiYearGdsEligibilityController.doYouLiveInScotland())
          }
        })
  }

  def doYouLiveInScotland(): Action[AnyContent] = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(views.html.multiyear.gds.do_you_live_in_scotland(doYouLiveInScotlandForm = doYouLiveInScotlandForm)))
  }

  def doYouLiveInScotlandAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      doYouLiveInScotlandForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.do_you_live_in_scotland(formWithErrors)),
        doYouLiveInScotlandInput => {
          doYouLiveInScotlandInput.doYouLiveInScotland match {
            case _ => Redirect(controllers.routes.MultiYearGdsEligibilityController.lowerEarnerCheck())
          }
        })
  }

  def lowerEarnerCheck(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      Ok(views.html.multiyear.gds.lower_earner(lowerEarnerFormInput = lowerEarnerForm))
  }

  def lowerEarnerCheckAction(): Action[AnyContent] = journeyEnforcedAction {
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

  def partnersIncomeCheck(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      Ok(views.html.multiyear.gds.partners_income_question(partnersIncomeForm))
  }

  def partnersIncomeCheckAction(): Action[AnyContent] = journeyEnforcedAction {
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
