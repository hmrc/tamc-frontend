/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

object CreateRelationshipRequest {
  implicit val formats: OFormat[CreateRelationshipRequest] = Json.format[CreateRelationshipRequest]
}

case class CreateRelationshipRequest(transferor_cid: Cid, transferor_timestamp: Timestamp, recipient_cid: Cid, recipient_timestamp: Timestamp, taxYears: List[Int])
