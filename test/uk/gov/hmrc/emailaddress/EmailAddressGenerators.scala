/*
 * Copyright 2019 HM Revenue & Customs
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

import scala.collection.mutable.ListBuffer
import scala.util.Random

object EmailAddressGenerators {

  lazy val random: Random = new Random()

  def randomEmailAddresses(): List[String] = {
    var res = ListBuffer[String]()
    val index = newIndex
    for (_ <- 1 to index) {
      val mailbox = randomString(1)
      val domain = randomString(2)
      res += s"$mailbox@$domain".mkString
    }
    res.toList
  }

  private def randomString(dotsNumber: Int): String = {
    var result = ""
    for (i <- 1 to dotsNumber) {
      val index = newIndex
      val res = random.alphanumeric.take(index).filter(!_.isDigit).mkString.toLowerCase
      if (i >= dotsNumber) {
        result += res
      } else {
        result += res + "."
      }
    }

    result
  }

  private def newIndex: Int = random.nextInt(10) + 5
}
