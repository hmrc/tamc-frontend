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

import play.api.libs.json.Json
import services.TimeService

case class UpdateRelationshipRequestHolder(request: UpdateRelationshipRequest, notification: UpdateRelationshipNotificationRequest)

object UpdateRelationshipRequestHolder {

  implicit val formats = Json.format[UpdateRelationshipRequestHolder]

  def apply(cacheData: UpdateRelationshipCacheDataTemp, isWelsh: Boolean): UpdateRelationshipRequestHolder = {

    //TODO Check that the date format is what is expected. i.e is it right to stringify a LocalDate and set it down?
    val relationshipInformation = RelationshipInformation(???, cacheData.endMaReason, cacheData.marriageEndDate.toString())

    //TODO the timestamp may always be None. The logic does not look correct inside either
    val participant1 = cacheData.relationshipRecords.recipientInformation(cacheData.endMaReason, ???)

    //TODO object to calculate
    val participate2 = cacheData.relationshipRecords.transferorInformation(cacheData.endMaReason, ???)


    val updateRelationshipRequest = UpdateRelationshipRequest(participant1, participate2, relationshipInformation)


    //TODO is the unknown correctm is isWelsh required???
    val emailNotificationData = UpdateRelationshipNotificationRequest(cacheData.email, cacheData.relationshipRecords, isWelsh)

    UpdateRelationshipRequestHolder(updateRelationshipRequest, emailNotificationData)

  }
}
