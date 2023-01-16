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

import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import viewModels.ClaimsViewModel
import views.html.coc.claims

class ClaimsTest extends BaseTest {

  lazy val claims = instanceOf[claims]
  lazy val claimViewModel = instanceOf[ClaimsViewModel]

  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))



  "claims" should {
    "return the correct title" in {

      val document = Jsoup.parse(claims(claimViewModel).toString)
      val title = document.title()
      val expected = messages("title.application.pattern", messages("title.date-of-marriage"))

      title shouldBe expected
    }
  }
}
