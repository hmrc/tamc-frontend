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

import org.joda.time.LocalDate
import play.api.libs.json._

object EndRelationshipReason {
  private val pattern = "dd/MM/yyyy"
  implicit val dateFormat: Format[LocalDate] = Format[LocalDate](JodaReads.jodaLocalDateReads(pattern), JodaWrites.jodaLocalDateWrites(pattern))
  implicit val formats: OFormat[EndRelationshipReason] = Json.format[EndRelationshipReason]
}

case class EndRelationshipReason(endReason: String, dateOfDivorce: Option[LocalDate] = None, timestamp: Option[String] = None)
