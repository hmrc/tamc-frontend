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

package views.multiyear

import forms.MultiYearLowerEarnerForm
import models.auth.UserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import utils.BaseTest
import views.html.multiyear.lower_earner

class LowerEarnerTest extends BaseTest {

  implicit val request: UserRequest[_] = UserRequest(FakeRequest(), None, true, None, false)
  lazy val multiYearLowerEarnerForm = instanceOf[MultiYearLowerEarnerForm].lowerEarnerForm
  lazy val lowerEarnerView = instanceOf[lower_earner]
  lazy val view = lowerEarnerView(multiYearLowerEarnerForm)

  "Lower Earner Test" should {

    "return correct title on the lower earner page" in {

      val document = Jsoup.parse(view.toString())
      val title = document.title()
      val expected = messages("")

      println(s"TEST DOCUMENT $document DOCUMENT TEST")
      println(s"TEST TITLE $title TITLE TEST")

      title should include(expected)

    }
  }
}
