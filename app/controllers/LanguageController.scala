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

import config.ApplicationConfig
import play.api.Play.current
import play.api.i18n.Lang
import play.api.mvc.{Action, LegacyI18nSupport}
import play.api.i18n.Messages.Implicits._

object LanguageController extends LanguageController

class LanguageController extends BaseController with LegacyI18nSupport {

  def switchToWelshEligibilityCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.eligibilityCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishEligibilityCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.eligibilityCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshDateOfBirthCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.dateOfBirthCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishDateOfBirthCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.dateOfBirthCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshLowerEarnerCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.lowerEarnerCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishLowerEarnerCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.lowerEarnerCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshPartnersIncomeCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.partnersIncomeCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishPartnersIncomeCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.partnersIncomeCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshHistory = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.history()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishHistory = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.history()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshIncomeChange = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.changeOfIncome()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishIncomeChange = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.changeOfIncome()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshBereavement = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.bereavement()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishBereavement = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.bereavement()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshConfirmEmail = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmEmail()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishConfirmEmail = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmEmail()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshConfirmUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmUpdate()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishConfirmUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmUpdate()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshConfirmCancel = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmCancel()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishConfirmCancel = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmCancel()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshConfirmReject = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmReject()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishConfirmReject = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmReject()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshFinishUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.finishUpdate()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishFinishUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.finishUpdate()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshTransfer = Action { implicit request =>
    Redirect(routes.TransferController.transfer()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishTransfer = Action { implicit request =>
    Redirect(routes.TransferController.transfer()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshDateOfMarriage = Action { implicit request =>
    Redirect(routes.TransferController.dateOfMarriage()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishDateOfMarriage = Action { implicit request =>
    Redirect(routes.TransferController.dateOfMarriage()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshEligibleYears = Action { implicit request =>
    Redirect(routes.TransferController.eligibleYears()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishEligibleYears = Action { implicit request =>
    Redirect(routes.TransferController.eligibleYears()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshPreviousYears = Action { implicit request =>
    Redirect(routes.TransferController.previousYears()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishPreviousYears = Action { implicit request =>
    Redirect(routes.TransferController.previousYears()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshConfirmYourEmail = Action { implicit request =>
    Redirect(routes.TransferController.confirmYourEmail()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishConfirmYourEmail = Action { implicit request =>
    Redirect(routes.TransferController.confirmYourEmail()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshConfirm = Action { implicit request =>
    Redirect(routes.TransferController.confirm()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishConfirm = Action { implicit request =>
    Redirect(routes.TransferController.confirm()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshFinished = Action { implicit request =>
    Redirect(routes.TransferController.finished()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishFinished = Action { implicit request =>
    Redirect(routes.TransferController.finished()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshHowItWorks = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.howItWorks()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishHowItWorks = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.howItWorks()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshEligibilityCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.eligibilityCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishEligibilityCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.eligibilityCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshDateOfBirthCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.dateOfBirthCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishDateOfBirthCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.dateOfBirthCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshLowerEarnerCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.lowerEarnerCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishLowerEarnerCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.lowerEarnerCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshPartnersIncomeCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.partnersIncomeCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishPartnersIncomeCheckPta = Action { implicit request =>
    Redirect(routes.MultiYearPtaEligibilityController.partnersIncomeCheck()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshCalculator = Action { implicit request =>
    Redirect(routes.GdsEligibilityController.calculator()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishCalculator = Action { implicit request =>
    Redirect(routes.GdsEligibilityController.calculator()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshCalculatorPta = Action { implicit request =>
    Redirect(routes.PtaEligibilityController.calculator()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishCalculatorPta = Action { implicit request =>
    Redirect(routes.PtaEligibilityController.calculator()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }

  def switchToWelshSignin = Action { implicit request =>
    Redirect(routes.AuthorisationController.sessionTimeout()).withLang(Lang(ApplicationConfig.LANG_LANG_WELSH))
  }

  def switchToEnglishSignin = Action { implicit request =>
    Redirect(routes.AuthorisationController.sessionTimeout()).withLang(Lang(ApplicationConfig.LANG_LANG_ENGLISH))
  }
}
