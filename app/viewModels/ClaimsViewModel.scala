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
case class ActiveRow(activeDateInterval: String, activeStatus: String)

object ActiveRow {

  def apply(relationshipRecord: RelationshipRecord)(implicit messages: Messages): ActiveRow = {

    val activeDateInterval = TextGenerator().taxDateIntervalString(
      relationshipRecord.participant1StartDate,
      relationshipRecord.participant1EndDate)
    val activeStatus = messages("change.status.active")

    ActiveRow(activeDateInterval, activeStatus)

  }
}

//TODO add tests for historic row
case class HistoricRow(historicDateInterval: String, historicStatus: String)

object HistoricRow {

  def apply(relationshipRecord: RelationshipRecord)(implicit messages: Messages): HistoricRow = {

    val historicDateInterval = TextGenerator().taxDateIntervalString(
      relationshipRecord.participant1StartDate,
      relationshipRecord.participant1EndDate)

    val cause = relationshipRecord.relationshipEndReason match {
      case None => ""
      case Some(reason) => reason.value.toUpperCase
    }

    //TODO get or else should be frm DEFAULT value not empty with will fail with runtime exception!?!?!?
    val status = if (cause == "") {
      //TODO to test this thing?
      ""
    } else {
      val messageKey = s"coc.end-reason.$cause"
      messages(messageKey)
    }

    HistoricRow(historicDateInterval, status)
  }

}

case class ClaimsViewModel(activeRow: ActiveRow,
                           historicRows: Seq[HistoricRow],
                           taxFreeAllowanceLink: Html,
                           backLinkUrl: String)

object ClaimsViewModel {

  def apply(activeRelationship: RelationshipRecord,
            historicRelationships: Seq[RelationshipRecord])(implicit messages: Messages): ClaimsViewModel = {

    val activeRow = ActiveRow(activeRelationship)
    val historicRows = historicRelationships.map(HistoricRow(_))


    ClaimsViewModel(activeRow, historicRows, taxFreeAllowanceLink, backLinkUrl)
  }

  private def taxFreeAllowanceLink(implicit messages: Messages): Html = {
    Html(
      s"""${messages("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messages("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)
  }

  //TODO implement browser back functionality so we can remove this
  private def backLinkUrl: String = controllers.routes.UpdateRelationshipController.history().url
}
