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

import play.api.libs.json.Format
import play.api.libs.json.Json

object CitizenName {
  implicit val formats: Format[CitizenName] = Json.format[CitizenName]
}

case class CitizenName(firstName: Option[String], lastName: Option[String]) {
  def fullName: Option[String] = (firstName, lastName) match {
    case (Some(firstName), Some(lastName)) =>
      Some(s"${firstName.toLowerCase.capitalize} ${lastName.toLowerCase.capitalize}")
    case (Some(firstName), None) =>
      Some(firstName.toLowerCase.capitalize)
    case (None, Some(lastName)) =>
      Some(lastName.toLowerCase.capitalize)
    case _ =>
      None
  }
}
