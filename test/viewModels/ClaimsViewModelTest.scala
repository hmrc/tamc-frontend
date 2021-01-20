/*
 * Copyright 2021 HM Revenue & Customs
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
import models.DesRelationshipEndReason.{Cancelled, Closed, Death, Default, Divorce, Hmrc, InvalidParticipant, Merger, Rejected, Retrospective, System}
import models._
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest
import views.helpers.LanguageUtils


class ClaimsViewModelTest extends TamcViewModelTest {

  lazy val currentOfTaxYear: Int = TaxYear.current.currentYear
  lazy val endOfTaxYear: LocalDate = TaxYear.current.finishes
  lazy val formattedEndOfTaxYear: String = endOfTaxYear.format(DateTimeFormatter.ofPattern("d MMMM yyyy").withLocale(Locale.UK))

  lazy val taxFreeHtml: Html =
    Html(
      s"""${messagesApi("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.appConfig.taxFreeAllowanceUrl}">
         |${messagesApi("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)

  val now = LocalDate.now()
  val dateInputPattern = "yyyyMMdd"

  def createRelationshipRecord(creationTimeStamp: LocalDate = now.minusDays(1),
                               participant1StartDate: LocalDate = now.minusDays(1),
                               relationshipEndReason: Option[DesRelationshipEndReason] = Some(Default),
                               participant1EndDate: Option[LocalDate] = None,
                               otherParticipantUpdateTimestamp: LocalDate = now.minusDays(1)): RelationshipRecord = {
    RelationshipRecord(
      Transferor.value,
      creationTimeStamp.format(DateTimeFormatter.ofPattern(dateInputPattern)),
      participant1StartDate.format(DateTimeFormatter.ofPattern(dateInputPattern)),
      relationshipEndReason,
      participant1EndDate.map(_.format(DateTimeFormatter.ofPattern(dateInputPattern))),
      otherParticipantInstanceIdentifier = "1",
      otherParticipantUpdateTimestamp.format(DateTimeFormatter.ofPattern(dateInputPattern)))
  }

  def createExpectedClaimRow(recordStartDate: LocalDate, recordEndDate: LocalDate, relationshipRecord: RelationshipRecord): ClaimsRow = {
    val expectedClaimsRow1DateRange = getDateRange(recordStartDate, recordEndDate)
    val expectedEndReason1 = getReason(relationshipRecord)

    ClaimsRow(expectedClaimsRow1DateRange ,expectedEndReason1)
  }

  "ClaimsViewModel" should {

    "create a ClaimsViewModel with active claims rows" in {

      val primaryActiveRecord = createRelationshipRecord()
      val viewModel = ClaimsViewModel(primaryActiveRecord, Seq.empty[RelationshipRecord])
      val dateInterval = LanguageUtils().taxDateIntervalString(primaryActiveRecord.participant1StartDate, None)
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
      lazy val dateInterval = LanguageUtils().taxDateIntervalString(participant1StartDate.format(DateTimeFormatter.ofPattern(dateInputPattern)),
        Some(participant1EndDate.format(DateTimeFormatter.ofPattern(dateInputPattern))))

      val endReasons = Set(
        Death, Divorce, InvalidParticipant, Cancelled, Rejected, Hmrc, Closed, Merger, Retrospective, System, Default
      )

      endReasons.foreach { endReason =>
        s"endRelationship reason is $endReason" in {

          val historicNonPrimaryRecord = createRelationshipRecord(creationTimeStamp, participant1StartDate,
            Some(endReason), Some(participant1EndDate))

          val viewModel = ClaimsViewModel(primaryActiveRecord, Seq(historicNonPrimaryRecord))
          val expectedStatus = getReason(historicNonPrimaryRecord)
          val expectedHistoricClaimsRow = ClaimsRow(dateInterval, expectedStatus)

          viewModel.historicRows shouldBe Seq(expectedHistoricClaimsRow)
        }
      }
    }

    "create a ClaimsViewModel which has an ordered historic non primary sequence of claims" in {

      val primaryActiveRecord = createRelationshipRecord()

      val previousClaims1CreationDate = now.minusYears(1)
      val previousClaims1StartDate = previousClaims1CreationDate
      val previousClaims1EndDate = previousClaims1CreationDate.plusMonths(8)

      val previousClaims2CreationDate = now.minusYears(2)
      val previousClaims2StartDate = previousClaims2CreationDate
      val previousClaims2EndDate = previousClaims2CreationDate.plusMonths(3)

      val previousClaims3CreationDate = now minusYears(3)
      val previousClaims3StartDate = previousClaims3CreationDate
      val previousClaims3EndDate = previousClaims3CreationDate.plusMonths(4)

      val previousClaimRecordMinus1 = createRelationshipRecord(creationTimeStamp = previousClaims1CreationDate,
        participant1StartDate = previousClaims1StartDate, participant1EndDate = Some(previousClaims1EndDate),
        relationshipEndReason = Some(Divorce))

      val previousClaimRecordMinus2 = createRelationshipRecord(creationTimeStamp = previousClaims2CreationDate,
        participant1StartDate = previousClaims2StartDate, participant1EndDate = Some(previousClaims2EndDate),
        relationshipEndReason = Some(Cancelled))

      val previousClaimRecordMinus3 = createRelationshipRecord(creationTimeStamp = previousClaims3CreationDate,
        participant1StartDate = previousClaims3StartDate, participant1EndDate = Some(previousClaims3EndDate))

      val historicSequenceUnordered = Seq(previousClaimRecordMinus2, previousClaimRecordMinus1, previousClaimRecordMinus3)

      val expectedClaimsRow1 = createExpectedClaimRow(previousClaims1StartDate, previousClaims1EndDate, previousClaimRecordMinus1)
      val expectedClaimsRow2 = createExpectedClaimRow(previousClaims2StartDate, previousClaims2EndDate, previousClaimRecordMinus2)
      val expectedClaimsRow3 = createExpectedClaimRow(previousClaims3StartDate, previousClaims3EndDate, previousClaimRecordMinus3)

      val orderedClaimRows = Seq(expectedClaimsRow1, expectedClaimsRow2, expectedClaimsRow3)

      ClaimsViewModel(primaryActiveRecord, historicSequenceUnordered).historicRows shouldBe orderedClaimRows
    }

    "create a historic claims row" when {
      "there is no historic end reason set" in {

        val primaryActiveRecord = createRelationshipRecord()

        val cyMinusOneStartDate = now.minusYears(1)
        val cyMinusOneEndDate = now.minusYears(1)
        val expectedClaimsRowMinusOne = getDateRange(cyMinusOneStartDate, cyMinusOneEndDate)

        val cyMinusOneRecord = createRelationshipRecord(participant1StartDate = cyMinusOneStartDate,
          participant1EndDate = Some(cyMinusOneEndDate), relationshipEndReason = None)

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
    LanguageUtils().taxDateIntervalString(startDate.format(DateTimeFormatter.ofPattern(dateInputPattern)),
      Some(endDate.format(DateTimeFormatter.ofPattern(dateInputPattern))))
  }

}