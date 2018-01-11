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

package utils

import play.api.i18n.Lang
import config.ApplicationConfig
import models.EndReasonCode

object LanguageUtils {

  def isWelsh(lang: Lang) =
    ApplicationConfig.LANG_LANG_WELSH == lang.language


  def switchWelshLanguage(endReasonCode: String): String = {
    if(endReasonCode == EndReasonCode.CANCEL) {
      controllers.routes.LanguageController.switchToWelshConfirmCancel.toString()
    } else {
      controllers.routes.LanguageController.switchToWelshConfirmReject.toString()
    }
  }


  def switchEnglishLanguage(endReasonCode: String): String = {
    if(endReasonCode == EndReasonCode.CANCEL){
      controllers.routes.LanguageController.switchToEnglishConfirmCancel.toString()

    } else {
      controllers.routes.LanguageController.switchToEnglishConfirmReject.toString()

    }
  }

}
