/*
 * Copyright 2025 HM Revenue & Customs
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
import models.{RelationshipRecord, Transferor}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import viewModels.ClaimsViewModelImpl
import views.html.coc.claims

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ClaimsContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: claims = inject[claims]
  val claimsViewModelImpl: ClaimsViewModelImpl = instanceOf[ClaimsViewModelImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  val now: LocalDate = LocalDate.now()
  val dateInputPattern = "yyyyMMdd"

  val relationshipRecord: RelationshipRecord = RelationshipRecord(
    Transferor.value,
    LocalDate.now.minusDays(1).format(DateTimeFormatter.ofPattern(dateInputPattern)),
    LocalDate.now.minusDays(1).format(DateTimeFormatter.ofPattern(dateInputPattern)),
    Some(Default),
    None,
    otherParticipantInstanceIdentifier = "1",
    LocalDate.now.minusDays(1).format(DateTimeFormatter.ofPattern(dateInputPattern)))

  val doc: Document = Jsoup.parse(view(claimsViewModelImpl(relationshipRecord, Seq.empty[RelationshipRecord])).toString())

  "Claims view page" should {
    "Display claim page heading" in {
      doc.getElementById("pageHeading").text() shouldBe "Your Marriage Allowance claims"
    }

    "Display correct table headers" in {
      doc.getElementsByClass("govuk-table__header")
        .eachText()
        .toArray contains Array("Tax Year", "Status", s"${TaxYear.current.startYear} to Present")
    }

    "Display tax free allowance link" in {
      doc.getElementById("taxFreeAllowance").text() shouldBe "You can see the transferred allowance in your tax-free amount."
      doc.getElementById("taxFreeAllowanceLink").attr("href") shouldBe "http://localhost:9230/check-income-tax/tax-free-allowance"
    }
  }

}
