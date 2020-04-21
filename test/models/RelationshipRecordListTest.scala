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

import uk.gov.hmrc.play.test.UnitSpec
import test_utils.data.RelationshipRecordData._

class RelationshipRecordListTest extends UnitSpec {

  "RelationshipRecordList" should {

    "return a RelationshipRecordList with LoggedInUserInfo" in {
      val recordList = Seq(activeRecipientRelationshipRecord, inactiveRelationshipRecord1)
      val loggedInUser = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some("First"), Some("Name"))))
      val relationshipRecordList = RelationshipRecordList(recordList, Some(loggedInUser))

      relationshipRecordList.relationships shouldBe Seq(activeRecipientRelationshipRecord, inactiveRelationshipRecord1)
      relationshipRecordList.userRecord shouldBe Some(LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some("First"), Some("Name")))))
    }

    "return a RelationshipRecordList without LoggedInUserInfo" in {
      val recordList = Seq(activeRecipientRelationshipRecord, inactiveRelationshipRecord1)
      val relationshipRecordList = RelationshipRecordList(recordList)

      relationshipRecordList.relationships shouldBe Seq(activeRecipientRelationshipRecord, inactiveRelationshipRecord1)
      relationshipRecordList.userRecord shouldBe None
    }

    "return Empty sequence for relationships" when {
      "relationships is an empty list" in {
        val relationshipRecordList = RelationshipRecordList(Seq())
        relationshipRecordList.relationships shouldBe Seq()
      }
    }
  }

}
