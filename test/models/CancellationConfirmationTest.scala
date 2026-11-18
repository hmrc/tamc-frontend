/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.libs.json.{JsValue, Json}
import utils.BaseTest

import java.time.LocalDate

class CancellationConfirmationTest extends BaseTest {

  "CancellationConfirmation" should {

    "serialize to JSON correctly with a valid end date" in {
      val confirmation = CancellationConfirmation(
        marriageAllowanceEndDate = LocalDate.of(2027, 4, 5)
      )

      val json: JsValue = Json.toJson(confirmation)

      json shouldBe Json.parse(
        """{
          |  "marriageAllowanceEndDate": "2027-04-05"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly with a valid end date" in {
      val json: JsValue = Json.parse(
        """{
          |  "marriageAllowanceEndDate": "2027-04-05"
          |}""".stripMargin
      )

      json.validate[CancellationConfirmation] shouldBe
        play.api.libs.json.JsSuccess(
          CancellationConfirmation(
            marriageAllowanceEndDate = LocalDate.of(2027, 4, 5)
          )
        )
    }

    "fail to deserialize from JSON when the end date is invalid" in {
      val json: JsValue = Json.parse(
        """{
          |  "marriageAllowanceEndDate": "invalid-date"
          |}""".stripMargin
      )

      json.validate[CancellationConfirmation].isError shouldBe true
    }

    "fail to deserialize from JSON when the end date is missing" in {
      val json: JsValue = Json.obj()

      json.validate[CancellationConfirmation].isError shouldBe true
    }
  }
}
