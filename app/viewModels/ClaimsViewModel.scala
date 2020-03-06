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

import config.ApplicationConfig
import models.RelationshipRecord
import play.api.i18n.Messages
import play.twirl.api.Html
import views.helpers.TextGenerator

//TODO add tests for active row
case class ClaimsRow(dateInterval: String, status: String)

case class ClaimsViewModel(activeRow: ClaimsRow,
                           historicRows: Seq[ClaimsRow],
                           taxFreeAllowanceLink: Html,
                           backLinkUrl: String)

object ClaimsViewModel {

  implicit val ordering: Ordering[RelationshipRecord] = Ordering.by(_.participant1StartDate)

  def apply(primaryRelationship: RelationshipRecord,
            historicRelationships: Seq[RelationshipRecord])(implicit messages: Messages): ClaimsViewModel = {

    //TODO could push to HOF if ordering could be dictated
    val activeRow = activeClaimsRow(primaryRelationship)
    val orderedHistoricRows = historicRelationships.sorted.map(historicClaimsRow(_))

    ClaimsViewModel(activeRow, orderedHistoricRows, taxFreeAllowanceLink, backLinkUrl)
  }

  private def taxFreeAllowanceLink(implicit messages: Messages): Html = {
    Html(
      s"""${messages("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messages("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)
  }

  private def activeClaimsRow(primaryRelationshipRecord: RelationshipRecord)(implicit messages: Messages): ClaimsRow = {

    val activeDateInterval = TextGenerator().taxDateIntervalString(
      primaryRelationshipRecord.participant1StartDate,
      primaryRelationshipRecord.participant1EndDate)

    val activeStatus = messages("change.status.active")

    ClaimsRow(activeDateInterval, activeStatus)

  }

  private def historicClaimsRow(nonPrimaryRelation: RelationshipRecord)(implicit messages: Messages): ClaimsRow = {

    val historicDateInterval = TextGenerator().taxDateIntervalString(
      nonPrimaryRelation.participant1StartDate,
      nonPrimaryRelation.participant1EndDate)

    //TODO shared RelationshipRecord domain across primary and non primary should be changed to cater for this option
    val cause = nonPrimaryRelation.relationshipEndReason match {
      case None => ""
      case Some(reason) => reason.value.toUpperCase
    }

    val status = if (cause == "") {
      ""
    } else {
      val messageKey = s"coc.end-reason.$cause"
      messages(messageKey)
    }

    ClaimsRow(historicDateInterval, status)

  }

  //TODO implement browser back functionality so we can remove this
  private def backLinkUrl: String = ""
}
