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

class TransferorEligibilityHolderTest extends BaseTest {
  "serialize to JSON correctly when name is provided" in {
    val holder = TransferorEligibilityHolder(
      eligible = true,
      name = Some(CitizenName(Some("John"), Some("Doe")))
    )

    val json: JsValue = Json.toJson(holder)

    json mustBe Json.parse(
      """{
        |  "eligible": true,
        |  "name": {
        |    "firstName": "John",
        |    "lastName": "Doe"
        |  }
        |}""".stripMargin
    )
  }

  "serialize to JSON correctly when name is not provided" in {
    val holder = TransferorEligibilityHolder(
      eligible = false,
      name = None
    )

    val json: JsValue = Json.toJson(holder)

    json mustBe Json.parse(
      """{
        |  "eligible": false
        |}""".stripMargin
    )
  }

  "deserialize from JSON correctly when name is provided" in {
    val json: JsValue = Json.parse(
      """{
        |  "eligible": true,
        |  "name": {
        |    "firstName": "Jane",
        |    "lastName": "Smith"
        |  }
        |}""".stripMargin
    )

    val holder = json.as[TransferorEligibilityHolder]

    holder mustBe TransferorEligibilityHolder(
      eligible = true,
      name = Some(CitizenName(Some("Jane"), Some("Smith")))
    )
  }

  "deserialize from JSON correctly when name is not provided" in {
    val json: JsValue = Json.parse(
      """{
        |  "eligible": false,
        |  "name": null
        |}""".stripMargin
    )

    val holder = json.as[TransferorEligibilityHolder]

    holder mustBe TransferorEligibilityHolder(
      eligible = false,
      name = None
    )
  }
}
