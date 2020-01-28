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

package forms.coc

import play.api.data.Form
import play.api.data.Forms.{optional, single, text}
import play.api.i18n.Messages
import utils.Constants.forms.coc.MakeChangesDecisionFormConstants

//TODO add tests
//TODO constants
object MakeChangesDecisionForm {
  def form(implicit messages: Messages): Form[Option[String]] = Form[Option[String]](
    //TODO error message
    single(MakeChangesDecisionFormConstants.StopMAChoice -> optional(text).verifying(messages("cxcxcxcx"), { _.isDefined }))

  )
}