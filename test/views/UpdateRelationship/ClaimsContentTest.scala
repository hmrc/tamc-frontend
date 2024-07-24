/*
 * Copyright 2024 HM Revenue & Customs
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

package views.UpdateRelationship

import models.DesRelationshipEndReason.Default
import models.auth.AuthenticatedUserRequest
import models.{DesRelationshipEndReason, RelationshipRecord, Transferor}
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import views.html.coc.claims

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClaimsContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: claims = inject[claims]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  val now: LocalDate = LocalDate.now()
  val dateInputPattern = "yyyyMMdd"

  def createRelationshipRecord(creationTimeStamp: LocalDate = LocalDate.now.minusDays(1),
                               participant1StartDate: LocalDate = LocalDate.now.minusDays(1),
                               relationshipEndReason: Option[DesRelationshipEndReason] = Some(Default),
                               participant1EndDate: Option[LocalDate] = None,
                               otherParticipantUpdateTimestamp: LocalDate = LocalDate.now.minusDays(1)): RelationshipRecord = {
    RelationshipRecord(
      Transferor.value,
      creationTimeStamp.format(DateTimeFormatter.ofPattern(dateInputPattern)),
      participant1StartDate.format(DateTimeFormatter.ofPattern(dateInputPattern)),
      relationshipEndReason,
      participant1EndDate.map(_.format(DateTimeFormatter.ofPattern(dateInputPattern))),
      otherParticipantInstanceIdentifier = "1",
      otherParticipantUpdateTimestamp.format(DateTimeFormatter.ofPattern(dateInputPattern)))
  }

  "Claims view page" should {
    "Display claim page heading" in {
      val primaryActiveRecord = createRelationshipRecord()
      //val document = Jsoup.parse(view(ClaimsViewModel(primaryActiveRecord, Seq.empty[RelationshipRecord])))

    }
  }

}
