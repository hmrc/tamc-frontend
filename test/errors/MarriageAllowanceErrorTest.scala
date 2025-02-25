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

package errors

import models.ResponseStatus
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import utils.BaseTest

class MarriageAllowanceErrorTest extends BaseTest with GuiceOneAppPerSuite {
  "MarriageAllowanceError" should {
    "serialize to JSON correctly" in {
      val error = MarriageAllowanceError(ResponseStatus("500"))
      val json: JsValue = Json.toJson(error)

      json mustBe Json.parse("""{"status":{"status_code":"500"}}""")
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse("""{"status":{"status_code":"500"}}""")
      val error = json.as[MarriageAllowanceError]

      error mustBe MarriageAllowanceError(ResponseStatus("500"))
    }

    "handle serialization and deserialization symmetrically" in {
      val error = MarriageAllowanceError(ResponseStatus("403"))
      val serializedJson = Json.toJson(error)
      val deserializedError = serializedJson.as[MarriageAllowanceError]

      deserializedError mustBe error
    }
  }
}
