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

import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class UpdateRelationshipNotificationRequestTest extends UnitSpec {

  val firstName = "First"
  val surname = "Surname"
  val loggedInUserInfo = LoggedInUserInfo(1, "20200304", None, _:Option[CitizenName])
  val isWelsh = false
  val role = Recipient
  val email = "email@email.com"
  val fullName = s"$firstName $surname"
  val emailAddress = EmailAddress(email)

  "UpdateRelationshipNotificationRequest" should {
    "create an UpdateRelationshipNotificationRequest when given valid input" in {
      val loggedInUser = loggedInUserInfo(Some(CitizenName(Some(firstName), Some(surname))))
      val expectedUpdateNotification = UpdateRelationshipNotificationRequest(fullName, emailAddress, role.value, isWelsh)
      UpdateRelationshipNotificationRequest(email, role, loggedInUser, isWelsh) shouldBe expectedUpdateNotification
    }

    "send unknown when is users name is not known" in {

      val loggedInUser = loggedInUserInfo(None)
      val expectedUpdateNotification = UpdateRelationshipNotificationRequest("Unknown", emailAddress, role.value, isWelsh)
      UpdateRelationshipNotificationRequest(email, role, loggedInUser, isWelsh) shouldBe expectedUpdateNotification
    }
  }
}
