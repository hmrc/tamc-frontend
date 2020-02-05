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

package views.helpers

import java.time.LocalDate

import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable

class TextGeneratorsSpec extends WordSpec with Matchers {

  private val currentYear = LocalDate.now().getYear
  private val years: immutable.Seq[Int] = (currentYear - 2 to currentYear + 3).toList

  "TaxGenerators" when {

    "taxDateInterval" must {
      for (year <- years) {
        val start = year
        val finish = year + 1
        //e.g. 6 April 2018 to 5 April 2019
        s"return the beginning and end dates of $year tax year(UK)" in {
          TextGenerators.taxDateInterval(year, isWelsh = false) shouldBe s"6 April $start to 5 April $finish"
        }
        s"return the beginning and end dates of $year tax year(CY)" in {
          //6 Ebrill 2020 i 5 Ebrill 2021
          TextGenerators.taxDateInterval(year, isWelsh = true) shouldBe s"6 Ebrill $start i 5 Ebrill $finish"
        }
      }
    }

    "taxDateIntervalMultiYear" must {
      for (year <- years) {
        val start = year
        val finish = year + 1
        val expected = finish + 1
        //e.g. 2017 to 2019
        s"return the years that two tax years span from $start to $finish (UK)" in {
          TextGenerators.taxDateIntervalMultiYear(start, finish, isWelsh = false) shouldBe s"$start to $expected"
        }
        s"return the years that two tax years span from $start to $finish (CY)" in {
          TextGenerators.taxDateIntervalMultiYear(start, finish, isWelsh = true) shouldBe s"$start i $expected"
        }
      }
    }

    "taxDateIntervalShort" must {
      for (year <- years) {
        val start = year
        val finish = year + 1
        s"return the years that a single($start) tax year spans across(UK)" in {
          TextGenerators.taxDateIntervalShort(start, isWelsh = false) shouldBe s"$start to $finish"
        }
        s"return the years that a single($start) tax year spans across(CY)" in {
          TextGenerators.taxDateIntervalShort(start, isWelsh = true) shouldBe s"$start i $finish"
        }
      }
    }

    "taxDateIntervalString" must {
      //      "return dates for one tax year to present(UK)" in {
      //        //TODO John delete prev format why???!!
      //        TextGenerators.taxDateIntervalString("05-05-2016", Some("05-05-2017"), isWelsh = false) shouldBe "2016 to 2018"
      //      }
      "return dates for one tax year to another(UK)" in {
        TextGenerators.taxDateIntervalString("20160505", None, isWelsh = false) shouldBe "2016 to Present"
      }

      //      "return dates for one tax year to present(CY)" in {
      //        TextGenerators.taxDateIntervalString("05-05-2016", Some("05-05-2017"), isWelsh = true) shouldBe "2016 i 2018"
      //      }
      "return dates for one tax year to another(CY)" in {
        TextGenerators.taxDateIntervalString("20160505", None, isWelsh = true) shouldBe "2016 i’r Presennol"
      }
    }

  }

}
