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

class LoggedInUserInfoTest extends BaseTest {
  "serialize to JSON correctly when has_allowance and name are provided" in {
    val userInfo = LoggedInUserInfo(
      cid = 12345,
      timestamp = "2023-10-06T12:00:00Z",
      has_allowance = Some(true),
      name = Some(CitizenName(Some("John"), Some("Doe")))
    )

    val json: JsValue = Json.toJson(userInfo)

    json mustBe Json.parse(
      """{
        |  "cid": 12345,
        |  "timestamp": "2023-10-06T12:00:00Z",
        |  "has_allowance": true,
        |  "name": {
        |    "firstName": "John",
        |    "lastName": "Doe"
        |  }
        |}""".stripMargin
    )
  }

  "serialize to JSON correctly when has_allowance is None and name is None" in {
    val userInfo = LoggedInUserInfo(
      cid = 67890,
      timestamp = "2023-10-06T12:30:00Z",
      has_allowance = None,
      name = None
    )

    val json: JsValue = Json.toJson(userInfo)

    json mustBe Json.parse(
      """{
        |  "cid": 67890,
        |  "timestamp": "2023-10-06T12:30:00Z"
        |}""".stripMargin
    )
  }

  "deserialize from JSON correctly when has_allowance and name are provided" in {
    val json: JsValue = Json.parse(
      """{
        |  "cid": 12345,
        |  "timestamp": "2023-10-06T12:00:00Z",
        |  "has_allowance": true,
        |  "name": {
        |    "firstName": "John",
        |    "lastName": "Doe"
        |  }
        |}""".stripMargin
    )

    val userInfo = json.as[LoggedInUserInfo]

    userInfo mustBe LoggedInUserInfo(
      cid = 12345,
      timestamp = "2023-10-06T12:00:00Z",
      has_allowance = Some(true),
      name = Some(CitizenName(Some("John"), Some("Doe")))
    )
  }

  "deserialize from JSON correctly when has_allowance is None and name is None" in {
    val json: JsValue = Json.parse(
      """{
        |  "cid": 67890,
        |  "timestamp": "2023-10-06T12:30:00Z",
        |  "has_allowance": null,
        |  "name": null
        |}""".stripMargin
    )

    val userInfo = json.as[LoggedInUserInfo]

    userInfo mustBe LoggedInUserInfo(
      cid = 67890,
      timestamp = "2023-10-06T12:30:00Z",
      has_allowance = None,
      name = None
    )
  }
}
