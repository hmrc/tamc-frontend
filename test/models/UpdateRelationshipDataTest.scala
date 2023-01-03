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

package models

import errors._
import test_utils.data.RelationshipRecordData.activeRecipientRelationshipRecord
import utils.UnitSpec

import java.time.LocalDate

class UpdateRelationshipDataTest extends UnitSpec {

  val loggedInUser = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some("First"), Some("Name"))))
  val relationshipRecords = RelationshipRecords(activeRecipientRelationshipRecord, Seq.empty[RelationshipRecord], loggedInUser)
  val email = "email@email.com"
  val endReason = "Divorce"
  val marriageAllowanceEndingDate = LocalDate.now()

  def createCacheData(relRecords: Option[RelationshipRecords] = Some(relationshipRecords),
                      emailAddress: Option[String] = Some(email),
                      maEndReason: Option[String] = Some(endReason),
                      maEndDate: Option[LocalDate] = Some(marriageAllowanceEndingDate)): UpdateRelationshipCacheData = {

    UpdateRelationshipCacheData(relRecords, emailAddress, maEndReason, maEndDate)
  }

  "UpdateRelationshipData" should {
    "create an UpdateRelationship object given all cache data" in {

      val cacheData = createCacheData()
      val updateData = UpdateRelationshipData(cacheData)

      updateData.relationshipRecords shouldBe relationshipRecords
      updateData.email shouldBe email
      updateData.endMaReason shouldBe endReason
      updateData.marriageEndDate shouldBe marriageAllowanceEndingDate
    }

    "return an error" when {

      "relationshipRecords are missing from the cache" in {
        val cacheData = createCacheData(relRecords = None)

        a[CacheMissingRelationshipRecords] shouldBe thrownBy(UpdateRelationshipData(cacheData))
      }

      "email is missing from the cache" in {
        val cacheData = createCacheData(emailAddress = None)

        a[CacheMissingEmail] shouldBe thrownBy(UpdateRelationshipData(cacheData))
      }

      "marriage allowance end reason is missing from the cache" in {
        val cacheData = createCacheData(maEndReason = None)

        a[CacheMissingEndReason] shouldBe thrownBy(UpdateRelationshipData(cacheData))
      }

      "marriage allowance end date is missing from the cache" in {
        val cacheData = createCacheData(maEndDate = None)

        a[CacheMissingMAEndingDates] shouldBe thrownBy(UpdateRelationshipData(cacheData))
      }

    }
  }
}
