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

case class RelationshipRecords(activeRelationship: Option[RelationshipRecord],
                               historicRelationships: Option[Seq[RelationshipRecord]],
                               loggedInUserInfo: Option[LoggedInUserInfo] = None){

  val recordStatus: RecordStatus = {

    (activeRelationship, historicRelationships) match {
      case(Some(RelationshipRecord(_, _, _, _, endDate, _, _)), _) if endDate.isEmpty => Active
      case(Some(RelationshipRecord(_, _, _, _, endDate, _, _)), _) if endDate.isDefined => ActiveHistoric
      case(_, _) => Historic
    }

  }

  val role: Role = {
    (activeRelationship, historicRelationships) match {
      case(Some(RelationshipRecord("Transferor", _, _, _, _, _, _)), _) => Transferor
      case(Some(RelationshipRecord("Recipient", _, _, _, _, _, _)), _) => Recipient
      case(_, Some(Seq(RelationshipRecord("Transferor", _, _, _, _, _, _), _*))) => Transferor
      case(_, Some(Seq(RelationshipRecord("Recipient", _, _, _, _, _, _), _*))) => Recipient
    }
  }
}

object RelationshipRecords {

  def apply(relationshipRecordList: RelationshipRecordList): RelationshipRecords = {

    val relationships = relationshipRecordList.relationships
    val activeRelationship = relationships.find(_.isActive)
    val historicRelationships = {
      if (relationships.size > 1 && relationships.head.participant1EndDate.isEmpty) {
        Some(relationships.tail)
      } else if (relationships.nonEmpty && relationships.head.participant1EndDate.isDefined) {
        Some(relationships)
      } else None
    }

    RelationshipRecords(activeRelationship, historicRelationships, relationshipRecordList.userRecord)
  }
}
