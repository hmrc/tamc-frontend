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

package test_utils.data

import models._
import test_utils.TestData.Cids

object RelationshipRecordData {
  val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo = LoggedInUserInfo(Cids.cid1, "", Some(true), Some(citizenName))

  val activeRecord = RelationshipRecord(Role.TRANSFEROR, "56787", "20130101", Some(""), Some("20130110"), "", "")
  val historicRecord = RelationshipRecord(Role.TRANSFEROR, "56789", "01-01-2012", Some(""), Some("1-01-2013"), "", "")

  val activeRelationshipRecordList = RelationshipRecordList(Some(activeRecord), None, Some(loggedInUserInfo), true, false, false)
  val historicRelationshipRecordList = RelationshipRecordList(None, Some(List(historicRecord)), Some(loggedInUserInfo), false, true, false)

}
