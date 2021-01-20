/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.emailaddress

import scala.util.Random

object EmailAddressGenerators {

  lazy val random: Random = new Random()

  def randomEmailAddresses(): List[String] = {
    val index = newIndex
    for (_ <- List.range(1, index)) yield {
      val mailbox = randomString(1)
      val domain = randomString(2)
      s"$mailbox@$domain"
    }
  }

  private def randomString(dotsNumber: Int): String = {
    val result = for (_ <- List.range(0, dotsNumber)) yield {
      val index = newIndex
      val res = random.alphanumeric.take(index).filter(!_.isDigit).mkString.toLowerCase
      res
    }

    result.mkString(".")
  }

  private def newIndex: Int = random.nextInt(10) + 5
}
