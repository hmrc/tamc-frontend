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

//constructor
sealed trait Role {
  def asString(): String
}

object Role {
  def asString(role: Role): String = {
    role match {
      case Transferor => "Transferor"
      case Recipient => "Transferor"
    }
  }
}

case object Transferor extends Role {
  def asString(): String = "Transferor"
}

case object Recipient extends Role {
  def asString(): String = "Recipient"
}

case object Unknown extends Role {
  def asString(): String = "Unknown"
}