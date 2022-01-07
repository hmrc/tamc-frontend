/*
 * Copyright 2022 HM Revenue & Customs
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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import play.api.libs.json.Json

case class RelationshipRecord(participant: String,
                              creationTimestamp: String,
                              participant1StartDate: String,
                              relationshipEndReason: Option[DesRelationshipEndReason] = None,
                              participant1EndDate: Option[String] = None,
                              otherParticipantInstanceIdentifier: String,
                              otherParticipantUpdateTimestamp: String) {

  def isActive: Boolean = participant1EndDate match {
    case None => true
    case Some(date) => isFutureDate(parseDateWithFormat(date))
  }

  def isFutureDate(date: LocalDate): Boolean =
    date.isAfter(getCurrentDate)

  def getCurrentDate: LocalDate =
    LocalDate.now()

  def getCurrentTaxYear: Int =
    uk.gov.hmrc.time.TaxYear.current.startYear

  def getTaxYearForDate(date: LocalDate): Int =
    uk.gov.hmrc.time.TaxYear.taxYearFor(date).startYear

  def getStartDateForTaxYear(year: Int): LocalDate =
    uk.gov.hmrc.time.TaxYear.firstDayOfTaxYear(year)

  def getPreviousYearDate: LocalDate =
    LocalDate.now().minusYears(1)

  def parseDateWithFormat(date: String, format: String = "yyyyMMdd"): LocalDate =
    LocalDate.parse(date, DateTimeFormatter.ofPattern(format))


  val role: Role = Role(participant)

  def overlappingTaxYears: Set[Int] = {

    val taxYearOfRelationshipStart = getTaxYearForDate(parseDateWithFormat(participant1StartDate))
    val taxYearOfRelationshipEnd = participant1EndDate.fold(getCurrentTaxYear)(
      participant1EndDateAsString => {
        val participant1EndDate = parseDateWithFormat(participant1EndDateAsString)
        val taxYearOfParticipant1EndDate = getTaxYearForDate(participant1EndDate)
        val isParticipant1EndDateOnTheFirstDayOfTaxYear: Boolean = participant1EndDate == getStartDateForTaxYear(taxYearOfParticipant1EndDate)

        relationshipEndReason match {
          case Some(DesRelationshipEndReason.Divorce) if isParticipant1EndDateOnTheFirstDayOfTaxYear => taxYearOfParticipant1EndDate - 1
          case _ => taxYearOfParticipant1EndDate
        }
      })

    (taxYearOfRelationshipStart to taxYearOfRelationshipEnd).toSet
  }
}

object RelationshipRecord {
  implicit val formats = Json.format[RelationshipRecord]
}
