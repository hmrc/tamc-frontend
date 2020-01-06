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

class RelationshipRecordTest extends UnitSpec with GuiceOneAppPerSuite {

  "isActive" should {
    "return true" when {
      "there is no relationship end date" in {
        relationshipRecordWitNoEndDate.isActive shouldBe true
      }
      "relationship end date is a future date" in {
        val relationshipEndDateTomorrow = new DateTime().plusDays(1).toString("yyyyMMdd")
        val relationshipRecordTomorrow = relationshipRecordWitNoEndDate.copy(participant1EndDate = Some(relationshipEndDateTomorrow))
        relationshipRecordTomorrow.isActive shouldBe true

        val relationshipEndDateFuture = new DateTime().plusYears(10).toString("yyyyMMdd")
        val relationshipRecordFuture = relationshipRecordWitNoEndDate.copy(participant1EndDate = Some(relationshipEndDateFuture))
        relationshipRecordFuture.isActive shouldBe true
      }
    }

    "return false" when {
      "relationship end date is a past date" in {
        val relationshipEndDateYesterday = new DateTime().minusDays(1).toString(DateUtils.DatePattern)
        val relationshipRecordYesterday = relationshipRecordWitNoEndDate.copy(participant1EndDate = Some(relationshipEndDateYesterday))
        relationshipRecordYesterday.isActive shouldBe false

        val relationshipEndDatePast = new DateTime().minusYears(5).toString(DateUtils.DatePattern)
        val relationshipRecordPast = relationshipRecordWitNoEndDate.copy(participant1EndDate = Some(relationshipEndDatePast))
        relationshipRecordPast.isActive shouldBe false
      }
      "relationship end date is today" in {
        val relationshipEndDate = new DateTime().toString(DateUtils.DatePattern)
        val relationshipRecord = relationshipRecordWitNoEndDate.copy(participant1EndDate = Some(relationshipEndDate))
        relationshipRecord.isActive shouldBe false
      }
    }
  }
  val relationshipRecordWitNoEndDate =
    RelationshipRecord(
      Role.RECIPIENT,
      "56787",
      "20130101",
      Some(RelationshipEndReason.Default),
      None,
      "",
      "")
}
