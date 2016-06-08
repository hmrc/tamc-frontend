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
}