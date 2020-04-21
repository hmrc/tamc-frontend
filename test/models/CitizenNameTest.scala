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

import utils.ControllerBaseTest

class CitizenNameTest extends ControllerBaseTest {

  "fullName" should {
    "return both names" when {
      "both first and last name are defined" in {
        val result = CitizenName(Some("First"), Some("Last"))
        result.fullName shouldBe Some("First Last")
      }
    }

    "return one name" when {
      "first name is defined" in {
        val result = CitizenName(Some("First"), None)
        result.fullName shouldBe Some("First")
      }

      "last name is defined" in {
        val result = CitizenName(None, Some("Last"))
        result.fullName shouldBe Some("Last")
      }
    }

    "return none" when {
      "no names are defined" in {
        val result = CitizenName(None, None)
        result.fullName shouldBe None
      }
    }
  }
}
