/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import play.api.i18n.Messages

object LanguageUtils {

  //TODO add test
  //TODO pass lang object not message?
  //TODO pass language not all these objects?? Easier to test and easier to maintain
  def isWelsh(messages: Messages) = {
    if (messages != null && messages.lang != null) {
      ApplicationConfig.LANG_LANG_WELSH == messages.lang.language
    } else {
      false
    }
  }
}





