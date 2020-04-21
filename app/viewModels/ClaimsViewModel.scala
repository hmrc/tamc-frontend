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
import models.DesRelationshipEndReason.Default
import models.RelationshipRecord
import play.api.i18n.Messages
import play.twirl.api.Html
import views.helpers.LanguageUtils

case class ClaimsRow(dateInterval: String, status: String)

case class ClaimsViewModel(activeRow: ClaimsRow,
                           historicRows: Seq[ClaimsRow],
                           taxFreeAllowanceLink: Html)

object ClaimsViewModel {

  implicit val historicOrdering: Ordering[RelationshipRecord] = Ordering.by((_:RelationshipRecord).creationTimestamp).reverse

  def apply(primaryRelationship: RelationshipRecord,
            historicRelationships: Seq[RelationshipRecord])(implicit messages: Messages): ClaimsViewModel = {

    val activeRow = activeClaimsRow(primaryRelationship)
    val orderedHistoricRows = historicRelationships.sorted.map(historicClaimsRow(_))

    ClaimsViewModel(activeRow, orderedHistoricRows, taxFreeAllowanceLink)
  }

  private def taxFreeAllowanceLink(implicit messages: Messages): Html = {
    Html(
      s"""${messages("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messages("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)
  }

  private def activeClaimsRow(primaryRelationshipRecord: RelationshipRecord)(implicit messages: Messages): ClaimsRow = {

    val activeDateInterval = LanguageUtils().taxDateIntervalString(
      primaryRelationshipRecord.participant1StartDate,
      primaryRelationshipRecord.participant1EndDate)

    val activeStatus = messages("change.status.active")

    ClaimsRow(activeDateInterval, activeStatus)

  }

  private def historicClaimsRow(nonPrimaryRelation: RelationshipRecord)(implicit messages: Messages): ClaimsRow = {

    val historicDateInterval = LanguageUtils().taxDateIntervalString(
      nonPrimaryRelation.participant1StartDate,
      nonPrimaryRelation.participant1EndDate)

    val cause = nonPrimaryRelation.relationshipEndReason match {
      case None => Default.value
      case Some(reason) => reason.value
    }

    val status = messages(s"coc.end-reason.${cause.toUpperCase}")

    ClaimsRow(historicDateInterval, status)
  }

}
