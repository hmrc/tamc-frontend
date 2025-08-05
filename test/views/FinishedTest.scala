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

import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, EmailAddress, NinoGenerator}
import views.html.finished

class FinishedTest extends BaseTest with NinoGenerator {

  lazy val nino = generateNino().nino
  lazy val finished = instanceOf[finished]
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))
  val email = EmailAddress("test@test.com")
  val name = "Alex"

  "Finished" should {
    "return the correct title" in {

      val document = Jsoup.parse(finished(email, name).toString())
      val title = document.title()
      val expected = messages("title.finished") + " - " + messages("title.application.pattern")

      title shouldBe expected
    }

    "display you will receive an email content" in {

      val document = Jsoup.parse(finished(email, name).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expectedYouWillReceiveAnEmail = messages("pages.finished.para1.email1")
      val expectedFromEmail = messages("pages.finished.para1.email2")

      paragraphTag should include(expectedYouWillReceiveAnEmail)
      paragraphTag should include(expectedFromEmail)

    }

    "display check your junk content" in {

      val document = Jsoup.parse(finished(email, name).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.finished.para2")

      paragraphTag should include(expected)

    }

    "display the correct What happens next h2" in {

      val document = Jsoup.parse(finished(email, name).toString())
      val paragraphTag = document.getElementsByTag("h2").toString
      val expected = messages("pages.finished.now")

      paragraphTag should include(expected)

    }

    "display the correct HMRC will now process your Marriage Allowance content" in {

      val document = Jsoup.parse(finished(email, name).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.finished.para4")

      paragraphTag should include(expected)

    }

    "display ou can check your current Marriage Allowance content" in {

      val document = Jsoup.parse(finished(email, name).toString())
      val paragraphTag = document.getElementsByTag("p").toString

      val expectedHMRCWillReview = messages("pages.finished.para4")
      val expectedName = messages(s"pages.finished.para4.2", name)
      val expectedWillRenewUnless = messages("pages.finished.para5")

      paragraphTag should include(expectedHMRCWillReview)
      paragraphTag should include(expectedName)
      paragraphTag should include(expectedWillRenewUnless)

    }

  }


}
