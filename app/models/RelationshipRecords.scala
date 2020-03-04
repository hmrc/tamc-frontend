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

import errors._
import play.api.Logger
import play.api.libs.json.Json

//TODO need to update this domain model once TAMC passes primary record through
case class RelationshipRecords(primaryRecord: RelationshipRecord, nonPrimaryRecords: Seq[RelationshipRecord],
                               loggedInUserInfo: LoggedInUserInfo) {

  def hasMarriageAllowanceBeenCancelled = primaryRecord.participant1EndDate.isDefined

  def recipientInformation: RecipientInformation = {
    primaryRecord.role match {
      case Transferor => RecipientInformation(primaryRecord.otherParticipantInstanceIdentifier, primaryRecord.otherParticipantUpdateTimestamp)
      case Recipient => RecipientInformation(loggedInUserInfo.cid.toString(), loggedInUserInfo.timestamp)
    }
  }

  def transferorInformation: TransferorInformation = {
    primaryRecord.role match {
      case Transferor => TransferorInformation(loggedInUserInfo.timestamp)
      case Recipient => TransferorInformation(primaryRecord.otherParticipantUpdateTimestamp)
    }
  }

}

//TODO this logic should live in TAMC
object RelationshipRecords {

  implicit val formats = Json.format[RelationshipRecords]

  val logger = Logger(this.getClass)

  def apply(relationshipRecordList: RelationshipRecordList): RelationshipRecords = {

    val relationships = relationshipRecordList.relationships
    val activeRecordCount = relationships.count(_.isActive)

    val primaryRecord: RelationshipRecord  = activeRecordCount match {
      case(0) => {
        logger.error("No active record found")
        throw NoPrimaryRecordError()
      }
      case(1) => relationships.find(_.isActive).head
      case multiple => {
        logger.error(s"$multiple active records found")
        throw MultipleActiveRecordError()
      }
    }

    val nonPrimaryRelationships = relationships.filterNot(_ == primaryRecord)

    val loggedInUser = relationshipRecordList.userRecord.getOrElse(throw CitizenNotFound())

    RelationshipRecords(primaryRecord, nonPrimaryRelationships, loggedInUser)
  }
}
