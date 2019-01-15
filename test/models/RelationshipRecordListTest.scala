/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.ControllerBaseSpec
import test_utils.data.RelationshipRecordData._

class RelationshipRecordListTest extends ControllerBaseSpec {

  "RelationshipRecordList" should {
    "set activeRecord as true" when {
      "activeRelationship is defined and participant1EndDate is empty" in {
        val recordList = new RelationshipRecordList(Some(activeRecordWithNoEndDate), None, None)
        recordList.activeRecord shouldBe true
        recordList.historicRecord shouldBe false
        recordList.historicActiveRecord shouldBe false
      }
    }

    "set historicRecord true" when {
      "historic relationship is defined" in {
        val recordList = new RelationshipRecordList(None, Some(List(historicRecord)), None)
        recordList.activeRecord shouldBe false
        recordList.historicRecord shouldBe true
        recordList.historicActiveRecord shouldBe false
      }

      "historic and active relationship is defined" in {
        val recordList = new RelationshipRecordList(Some(activeRecordWithNoEndDate), Some(List(historicRecord)), None)
        recordList.activeRecord shouldBe true
        recordList.historicRecord shouldBe true
        recordList.historicActiveRecord shouldBe false
      }
    }

    "set activeHistoricRecord as true" when {
      "activeRelationship and participant1EndDate is defined" in {
        val recordList = new RelationshipRecordList(Some(activeRecord), None, None)
        recordList.activeRecord shouldBe false
        recordList.historicRecord shouldBe false
        recordList.historicActiveRecord shouldBe true
      }
    }
  }
}
