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
import services.TimeService.parseDateWithFormat
import utils.DateUtils

case class RelationshipRecordWrapper(
                                      relationships: Seq[RelationshipRecord],
                                      userRecord: Option[LoggedInUserInfo] = None){

  def activeRelationship: Option[RelationshipRecord] = relationships.find(_.isActive)

  def historicRelationships: Option[Seq[RelationshipRecord]] = {
    if (relationships.size > 1 && relationships.head.participant1EndDate.isEmpty) {
      Some(relationships.tail)
    } else if (relationships.nonEmpty && relationships.head.participant1EndDate.isDefined) {
      Some(relationships)
    } else None
  }
}


case class RelationshipRecord(
                               participant: String,
                               creationTimestamp: String,
                               participant1StartDate: String,
                               relationshipEndReason: Option[RelationshipEndReason] = None,
                               participant1EndDate: Option[String] = None,
                               otherParticipantInstanceIdentifier: String,
                               otherParticipantUpdateTimestamp: String){

  def isActive:Boolean = participant1EndDate match{
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

case class RelationshipRecordList(
                                   activeRelationship: Option[RelationshipRecord] = None,
                                   historicRelationships: Option[Seq[RelationshipRecord]] = None,
                                   LoggedInUserInfo: Option[LoggedInUserInfo] = None,
                                   activeRecord: Boolean,
                                   historicRecord: Boolean,
                                   historicActiveRecord: Boolean) {

  def this(
            activeRelationship: Option[RelationshipRecord],
            historicRelationships: Option[Seq[RelationshipRecord]],
            loggedInUserInfo: Option[LoggedInUserInfo]) = this(activeRelationship,
    historicRelationships, loggedInUserInfo,
    activeRecord = activeRelationship.isDefined && activeRelationship.get.participant1EndDate.isEmpty,
    historicRecord = historicRelationships.isDefined,
    historicActiveRecord = activeRelationship.isDefined && activeRelationship.get.participant1EndDate.isDefined
  )
}

object RelationshipRecord {
  implicit val formats = Json.format[RelationshipRecord]
}

object Role {
  val TRANSFEROR = "Transferor"
  val RECIPIENT = "Recipient"
}

object EndReasonCode {
  val CANCEL = "CANCEL"
  val REJECT = "REJECT"
  val DIVORCE = "DIVORCE"
  val DIVORCE_CY = "DIVORCE_CY"
  val DIVORCE_PY = "DIVORCE_PY"
  val EARNINGS = "EARNINGS"
  val BEREAVEMENT = "BEREAVEMENT"
}

object RelationshipRecordWrapper {
  implicit val formats = Json.format[RelationshipRecordWrapper]
}
