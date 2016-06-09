package controllers

import play.api.Play.current
import play.api.i18n.Lang
import play.api.mvc.Action
import config.ApplicationConfig
import uk.gov.hmrc.play.frontend.controller.FrontendController

object LanguageController extends LanguageController

class LanguageController extends FrontendController {

  def switchToWelshEligibilityCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.eligibilityCheck()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishEligibilityCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.eligibilityCheck()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshLowerEarnerCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.lowerEarnerCheck()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishLowerEarnerCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.lowerEarnerCheck()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshPartnersIncomeCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.partnersIncomeCheck()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishPartnersIncomeCheck = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.partnersIncomeCheck()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshVerify = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.verify()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishVerify = Action { implicit request =>
    Redirect(routes.MultiYearGdsEligibilityController.verify()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshHistory = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.history()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishHistory = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.history()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshConfirmEmail = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmEmail()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishConfirmEmail = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmEmail()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshConfirmUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmUpdate()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishConfirmUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmUpdate()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshConfirmCancel = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmCancel()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishConfirmCancel = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmCancel()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshConfirmReject = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmReject()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishConfirmReject = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.confirmReject()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshFinishUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.finishUpdate()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishFinishUpdate = Action { implicit request =>
    Redirect(routes.UpdateRelationshipController.finishUpdate()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshTransfer = Action { implicit request =>
    Redirect(routes.TransferController.transfer()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishTransfer = Action { implicit request =>
    Redirect(routes.TransferController.transfer()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshEligibleYears = Action { implicit request =>
    Redirect(routes.TransferController.eligibleYears()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishEligibleYears = Action { implicit request =>
    Redirect(routes.TransferController.eligibleYears()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshPreviousYears = Action { implicit request =>
    Redirect(routes.TransferController.previousYears()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishPreviousYears = Action { implicit request =>
    Redirect(routes.TransferController.previousYears()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshConfirmYourEmail = Action { implicit request =>
    Redirect(routes.TransferController.confirmYourEmail()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishConfirmYourEmail = Action { implicit request =>
    Redirect(routes.TransferController.confirmYourEmail()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshConfirm = Action { implicit request =>
    Redirect(routes.TransferController.confirm()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishConfirm = Action { implicit request =>
    Redirect(routes.TransferController.confirm()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }

  def switchToWelshFinished = Action { implicit request =>
    Redirect(routes.TransferController.finished()).withLang(Lang(ApplicationConfig.LANG_CODE_WELSH))
  }

  def switchToEnglishFinished = Action { implicit request =>
    Redirect(routes.TransferController.finished()).withLang(Lang(ApplicationConfig.LANG_CODE_ENGLISH))
  }  
}