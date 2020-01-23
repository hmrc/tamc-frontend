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

package utils

import play.api.Logger

object DateUtils {

  val datePattern: String = "yyyyMMdd"

  def isFutureDate(date: String): Boolean = {
    var res = false
    val format = new java.text.SimpleDateFormat(datePattern)
    try {
      val time = format.parse(date).getTime
      res = time > System.currentTimeMillis()
    } catch {
      case exp: Throwable => Logger.error(s"Failed to parse date [$date] into format [$datePattern]", exp)
    }

    res
  }
}
