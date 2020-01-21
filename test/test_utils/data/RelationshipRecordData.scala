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

package test_utils.data

import models._
import test_utils.TestData.Cids
import uk.gov.hmrc.emailaddress.EmailAddress

object RelationshipRecordData {
  val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo = LoggedInUserInfo(Cids.cid1, "", Some(true), Some(citizenName))
  val notificationRecord = NotificationRecord(EmailAddress("test@test.com"))

  val activeRecord = RelationshipRecord(Role.RECIPIENT, "56787", "20130101", Some(RelationshipEndReason.Default), Some("20130110"), "", "")
  val activeRecordWithNoEndDate: RelationshipRecord = activeRecord.copy(relationshipEndReason = None, participant1EndDate = None)

  val historicRecord = RelationshipRecord(Role.TRANSFEROR, "56789", "01-01-2012", Some(RelationshipEndReason.Death), Some("1-01-2013"), "", "")
  val historicRecordDivorce: RelationshipRecord = historicRecord.copy(relationshipEndReason = Some(RelationshipEndReason.Divorce))
  val historicRecordDivorcePY = RelationshipRecord(Role.RECIPIENT, "12345", "01-01-2002", Some(RelationshipEndReason.Divorce), Some("1-01-2013"), "", "")

  val activeRelationshipRecordList = RelationshipRecordList(Some(activeRecord), None, Some(loggedInUserInfo), true, false, false)
  val historicRelationshipRecordList = RelationshipRecordList(None, Some(List(historicRecord)), Some(loggedInUserInfo), false, true, false)
  val multiHistoricRelRecordList = historicRelationshipRecordList.copy(historicRelationships = Some(List(historicRecordDivorce, historicRecordDivorcePY)))
  val bothRelationshipRecordList: RelationshipRecordList = historicRelationshipRecordList.copy(activeRelationship = Some(activeRecord), activeRecord = true)
  val noRelationshipRecordList = RelationshipRecordList(None, None, Some(loggedInUserInfo), false, false, false)
  val activeHistoricRelRecordList = RelationshipRecordList(Some(activeRecord), Some(List(historicRecordDivorcePY)), Some(loggedInUserInfo), false, true, true)

  val updateRelationshipCacheData = UpdateRelationshipCacheData(Some(loggedInUserInfo), Some(Role.TRANSFEROR), Some(activeRecord),
    notification = Some(notificationRecord), relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.DIVORCE_PY)))
}
