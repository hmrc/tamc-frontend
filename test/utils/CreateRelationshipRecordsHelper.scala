/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import models.{CitizenName, LoggedInUserInfo, RelationshipRecord, RelationshipRecords}

trait CreateRelationshipRecordsHelper {

  def createRelationshipRecords(roleType: String = "Transferor",
                                 nonPrimaryRecords: Seq[RelationshipRecord] = Seq.empty[RelationshipRecord]): RelationshipRecords = {

    val citizenName = CitizenName(Some("Test"), Some("User"))
    val loggedInUserInfo = LoggedInUserInfo(
      cid = 123456789,
      timestamp = "20181212",
      has_allowance = None,
      name = Some(citizenName)
    )
    val primaryRelationshipRecord = RelationshipRecord(
      participant = roleType,
      creationTimestamp = "20181212",
      participant1StartDate = "20181212",
      relationshipEndReason = None,
      participant1EndDate = None,
      otherParticipantInstanceIdentifier = "1234567",
      otherParticipantUpdateTimestamp = "20181212"
    )

    RelationshipRecords(primaryRelationshipRecord, nonPrimaryRecords, loggedInUserInfo)
  }

}
