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

import config.{ApplicationConfig, ApplicationGlobal}
import models.{Active, RecordStatus, RelationshipRecord}
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.LanguageUtils
import views.helpers.TextGenerators

//TODO add tests for active row
case class ActiveRow(activeDateInterval: String, activeStatus: String)

object ActiveRow {

  def apply(relationshipRecord: RelationshipRecord)(implicit messages: Messages): ActiveRow = {

    val activeDateInterval = TextGenerators.taxDateIntervalString(
      relationshipRecord.participant1StartDate,
      isWelsh = LanguageUtils.isWelsh(messages))
    val activeStatus = messages("change.status.active")

    ActiveRow(activeDateInterval, activeStatus)

  }
}

//TODO add tests for historic row
case class HistoricRow(historicDateInterval: String, historicStatus: String)

object HistoricRow {

  def apply(relationshipRecord: RelationshipRecord)(implicit messages: Messages): HistoricRow = {

    val historicDateInterval = TextGenerators.taxDateIntervalString(
      relationshipRecord.participant1StartDate,
      relationshipRecord.participant1EndDate,
      LanguageUtils.isWelsh(messages))

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

case class ClaimsViewModel(activeRow: Option[ActiveRow],
                           historicRows: Option[Seq[HistoricRow]],
                           isActiveRecord: Boolean,
                           taxFreeAllowanceLink: Html,
                           backLinkUrl: String)

object ClaimsViewModel {

  def apply(activeRelationship: Option[RelationshipRecord],
            historicRelationships: Option[Seq[RelationshipRecord]],
            recordStatus: RecordStatus)(implicit messages: Messages): ClaimsViewModel = {

    val activeRow = activeRelationship map (ActiveRow(_))
    val historicRows = historicRelationships map { historicRelationships =>
      historicRelationships map (HistoricRow(_))
    }

    val isActiveRecord = recordStatus == Active

    ClaimsViewModel(activeRow, historicRows, isActiveRecord, taxFreeAllowanceLink, backLinkUrl(recordStatus))
  }

  private def taxFreeAllowanceLink(implicit messages: Messages): Html = {
    Html(
      s"""${messages("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messages("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)
  }

  private def backLinkUrl(recordStatus: RecordStatus): String = {
    recordStatus match {
      case Active =>
        controllers.routes.UpdateRelationshipController.decision().url
      case _ =>
        controllers.routes.UpdateRelationshipController.history().url
    }
  }
}
