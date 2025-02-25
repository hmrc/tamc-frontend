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
import play.api.libs.json.{JsValue, Json}
import utils.BaseTest

import java.time.LocalDate

class MarriageAllowanceEndingDatesTest extends BaseTest {
  "MarriageAllowanceEndingDates" should {
    "serialize to JSON correctly" in {
      val dates = MarriageAllowanceEndingDates(
        marriageAllowanceEndDate = LocalDate.of(2023, 12, 31),
        personalAllowanceEffectiveDate = LocalDate.of(2024, 1, 1)
      )

      val json: JsValue = Json.toJson(dates)

      json mustBe Json.parse(
        """{
          |  "marriageAllowanceEndDate": "2023-12-31",
          |  "personalAllowanceEffectiveDate": "2024-01-01"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "marriageAllowanceEndDate": "2023-12-31",
          |  "personalAllowanceEffectiveDate": "2024-01-01"
          |}""".stripMargin
      )

      val dates = json.as[MarriageAllowanceEndingDates]

      dates mustBe MarriageAllowanceEndingDates(
        marriageAllowanceEndDate = LocalDate.of(2023, 12, 31),
        personalAllowanceEffectiveDate = LocalDate.of(2024, 1, 1)
      )
    }
  }
}
