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
import play.api.libs.json.{JsValue, Json, JsError}
import utils.{BaseTest, EmailAddress}

class NotificationRecordTest extends BaseTest {
  "NotificationRecord" should {

    "serialize to JSON correctly with a valid email address" in {
      val record = NotificationRecord(EmailAddress("john.doe@example.com"))

      val json: JsValue = Json.toJson(record)

      json mustBe Json.parse(
        """{
          |  "transferor_email": "john.doe@example.com"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly with a valid email address" in {
      val json: JsValue = Json.parse(
        """{
          |  "transferor_email": "john.doe@example.com"
          |}""".stripMargin
      )

      val record = json.validate[NotificationRecord]

      record.isSuccess mustBe true
      record.get mustBe NotificationRecord(EmailAddress("john.doe@example.com"))
    }

    "fail to deserialize from JSON when the email address is invalid" in {
      val json: JsValue = Json.parse(
        """{
          |  "transferor_email": "invalid-email"
          |}""".stripMargin
      )

      val record = json.validate[NotificationRecord]

      record.isError mustBe true
      record match {
        case JsError(errors) =>
          errors.head._2.head.message should include ("not a valid email address")
        case _ =>
          fail("Expected a JsError due to invalid email")
      }
    }
  }
}
