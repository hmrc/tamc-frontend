/*
 * Copyright 2023 HM Revenue & Customs
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

package test_utils.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import models._

object RelationshipRecordData {

  val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

  val activeRecipientRelationshipRecord: RelationshipRecord = RelationshipRecord(
    Recipient.value,
    creationTimestamp = "20150531235901",
    participant1StartDate = "20150531235901",
    relationshipEndReason = Some(DesRelationshipEndReason.Default),
    participant1EndDate = None,
    otherParticipantInstanceIdentifier = "123456789123",
    otherParticipantUpdateTimestamp = "20150531235901")

  val activeTransferorRelationshipRecord2: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant = Transferor.value)
  val activeRelationshipEndDate1: String = dateFormat.format(LocalDateTime.now().plusDays(10))
  val activeTransferorRelationshipRecord3: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(activeRelationshipEndDate1))


  val inactiveRelationshipEndDate1: String = dateFormat.format(LocalDateTime.now().minusDays(1))
  val inactiveRelationshipEndDate2: String = dateFormat.format(LocalDateTime.now().minusDays(10))
  val inactiveRelationshipEndDate3: String = dateFormat.format(LocalDateTime.now().minusDays(1000))

  val inactiveRecipientRelationshipRecord1: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveRecipientRelationshipRecord2: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveRecipientRelationshipRecord3: RelationshipRecord = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  val inactiveTransferorRelationshipRecord1: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveTransferorRelationshipRecord2: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveTransferorRelationshipRecord3: RelationshipRecord = activeTransferorRelationshipRecord2.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

  val activeRelationshipRecord2 = activeRecipientRelationshipRecord.copy()
  val inactiveRelationshipRecord1 = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  val inactiveRelationshipRecord2 = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  val inactiveRelationshipRecord3 = activeRecipientRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))

}
