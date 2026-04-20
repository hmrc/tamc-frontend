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
import views.html.errors.transferor_deceased

class TransferorDeceasedTest extends BaseTest {

  lazy val transferorDeceased = instanceOf[transferor_deceased]
  lazy val baseUserRequest: BaseUserRequest[?] = UserRequest(FakeRequest(), None, true, Some(""), true)
  override lazy val messages = Helpers.stubMessages()


  "transferorDeceased" should {
    "return correct page title of transferorDeceased page" in {

      val document = Jsoup.parse(transferorDeceased()(messages, baseUserRequest).toString)
      val title = document.title()
      val expected = messages("title.cannot-use-service") + " - " + messages("title.pattern")

      title shouldBe expected

    }

    "return you can apply for marriage allowance from PTA content" in {

      val document = Jsoup.parse(transferorDeceased()(messages, baseUserRequest).toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expectedPart1 = messages("technical.transferor-dead-p1")
      val expectedPart2 = messages("technical.transferor-dead-p2")

      paragraphTag should include(expectedPart1)
      paragraphTag should include(expectedPart2)

    }
  }

}
