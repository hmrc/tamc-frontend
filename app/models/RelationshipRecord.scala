/*
 * Copyright 2018 HM Revenue & Customs
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

case class RelationshipRecordWrapper(
  relationships: Seq[RelationshipRecord],
  userRecord: Option[LoggedInUserInfo] = None)

case class RelationshipRecord(
  participant: String,
  creationTimestamp: String,
  participant1StartDate: String,
  relationshipEndReason: Option[String] = None,
  participant1EndDate: Option[String] = None,
  otherParticipantInstanceIdentifier: String,
  otherParticipantUpdateTimestamp: String)

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
    if (activeRelationship != None && activeRelationship.get.participant1EndDate == None) true else false,
    if (historicRelationships != None) true else false,
    if (activeRelationship != None && activeRelationship.get.participant1EndDate != None) true else false)
}

object RelationshipRecordList {

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
