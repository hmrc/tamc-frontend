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

import org.joda.time.LocalDateTime
import test_utils.data.RelationshipRecordData.activeRecipientRelationshipRecord
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class UpdateRelationshipRequestHolderTest extends UnitSpec {

  val now = LocalDateTime.now()
  val firstName = "First"
  val surname = "Surname"
  val timeStampFormat = "YYYYMMdhhmmss"
  val instanceIdentifier = 123456789
  val loggedInUser = LoggedInUserInfo(instanceIdentifier, now.toString(timeStampFormat), None, Some(CitizenName(Some(firstName), Some(surname))))
  val email = "email@email.com"
  val fullName = s"$firstName $surname"
  val maEndReason = "Divorce"
  val marriageAllowanceEndDate = now.toLocalDate.plusDays(1)
  val isWelsh = false
  val relationshipRecords = RelationshipRecords(activeRecipientRelationshipRecord, Seq.empty[RelationshipRecord], loggedInUser)

  "UpdateRelationshipRequestHolder" should {
    "create an UpdateRelationshipRequestHolder object" when {
      "a valid update data model is provided" in {

        val updateRelationshipData = UpdateRelationshipData(relationshipRecords, email, maEndReason, marriageAllowanceEndDate)

        val expectedRecipientInformation = RecipientInformation(instanceIdentifier.toString, now.toString(timeStampFormat))
        val expectedTransferorInformation = TransferorInformation(activeRecipientRelationshipRecord.otherParticipantUpdateTimestamp)
        val expectedRelationshipInfo = RelationshipInformation(activeRecipientRelationshipRecord.creationTimestamp, DesEnumeration(maEndReason),
          marriageAllowanceEndDate.toString("yyyyMMdd"))
        val expectedEmailNotification = UpdateRelationshipNotificationRequest(fullName, EmailAddress(email), Recipient.value)
        val expectedUpdateRelationshipRequest = UpdateRelationshipRequest(expectedRecipientInformation, expectedTransferorInformation, expectedRelationshipInfo)

        val expectedRequestHolder = UpdateRelationshipRequestHolder(expectedUpdateRelationshipRequest, expectedEmailNotification)

        UpdateRelationshipRequestHolder(updateRelationshipData, isWelsh) shouldBe expectedRequestHolder

      }
    }
  }
}
