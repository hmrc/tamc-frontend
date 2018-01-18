/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.JsValue
import play.api.libs.json.JsSuccess
import play.api.libs.json.Format
import play.api.libs.json._

object RelationshipRecordStatusWrapper {
  implicit val formats = Json.format[RelationshipRecordStatusWrapper]
}

case class RelationshipRecordStatusWrapper(relationship_record: RelationshipRecordWrapper = RelationshipRecordWrapper(Seq()), status: ResponseStatus)
