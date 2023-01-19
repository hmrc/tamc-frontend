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

package views.multiyear

import models.auth.UserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import utils.BaseTest
import forms.MultiYearEligibilityCheckForm
import views.html.multiyear.eligibility_check

class EligibilityCheckTest extends BaseTest {

  lazy val eligibilityCheck = instanceOf[eligibility_check]
  implicit val request: UserRequest[_] = UserRequest(FakeRequest(), None, true, None, false)
  lazy val eligibilityCheckForm = MultiYearEligibilityCheckForm.eligibilityForm


  "Eligibility Check page" should {
    "return correct page title of how it works page" in {

      val document = Jsoup.parse(eligibilityCheck(eligibilityCheckForm).toString())
      val title = document.title()
      val expected = messages("title.eligibility.pattern", messages("eligibility.check.h1"))

      title shouldBe expected

    }
    "display you can claim marriage allowance if partner has died content" in {

      val document = Jsoup.parse(eligibilityCheck(eligibilityCheckForm).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("eligibility.check.married.error2")

      paragraphTag should include(expected)

    }
    "display you are not eligible for married allowance content" in {

      val document = Jsoup.parse(eligibilityCheck(eligibilityCheckForm).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("eligibility.check.married.error1")

      paragraphTag should include(expected)

    }
  }


}
