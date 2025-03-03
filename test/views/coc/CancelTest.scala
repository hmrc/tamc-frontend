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
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import views.html.coc.cancel

class CancelTest extends BaseTest with NinoGenerator {

  lazy val cancelling = instanceOf[cancel]
  lazy val nino: String = generateNino().nino

  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))


  "cancel" should {
    "return the correct title" in {

      val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val document = Jsoup.parse(cancelling(expectedDates).toString)
      val title = document.title()
      val expected = messages("pages.cancel.title") + " - " + messages("title.pattern")

      title shouldBe expected
    }

    "return We will cancel your Marriage Allowance content" in {

      val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val document = Jsoup.parse(cancelling(expectedDates).toString)
      val paragraphTag = document.getElementsByTag("p").toString
      val expectedPart1 = messages("pages.cancel.paragraph1")
      val expectedPart2 = messages("pages.cancel.paragraph2")

      paragraphTag should include(expectedPart1)
      paragraphTag should include(expectedPart2)

    }
  }

}
