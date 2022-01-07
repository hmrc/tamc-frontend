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

import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.domain.{SimpleObjectReads, SimpleObjectWrites}

object Gender {
  private val validValues: List[String] = List("M", "F")

  def isValid(code: String): Boolean = validValues.contains(code)

  implicit val genderWrite: Writes[Gender] = new SimpleObjectWrites[Gender](_.gender)
  implicit val genderRead: Reads[Gender] = new SimpleObjectReads[Gender]("gender", Gender.apply)
}

case class Gender(gender: String) {
  require(Gender.isValid(gender), s"$gender is not a valid gender.")
}
