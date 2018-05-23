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
import config.ApplicationConfig
import forms.MultiYearDateOfBirthForm._
import forms.MultiYearDoYouLiveInScotlandForm._
import forms.MultiYearDoYouWantToApplyForm._
import forms.MultiYearEligibilityCheckForm.eligibilityForm
import forms.MultiYearLowerEarnerForm.lowerEarnerForm
import forms.MultiYearPartnersIncomeQuestionForm.partnersIncomeForm
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, _}
import services.EligibilityCalculatorService
import uk.gov.hmrc.play.config.RunMode
import utils.{TamcBreadcrumb, scottishResident}
import views.html.multiyear.gds._

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
        response = Ok(eligibility_check(eligibilityCheckForm = eligibilityForm)))
  }

  def eligibilityCheckAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      eligibilityForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(eligibility_check(formWithErrors)),
        eligibilityInput => {
          if (eligibilityInput.married) {
            Redirect(controllers.routes.MultiYearGdsEligibilityController.dateOfBirthCheck())
          } else {
            Ok(eligibility_non_eligible_finish())
          }
        })
  }

  def doYouWantToApply(): Action[AnyContent] = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(views.html.multiyear.gds.do_you_want_to_apply(doYouWantToApplyForm = doYouWantToApplyForm)))
  }

  def doYouWantToApplyAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      doYouWantToApplyForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.gds.do_you_want_to_apply(formWithErrors)),
        doYouWantToApplyInput => {
          if (doYouWantToApplyInput.doYouWantToApply) {
            Redirect(controllers.routes.UpdateRelationshipController.history())
          } else {
            Redirect(Call("GET", ApplicationConfig.gdsFinishedUrl))
          }
        })
  }

  def dateOfBirthCheck(): Action[AnyContent] = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(date_of_birth_check(dateofBirthCheckForm = dateOfBirthForm)))
  }

  def dateOfBirthCheckAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      dateOfBirthForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(date_of_birth_check(formWithErrors)),
        dateOfBirthInput => {
          Redirect(controllers.routes.MultiYearGdsEligibilityController.doYouLiveInScotland())
        })
  }

  def doYouLiveInScotland(): Action[AnyContent] = unauthorisedAction {
    implicit request =>
      setPtaAwareGdsJourney(
        request = request,
        response = Ok(do_you_live_in_scotland(doYouLiveInScotlandForm = doYouLiveInScotlandForm))
          .withSession(request.session - "scottish_resident")
      )
  }

  def doYouLiveInScotlandAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      doYouLiveInScotlandForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(do_you_live_in_scotland(formWithErrors)),
        doYouLiveInScotlandInput => {
          Redirect(controllers.routes.MultiYearGdsEligibilityController.lowerEarnerCheck())
            .withSession(request.session + ("scottish_resident" -> doYouLiveInScotlandInput.doYouLiveInScotland.toString))
        })
  }

  def lowerEarnerCheck(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      Ok(lower_earner(lowerEarnerFormInput = lowerEarnerForm))
  }

  def lowerEarnerCheckAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      lowerEarnerForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(lower_earner(formWithErrors)),
        lowerEarnerInput => {
          Redirect(controllers.routes.MultiYearGdsEligibilityController.partnersIncomeCheck())
        })
  }

  def partnersIncomeCheck(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      Ok(partners_income_question(partnersIncomeForm, scottishResident(request)))
  }

  def partnersIncomeCheckAction(): Action[AnyContent] = journeyEnforcedAction {
    implicit request =>
      partnersIncomeForm.bindFromRequest.fold(
        formWithErrors =>
          BadRequest(partners_income_question(formWithErrors, scottishResident(request))),
        partnersIncomeInput => {
          Redirect(controllers.routes.MultiYearGdsEligibilityController.doYouWantToApply())
        })
  }
}
