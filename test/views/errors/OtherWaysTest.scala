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

import org.jsoup.Jsoup
import utils.BaseTest
import views.html.errors.other_ways

class OtherWaysTest extends BaseTest {

  lazy val otherWays = instanceOf[other_ways]


  "otherWays" should {

    "return correct page title of otherWays page" in {

      val document = Jsoup.parse(otherWays.toString)
      val title = document.title()
      val expected = messages("title.pattern", messages("technical.other-ways.h1"))

      title shouldBe expected

    }

    "return you can apply for marriage allowance from PTA content" in {

      val document = Jsoup.parse(otherWays.toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expectedPart1 = messages("technical.other-ways.para0.part1")
      val expectedLink = messages("technical.other-ways.para0.link-text")
      val expectedPart2 = messages("technical.other-ways.para0.part2")

      paragraphTag should include(expectedPart1)
      paragraphTag should include(expectedLink)
      paragraphTag should include(expectedPart2)

    }

    "return you can call hmrc to make an application content" in {

      val document = Jsoup.parse(otherWays.toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expectedPart1 = messages("technical.other-ways.para1")
      val expectedPart2 = messages("technical.other-ways.para2")

      paragraphTag should include(expectedPart1)
      paragraphTag should include(expectedPart2)

    }

  }

}
