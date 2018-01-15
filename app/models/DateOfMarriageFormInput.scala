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

import org.joda.time.LocalDate

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes

object DateOfMarriageFormInput {
  private val pattern = "dd/MM/yyyy"
  implicit val dateFormat = Format[LocalDate](Reads.jodaLocalDateReads(pattern), Writes.jodaLocalDateWrites(pattern))
  implicit val formats = Json.format[DateOfMarriageFormInput]
}

case class DateOfMarriageFormInput(dateOfMarriage: LocalDate)
