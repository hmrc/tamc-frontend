package utils

import play.api.i18n.Lang
import config.ApplicationConfig

object LanguageUtils {
  def isWelsh(lang: Lang) =
    ApplicationConfig.LANG_LANG_WELSH == lang.language
}