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

class ResponseStatusTest extends BaseTest {
  "ResponseStatus" should {
    "serialize to JSON correctly" in {
      val responseStatus = ResponseStatus("404")
      val json: JsValue = Json.toJson(responseStatus)

      json mustBe Json.parse("""{"status_code":"404"}""")
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse("""{"status_code":"404"}""")
      val responseStatus = json.as[ResponseStatus]

      responseStatus mustBe ResponseStatus("404")
    }

    "handle serialization and deserialization symmetrically" in {
      val responseStatus = ResponseStatus("200")
      val serializedJson = Json.toJson(responseStatus)
      val deserializedResponseStatus = serializedJson.as[ResponseStatus]

      deserializedResponseStatus mustBe responseStatus
    }
  }

}
