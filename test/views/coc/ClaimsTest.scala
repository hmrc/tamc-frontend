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

import models.RelationshipRecord
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.BaseTest
import viewModels.ClaimsViewModelImpl
import views.html.coc.claims

class ClaimsTest extends BaseTest {

  lazy val claims = instanceOf[claims]
  lazy val claimViewModel = instanceOf[ClaimsViewModelImpl]
  lazy val relationshipRecord = RelationshipRecord("Recipient","creationTimestamp","20220101", None, None,"otherPaticipant","otherParticipantupdateTimestamp")
  lazy val relationshipRecord1 = RelationshipRecord("Recipient","creationTimestamp","20220101", None, None,"otherPaticipant","otherParticipantupdateTimestamp")

  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))

  "claims" should {
    "return the correct title" in {

      val document = Jsoup.parse(claims(claimViewModel(relationshipRecord, Seq(relationshipRecord1))).toString)
      val title = document.title()
      val expected = messages("title.pattern", messages("title.date-of-marriage"))

      title shouldBe expected
    }

    "return Your Marriage Allowance claims h1" in {

      val document = Jsoup.parse(claims(claimViewModel(relationshipRecord, Seq(relationshipRecord1))).toString)
      val h1Tag = document.getElementsByTag("h1").toString
      val expected = messages("pages.claims.title")

      h1Tag should include(expected)
    }
  }
}
