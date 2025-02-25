/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.matchers.must.Matchers.mustBe
import utils.BaseTest

class CurrentYearInputTest extends BaseTest {
  "CurrentYearInput" should {

    "initialize correctly with applyForCurrentYear = Some(true)" in {
      val input = CurrentYearInput(applyForCurrentYear = Some(true))
      input.applyForCurrentYear mustBe Some(true)
    }

    "initialize correctly with applyForCurrentYear = Some(false)" in {
      val input = CurrentYearInput(applyForCurrentYear = Some(false))
      input.applyForCurrentYear mustBe Some(false)
    }

    "initialize correctly with applyForCurrentYear = None" in {
      val input = CurrentYearInput(applyForCurrentYear = None)
      input.applyForCurrentYear mustBe None
    }

    "extract applyForCurrentYear using unapply when value is Some(true)" in {
      val input = CurrentYearInput(applyForCurrentYear = Some(true))

      CurrentYearInput.unapply(input) mustBe Some(Some(true))
    }

    "extract applyForCurrentYear using unapply when value is Some(false)" in {
      val input = CurrentYearInput(applyForCurrentYear = Some(false))

      CurrentYearInput.unapply(input) mustBe Some(Some(false))
    }

    "extract applyForCurrentYear using unapply when value is None" in {
      val input = CurrentYearInput(applyForCurrentYear = None)

      CurrentYearInput.unapply(input) mustBe Some(None)
    }

    "match correctly in a pattern matching scenario with Some(true)" in {
      val input = CurrentYearInput(applyForCurrentYear = Some(true))
      val result = input match {
        case CurrentYearInput(Some(value)) => value
        case _ => false
      }
      result mustBe true
    }

    "match correctly in a pattern matching scenario with None" in {
      val input = CurrentYearInput(applyForCurrentYear = None)
      val result = input match {
        case CurrentYearInput(Some(value)) => value
        case CurrentYearInput(None) => false
      }
      result mustBe false
    }
  }

}
