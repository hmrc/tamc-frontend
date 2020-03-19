/*
 * Copyright 2020 HM Revenue & Customs
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

package viewModels

import java.util.Locale

import _root_.config.ApplicationConfig
import models.DesRelationshipEndReason.{Active, Cancelled, Closed, Death, Default, Divorce, Hmrc, InvalidParticipant, Merger, Rejected, Retrospective, System}
import models.{DesRelationshipEndReason, Recipient, RelationshipRecord, RelationshipRecords}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest
import views.helpers.TextGenerator


class ClaimsViewModelTest extends TamcViewModelTest {

  lazy val currentOfTaxYear: Int = TaxYear.current.currentYear
  lazy val endOfTaxYear: LocalDate = TaxYear.current.finishes
  lazy val formattedEndOfTaxYear: String = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

  lazy val taxFreeHtml: Html =
    Html(
      s"""${messagesApi("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messagesApi("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)

  val now = LocalDate.now()
  val dateInputPattern = "yyyyMMdd"

  def createRelationshipRecord(creationTimeStamp: LocalDate = now.minusDays(1),
                               participant1StartDate: LocalDate = now.minusDays(1),
                               relationshipEndReason: Option[DesRelationshipEndReason] = Some(DesRelationshipEndReason.Default),
                               participant1EndDate: Option[String] = None,
                               otherParticipantUpdateTimestamp: LocalDate = now.minusDays(1)): RelationshipRecord = {
    RelationshipRecord(
      Recipient.value,
      creationTimeStamp.toString(dateInputPattern),
      participant1StartDate.toString(dateInputPattern),
      relationshipEndReason,
      participant1EndDate,
      otherParticipantInstanceIdentifier = "1",
      otherParticipantUpdateTimestamp.toString(dateInputPattern))
  }

  "ClaimsViewModel" should {

    "create a ClaimsViewModel with active claims rows" in {

      val primaryActiveRecord = createRelationshipRecord()
      val viewModel = ClaimsViewModel(primaryActiveRecord, Seq.empty[RelationshipRecord])
      val dateInterval = TextGenerator().taxDateIntervalString(primaryActiveRecord.participant1StartDate, None)
      val activeRow = ClaimsRow(dateInterval, messagesApi("change.status.active"))

      viewModel.activeRow shouldBe activeRow
      viewModel.historicRows shouldBe Seq.empty[ClaimsRow]
      viewModel.taxFreeAllowanceLink shouldBe taxFreeHtml

    }

    "create a ClaimsViewModel with historic claims rows" when {

      val primaryActiveRecord = createRelationshipRecord()
      val creationTimeStamp = now.minusDays(2)
      val participant1StartDate = now.minusDays(2)
      val participant1EndDate = now.minusDays(1)
      lazy val dateInterval = TextGenerator().taxDateIntervalString(participant1StartDate.toString(dateInputPattern),
        Some(participant1EndDate.toString(dateInputPattern)))

      val endReasons = Set(
        Death, Divorce, InvalidParticipant, Cancelled, Rejected, Hmrc, Closed, Merger, Retrospective, System, Default
      )

      endReasons.foreach { endReason =>
        s"endRelationship reason is $endReason" in {

          val historicNonPrimaryRecord = createRelationshipRecord(creationTimeStamp, participant1StartDate,
            Some(endReason), Some(participant1EndDate.toString(dateInputPattern)))

          val viewModel = ClaimsViewModel(primaryActiveRecord, Seq(historicNonPrimaryRecord))
          val expectedStatus = getReason(historicNonPrimaryRecord)
          val expectedHistoricClaimsRow = ClaimsRow(dateInterval, expectedStatus)

          viewModel.historicRows shouldBe Seq(expectedHistoricClaimsRow)
        }
      }
    }

    "create a ClaimsViewModel which has an ordered historic non primary sequence of claims" in {

      val primaryActiveRecord = createRelationshipRecord()

      val cyMinusOneStartDate = now.minusYears(1)
      val cyMinusOneEndDate = now.minusYears(1)
      val expectedClaimsRowMinusOne = getDateRange(cyMinusOneStartDate, cyMinusOneEndDate)

      val cyMinusTwoStartDate = now.minusYears(2)
      val cyMinusTwoEndDate = now.minusYears(2)
      val expectedClaimsRowMinusTwo = getDateRange(cyMinusTwoStartDate, cyMinusTwoEndDate)

      val cyMinusThreeStartDate = now.minusYears(3)
      val cyMinusThreeEndDate = now.minusYears(3)
      val expectedClaimsRowMinusThree = getDateRange(cyMinusThreeStartDate, cyMinusThreeEndDate)

      val endReason = Divorce

      val cyMinusOneRecord = createRelationshipRecord(participant1StartDate = cyMinusOneStartDate,
        participant1EndDate = Some(cyMinusOneEndDate.toString(dateInputPattern)), relationshipEndReason = Some(endReason))

      val cyMinusTwoRecord = createRelationshipRecord(participant1StartDate = cyMinusTwoStartDate,
        participant1EndDate = Some(cyMinusTwoEndDate.toString(dateInputPattern)), relationshipEndReason = Some(endReason))

      val cyMinusThreeRecord = createRelationshipRecord(participant1StartDate = cyMinusThreeStartDate,
        participant1EndDate = Some(cyMinusThreeEndDate.toString(dateInputPattern)), relationshipEndReason = Some(endReason))

      val viewModel = ClaimsViewModel(primaryActiveRecord, Seq(cyMinusTwoRecord, cyMinusThreeRecord, cyMinusOneRecord))
      val expectedStatus = messagesApi(s"coc.end-reason.${endReason.value}")
      val orderedClaimRows = Seq(ClaimsRow(expectedClaimsRowMinusThree ,expectedStatus), ClaimsRow(expectedClaimsRowMinusTwo, expectedStatus),
        ClaimsRow(expectedClaimsRowMinusOne, expectedStatus))

      viewModel.historicRows shouldBe orderedClaimRows
    }

    "create a historic claims row" when {
      "there is no historic end reason set" in {

        val primaryActiveRecord = createRelationshipRecord()

        val cyMinusOneStartDate = now.minusYears(1)
        val cyMinusOneEndDate = now.minusYears(1)
        val expectedClaimsRowMinusOne = getDateRange(cyMinusOneStartDate, cyMinusOneEndDate)

        val cyMinusOneRecord = createRelationshipRecord(participant1StartDate = cyMinusOneStartDate,
          participant1EndDate = Some(cyMinusOneEndDate.toString(dateInputPattern)), relationshipEndReason = None)

        val viewModel = ClaimsViewModel(primaryActiveRecord, Seq(cyMinusOneRecord))
        val expectedStatus = messagesApi("coc.end-reason.DEFAULT")
        val claimsRows = Seq(ClaimsRow(expectedClaimsRowMinusOne , expectedStatus))

        viewModel.historicRows shouldBe claimsRows

      }
    }

  }

  private def getReason(record: RelationshipRecord)(implicit messages: Messages): String = {
    record.relationshipEndReason match {
      case Some(endReason) => messages(s"coc.end-reason.${endReason.value}")
      case _ => ""
    }
  }

  private def getDateRange(startDate: LocalDate, endDate: LocalDate) = {
    TextGenerator().taxDateIntervalString(startDate.toString(dateInputPattern), Some(endDate.toString(dateInputPattern)))
  }

}