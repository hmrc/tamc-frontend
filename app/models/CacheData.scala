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


case class CacheData(
  transferor: Option[UserRecord],
  recipient: Option[RecipientRecord],
  notification: Option[NotificationRecord],
  relationshipCreated: Option[Boolean] = None,
  selectedYears: Option[List[Int]] = None,
  recipientDetailsFormData: Option[RecipientDetailsFormInput] = None,
  dateOfMarriage : Option[DateOfMarriageFormInput] = None)

case class UpdateRelationshipCacheData(loggedInUserInfo: Option[LoggedInUserInfo] = None, roleRecord: Option[String] = None,activeRelationshipRecord: Option[RelationshipRecord] = None, historicRelationships: Option[Seq[RelationshipRecord]] = None, notification: Option[NotificationRecord], relationshipEndReasonRecord: Option[EndRelationshipReason] = None, relationshipUpdated: Option[Boolean] = None)
