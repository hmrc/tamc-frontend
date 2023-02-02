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
import views.html.errors.no_tax_year_transferor

class NoTaxYearTransferor extends BaseTest {

  lazy val noTaxYearTransferor = instanceOf[no_tax_year_transferor]
  lazy val baseUserRequest: BaseUserRequest[_] = UserRequest(FakeRequest(), None, true, Some(""), true)
  override lazy val messages = Helpers.stubMessages()

  "noTaxYearTransferor" should {
    "return the correct title" in {
      val doc = Jsoup.parse(noTaxYearTransferor()(messages, baseUserRequest).toString)
      val title = doc.title()
      val expected = messages("title.pattern", messages("title.no-tax-years"))

      title shouldBe expected
    }

    "return Your Marriage Allowance claims h1" in {

      val doc = Jsoup.parse(noTaxYearTransferor()(messages, baseUserRequest).toString)
      val h1Tag = doc.getElementsByTag("h1").toString
      val expected = messages("eligibility.check.header")

      h1Tag should include(expected)
    }


    "return We will cancel your Marriage Allowance content" in {

      val doc = Jsoup.parse(noTaxYearTransferor()(messages, baseUserRequest).toString)
      val paragraphTag = doc.getElementsByTag("p").toString
      val expected = messages("transferor.no-previous-years-available")

      paragraphTag should include(expected)

    }
  }


}
