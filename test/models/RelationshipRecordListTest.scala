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

import org.joda.time.DateTime
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.play.test.UnitSpec
import utils.DateUtils

import scala.concurrent.ExecutionContext

class RelationshipRecordListTest extends UnitSpec with GuiceOneAppPerSuite {

  "activeRelationshipRecord" should {
    "return None" when {
      "relationships is an empty list" in {
        val relationshipRecordList = RelationshipRecordList(Seq())
        relationshipRecordList.relationships shouldBe Seq()
      }
      "there is no active relationship records on the list" in {

        val relationshipRecordWrapper: RelationshipRecordList = RelationshipRecordList(
          Seq(
            inactiveRelationshipRecord1,
            inactiveRelationshipRecord2,
            inactiveRelationshipRecord3
          ))

        relationshipRecordWrapper.relationships.foreach { elem =>
          elem shouldBe None
        }
        //        relationshipRecordWrapper.relationships.head shouldBe None
        //        relationshipRecordWrapper.relationships(1) shouldBe None
        //        relationshipRecordWrapper.relationships.tail shouldBe None
      }
    }

    //    "return the head of active relationship list" when {
    //      "there are active and inactive relationship records on the list" in {
    //        val relationshipRecordWrapper =
    //          RelationshipRecordList(
    //            Seq(
    //              activeRelationshipRecord,
    //              activeRelationshipRecord2,
    //              inactiveRelationshipRecord1,
    //              inactiveRelationshipRecord2,
    //              inactiveRelationshipRecord3
    //            ))
    //        relationshipRecordWrapper.activeRelationship shouldBe Some(activeRelationshipRecord)
    //      }
    //    }
  }

  private val activeRelationshipRecord =
    RelationshipRecord(
      RoleOld.RECIPIENT,
      "56787",
      "20130101",
      Some(RelationshipEndReason.Default),
      None,
      "",
      "")

  private val activeRelationshipRecord2 = activeRelationshipRecord.copy()


  private val inactiveRelationshipEndDate1 = new DateTime().minusDays(1).toString(DateUtils.DatePattern)
  private val inactiveRelationshipEndDate2 = new DateTime().minusDays(10).toString(DateUtils.DatePattern)
  private val inactiveRelationshipEndDate3 = new DateTime().minusDays(1000).toString(DateUtils.DatePattern)

  private val inactiveRelationshipRecord1 = activeRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate1))
  private val inactiveRelationshipRecord2 = activeRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate2))
  private val inactiveRelationshipRecord3 = activeRelationshipRecord.copy(participant1EndDate = Some(inactiveRelationshipEndDate3))
}
