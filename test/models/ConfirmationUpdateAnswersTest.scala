/*
 * Copyright 2022 HM Revenue & Customs
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

import errors.{CacheMissingEmail, CacheMissingMAEndingDates, CacheMissingRelationshipRecords}

import java.time.LocalDate
import test_utils.data.RelationshipRecordData.activeRecipientRelationshipRecord
import utils.UnitSpec

class ConfirmationUpdateAnswersTest extends UnitSpec {

  val loggedInUser = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some("first"), Some("surname"))))
  val relationshipRecords = RelationshipRecords(activeRecipientRelationshipRecord, Seq.empty[RelationshipRecord], loggedInUser)
  val email = "email@email.com"
  val dateOfDivorce = Some(LocalDate.now().minusDays(1))
  val marriageAllowanceEndingDates = MarriageAllowanceEndingDates(LocalDate.now(), LocalDate.now())

  def createCacheData(relRecords: Option[RelationshipRecords] = Some(relationshipRecords),
                      divorceDate: Option[LocalDate] = dateOfDivorce,
                      emailAddress: Option[String] = Some(email),
                      maEndingDates: Option[MarriageAllowanceEndingDates] = Some(marriageAllowanceEndingDates)): ConfirmationUpdateAnswersCacheData = {

    ConfirmationUpdateAnswersCacheData(relRecords, divorceDate, emailAddress, maEndingDates)
  }

  "ConfirmationUpdateAnswers" should {

    "create a ConfirmationUpdateAnswers object given all cache data" in {

      val cacheData = createCacheData()
      val confirmUpdateAnswers = ConfirmationUpdateAnswers(cacheData)

      confirmUpdateAnswers shouldBe ConfirmationUpdateAnswers(loggedInUser, dateOfDivorce, email, marriageAllowanceEndingDates)

    }

    "return an error" when {

      "relationshipRecords are missing from the cache" in {
        val cacheData = createCacheData(relRecords = None)

        a[CacheMissingRelationshipRecords] shouldBe thrownBy(ConfirmationUpdateAnswers(cacheData))
      }

      "email is missing from the cache" in {
        val cacheData = createCacheData(emailAddress = None)

        a[CacheMissingEmail] shouldBe thrownBy(ConfirmationUpdateAnswers(cacheData))
      }

      "marriage allowance ending dates are missing from the cache" in {
        val cacheData = createCacheData(maEndingDates = None)

        a[CacheMissingMAEndingDates] shouldBe thrownBy(ConfirmationUpdateAnswers(cacheData))
      }
    }
  }
}