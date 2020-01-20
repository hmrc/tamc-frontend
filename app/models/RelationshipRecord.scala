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

package models

import play.api.libs.json.Json
import services.TimeService
import utils.DateUtils

case class RelationshipRecord(
                               participant: String,
                               creationTimestamp: String,
                               participant1StartDate: String,
                               relationshipEndReason: Option[RelationshipEndReason] = None,
                               participant1EndDate: Option[String] = None,
                               otherParticipantInstanceIdentifier: String,
                               otherParticipantUpdateTimestamp: String) {

  def isActive: Boolean = participant1EndDate match{
    case None => true
    case Some(date) => DateUtils.isFutureDate(date)
  }

  def overlappingTaxYears: Set[Int] = {
    val timeService = TimeService

    val parseDate = timeService.parseDateWithFormat(_: String, format = DateUtils.DatePattern)

    val taxYearOfRelationshipStart = timeService.getTaxYearForDate(parseDate(participant1StartDate))
    val taxYearOfRelationshipEnd = participant1EndDate.fold(timeService.getCurrentTaxYear)(
      participant1EndDateAsString => {
        val participant1EndDate = parseDate(participant1EndDateAsString)
        val taxYearOfParticipant1EndDate = timeService.getTaxYearForDate(participant1EndDate)
        val isParticipant1EndDateOnTheFirstDayOfTaxYear: Boolean = participant1EndDate == timeService.getStartDateForTaxYear(taxYearOfParticipant1EndDate)

        relationshipEndReason match {
          case Some(RelationshipEndReason.Divorce) if isParticipant1EndDateOnTheFirstDayOfTaxYear => taxYearOfParticipant1EndDate - 1
          case _ => taxYearOfParticipant1EndDate
        }
      })

    (taxYearOfRelationshipStart to taxYearOfRelationshipEnd).toSet
  }
}

object RelationshipRecord {
  implicit val formats = Json.format[RelationshipRecord]
}
