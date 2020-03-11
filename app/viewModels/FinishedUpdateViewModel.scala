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

package viewModels

import models.{Role, Transferor}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.emailaddress.EmailAddress

case class FinishedUpdateViewModel(email: EmailAddress, paragraph: Html)

object FinishedUpdateViewModel{

  def apply(emailAddress: EmailAddress, role: Role)(implicit messages: Messages): FinishedUpdateViewModel = {
    FinishedUpdateViewModel(emailAddress, paragraph(role))
  }

  private def paragraph(role: Role)(implicit messages: Messages): Html = {
    val href = controllers.routes.UpdateRelationshipController.history().url
    val message1 = messages("pages.coc.finish.para1")
    val message2 = messages("general.helpline.enquiries.link.pretext")
    val message3 = messages("pages.coc.finish.para2")
    val link = s"<a href=$href>${messages("pages.coc.finish.check.status.link")}</a>"

    if(role == Transferor) {
      Html(s"<h2>${messages("pages.coc.finish.whn")}</h2>" +
        s"<p>$message1 $message2 $link $message3</p>")
    } else {
      Html(s"<p>$message1</p>")
    }
  }
}
