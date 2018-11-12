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

package views.helpers

import org.scalatest.{Matchers, WordSpec}

class TextGeneratorsSpec extends WordSpec with Matchers {

  "TaxGenerators" when {

    "taxDateInterval is called" must {
      "return the beginning and end dates of a tax year, e.g. 6 April 2018 to 5 April 2019" in {
        TextGenerators.taxDateInterval(2018, isWelsh = false) shouldBe "6 April 2018 to 5 April 2019"
      }
    }

    "taxDateIntervalMultiYear is called" must {
      "return the years that two tax years span from and to, e.g. 2017 to 2019" in {
        TextGenerators.taxDateIntervalMultiYear(2017, 2018, isWelsh = false) shouldBe "2017 to 2019"
        TextGenerators.taxDateIntervalMultiYear(2016, 2018, isWelsh = false) shouldBe "2016 to 2019"
      }
    }

    "taxDateIntervalShort" must {
      "return the years that a single tax year spans across" in {
        TextGenerators.taxDateIntervalShort(2018) shouldBe "2018 to 2019"
        TextGenerators.taxDateIntervalShort(2017) shouldBe "2017 to 2018"
      }
    }

    "taxDateIntervalString" must {
      "return dates for one tax year to present" in {
        TextGenerators.taxDateIntervalString("05-05-2016",Some("05-05-2017"),isWelsh = false) shouldBe "2016 to 2018"
      }
      "return dates for one tax year to another" in {
        TextGenerators.taxDateIntervalString("20160505",None,isWelsh = false) shouldBe "2016 to Present"

      }
    }

  }

}
