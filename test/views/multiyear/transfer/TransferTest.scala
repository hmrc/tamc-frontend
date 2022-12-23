/*
 * Copyright 2022 HM Revenue & Customs
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

package views.multiyear.transfer

import forms.RecipientDetailsForm
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import views.html.multiyear.transfer.transfer

import java.time.LocalDate

class TransferTest extends BaseTest {

  lazy val transferView = instanceOf[transfer]
  lazy val transferForm = instanceOf[RecipientDetailsForm]
  implicit  val request = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))


  "Transfer page" should {
    "display the correct page title of transfer page" in {

      val document = Jsoup.parse(transferView(transferForm.recipientDetailsForm(LocalDate.now, Nino("AA000000A"))).toString())
      val title = document.title()
      val expected = messages("title.application.pattern", messages("pages.form.h1"))

      title shouldBe expected
    }

        "display lower income content" in {

          val document = Jsoup.parse(transferView(transferForm.recipientDetailsForm(LocalDate.now, Nino("AA000000A"))).toString())
          val paragraphTag = document.getElementsByTag("p").toString
          val expected = messages("pages.form.details")

          paragraphTag should include(expected)

        }
  }

}
