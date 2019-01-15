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

package test_utils


import models._
import test_utils.TestData.Cids
import uk.gov.hmrc.emailaddress.EmailAddress

object MarriageAllowanceConnectorTestData {

  val email = EmailAddress("example@example.com")

  val createRelationshipRequest = CreateRelationshipRequest(Cids.cid1, "", Cids.cid2, "", Nil)

  val notification = CreateRelationshipNotificationRequest("", email)

  val relationshipRequestHolder = CreateRelationshipRequestHolder(createRelationshipRequest, notification)

  val participant1 = RecipientInformation("", "")
  val participant2 = TransferorInformation("")
  val relationshipInformation = RelationshipInformation("", "", "")

  val updateRelationshipRequest = UpdateRelationshipRequest(participant1, participant2, relationshipInformation)

  val updateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest("", email, "")

  val updateRelationshipRequestHolder = UpdateRelationshipRequestHolder(updateRelationshipRequest, updateRelationshipNotificationRequest)

}
