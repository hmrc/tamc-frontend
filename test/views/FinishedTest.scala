/*
 * Copyright 2023 HM Revenue & Customs
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

package views

import config.ApplicationConfig
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, EmailAddress, NinoGenerator}
import views.html.finished

import java.text.NumberFormat

class FinishedTest extends BaseTest with NinoGenerator {

  lazy val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]
  lazy val nino = generateNino().nino
  lazy val finished = instanceOf[finished]
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))
  override implicit lazy val messages: Messages = instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  lazy val email = EmailAddress("test@test.com")
  lazy val name = "Alex"
  lazy val maxBenefit = NumberFormat.getIntegerInstance().format(applicationConfig.MAX_BENEFIT())
  lazy val document = Jsoup.parse(finished(email, name).toString())

  "Finished" should {
    "return the correct title" in {
      val title = document.title()
      val expected = "Application confirmed - Marriage Allowance application - GOV.UK"

      title shouldBe expected
    }

    "display you will receive an email content" in {
      val paragraph = document.getElementById("paragraph-1").toString
      val expectedPart1 = "A confirmation email will be sent to"
      val expectedEmail = "test@test.com"
      val expectedPart2 = "from noreply@tax.service.gov.uk within 24 hours"

      paragraph should include(expectedPart1)
      paragraph should include(expectedEmail)
      paragraph should include(expectedPart2)

    }

    "display check your junk content" in {
      val paragraph = document.getElementById("paragraph-2").toString
      val expected = "If you cannot find it in your inbox, please check your spam or junk folder."

      paragraph should include(expected)

    }

    "display the correct What happens next h2" in {
      val paragraph = document.getElementsByTag("h2").toString
      val expected = "What happens next"

      paragraph should include(expected)

    }

    "display the correct HMRC will now review your Marriage Allowance content" in {
      val paragraph = document.getElementById("paragraph-3").toString
      val expected =
        s"HMRC will review your application. If you are eligible, HMRC will change your and Alex’s tax codes to save Alex up to £$maxBenefit"

      paragraph should include(expected)

    }

    "display that Marriage Allowance renews automatically" in {
      val paragraph = document.getElementById("paragraph-4").toString
      val expected = "Marriage Allowance renews each year unless you or Alex cancel it, or you are no longer eligible."

      paragraph should include(expected)

    }

  }


}
