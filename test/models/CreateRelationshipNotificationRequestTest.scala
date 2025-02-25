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
import utils.{BaseTest, EmailAddress}

import scala.util.Try

class CreateRelationshipNotificationRequestTest extends BaseTest {
  "CreateRelationshipNotificationRequest" should {

    "serialize to JSON correctly" in {
      val request = CreateRelationshipNotificationRequest(
        full_name = "John Doe",
        email = EmailAddress("john.doe@example.com"),
        welsh = false
      )

      val json: JsValue = Json.toJson(request)

      json mustBe Json.parse(
        """{
          |  "full_name": "John Doe",
          |  "email": "john.doe@example.com",
          |  "welsh": false
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "full_name": "John Doe",
          |  "email": "john.doe@example.com",
          |  "welsh": false
          |}""".stripMargin
      )

      val result = json.validate[CreateRelationshipNotificationRequest]

      result.isSuccess mustBe true
      result.get mustBe CreateRelationshipNotificationRequest(
        full_name = "John Doe",
        email = EmailAddress("john.doe@example.com"),
        welsh = false
      )
    }

    "serialize and deserialize symmetrically" in {
      val request = CreateRelationshipNotificationRequest(
        full_name = "Jane Smith",
        email = EmailAddress("jane.smith@example.com"),
        welsh = true
      )

      val json = Json.toJson(request)
      val deserialized = json.as[CreateRelationshipNotificationRequest]

      deserialized mustBe request
    }

    "fail to deserialize when email is invalid" in {
      val json: JsValue = Json.parse(
        """{
          |  "full_name": "John Doe",
          |  "email": "invalid_email",
          |  "welsh": false
          |}""".stripMargin
      )

      val result = Try(json.as[CreateRelationshipNotificationRequest])

      result.isFailure mustBe true
      result.failed.get.getMessage should include(
        "not a valid email address"
      )
    }
  }
}
