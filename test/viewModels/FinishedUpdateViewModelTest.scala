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

import models.{Recipient, Transferor}
import play.twirl.api.Html
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.TamcViewModelTest

class FinishedUpdateViewModelTest extends TamcViewModelTest {

  val email = EmailAddress("email@email.com")

  "FinishedUpdateViewModel" should {
    "Return paragraphs with the correct content" when {
      "User is a Transferor" in {
        val viewModel = FinishedUpdateViewModel(email, Transferor)

        val expectedHref = controllers.routes.UpdateRelationshipController.history().url
        val expectedMessage1 = messagesApi("pages.coc.finish.para1")
        val expectedMessage2 = messagesApi("general.helpline.enquiries.link.pretext")
        val expectedMessage3 = messagesApi("pages.coc.finish.para2")
        val expectedLink = s"<a href=$expectedHref>${messagesApi("pages.coc.finish.check.status.link")}</a>"

        val expectedHtml = Html(
          s"<h2>${messagesApi("pages.coc.finish.whn")}</h2>" +
          s"<p>$expectedMessage1 $expectedMessage2 $expectedLink $expectedMessage3</p>"
        )

        viewModel shouldBe FinishedUpdateViewModel(email, expectedHtml)
      }

      "User is a Recipient" in {
        val viewModel = FinishedUpdateViewModel(email, Recipient)
        val expectedHtml = Html(s"<p>${messagesApi("pages.coc.finish.para1")}</p>")

        viewModel shouldBe FinishedUpdateViewModel(email, expectedHtml)
      }
    }
  }
}
