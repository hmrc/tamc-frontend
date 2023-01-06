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

package views.coc

import models.MarriageAllowanceEndingDates
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import views.html.coc.cancel

class CancelTest extends BaseTest {

  lazy val cancel = instanceOf[cancel]
  lazy val marriageAllowanceEndingDates = instanceOf[MarriageAllowanceEndingDates]

  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))


  "cancel" should {
    "return the correct title" in {

      val document = Jsoup.parse(cancel(marriageAllowanceEndingDates).toString)
      val title = document.title()
      val expected = messages("title.pattern", messages("pages.cancel.title"))

      title shouldBe expected
    }

    "return We will cancel your Marriage Allowance content" in {

      val document = Jsoup.parse(cancel(marriageAllowanceEndingDates).toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expectedPart1 = messages("pages.cancel.paragraph1")
      val expectedPart2 = messages("pages.cancel.paragraph2")

      paragraphTag should include(expectedPart1)
      paragraphTag should include(expectedPart2)

    }
  }

}
