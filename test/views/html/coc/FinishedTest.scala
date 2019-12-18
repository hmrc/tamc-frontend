/*
 * Copyright 2019 HM Revenue & Customs
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

package views.html.coc

import play.twirl.api.Html
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.TamcViewTest

class FinishedTest extends TamcViewTest {

  val emailAddress = "test@test.com"

  override def view: Html = views.html.coc.finished(EmailAddress(emailAddress))

  "finished page " should {

    behave like pageWithTitle(messagesApi("title.change.complete"))
    behave like pageWithHeader(messagesApi("pages.coc.finish.header"))

  }



}
