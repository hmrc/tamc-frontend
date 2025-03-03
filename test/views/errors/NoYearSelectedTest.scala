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

package views.errors

import models.auth.{BaseUserRequest, UserRequest}
import org.jsoup.Jsoup
import play.api.test.{FakeRequest, Helpers}
import utils.BaseTest
import views.html.errors.no_year_selected

class NoYearSelectedTest extends BaseTest {

  lazy val noYearSelected = instanceOf[no_year_selected]
  lazy val baseUserRequest: BaseUserRequest[?] = UserRequest(FakeRequest(), None, true, Some(""), true)
  override lazy val messages = Helpers.stubMessages()

  "noYearsSelected" should {
    "display correct h1" in {

      val doc = Jsoup.parse(noYearSelected()(messages, baseUserRequest).toString)
      val h1Tag = doc.getElementsByTag("h1").toString
      val expected = messages("pages.noyears.h1")

      h1Tag should include(expected)
    }

    "return the correct content" in {

      val doc = Jsoup.parse(noYearSelected()(messages, baseUserRequest).toString)
      val paragraphTag = doc.getElementsByTag("p").toString
      val expectedPart1 = messages("pages.noyears.findoutmore.part1")
      val expectedPart2 = messages("pages.noyears.findoutmore.link-text")

      paragraphTag should include(expectedPart1)
      paragraphTag should include(expectedPart2)

    }
  }

}

