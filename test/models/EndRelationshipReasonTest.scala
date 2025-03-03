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
import utils.BaseTest

import java.time.LocalDate

class EndRelationshipReasonTest extends BaseTest {
  "EndRelationshipReason" should {

    "serialize to JSON correctly when all fields are present" in {
      val reason = EndRelationshipReason(
        endReason = "Divorce",
        dateOfDivorce = Some(LocalDate.of(2023, 8, 1)),
        timestamp = Some("2023-08-01T12:34:56")
      )

      val json: JsValue = Json.toJson(reason)

      json.toString mustBe Json.parse(
        """{
          |  "endReason": "Divorce",
          |  "dateOfDivorce": "01/08/2023",
          |  "timestamp": "2023-08-01T12:34:56"
          |}""".stripMargin
      ).toString
    }

    "serialize to JSON correctly when optional fields are missing" in {
      val reason = EndRelationshipReason(
        endReason = "Other Reason"
      )

      val json: JsValue = Json.toJson(reason)

      json.toString mustBe Json.parse(
        """{
          |  "endReason": "Other Reason"
          |}""".stripMargin
      ).toString
    }

    "deserialize from JSON correctly when all fields are present" in {
      val json: JsValue = Json.parse(
        """{
          |  "endReason": "Divorce",
          |  "dateOfDivorce": "01/08/2023",
          |  "timestamp": "2023-08-01T12:34:56"
          |}""".stripMargin
      )

      val result = json.validate[EndRelationshipReason]

      result.isSuccess mustBe true
      result.get mustBe EndRelationshipReason(
        endReason = "Divorce",
        dateOfDivorce = Some(LocalDate.of(2023, 8, 1)),
        timestamp = Some("2023-08-01T12:34:56")
      )
    }

    "deserialize from JSON correctly when optional fields are missing" in {
      val json: JsValue = Json.parse(
        """{
          |  "endReason": "Other Reason"
          |}""".stripMargin
      )

      val result = json.validate[EndRelationshipReason]

      result.isSuccess mustBe true
      result.get mustBe EndRelationshipReason(
        endReason = "Other Reason",
        dateOfDivorce = None,
        timestamp = None
      )
    }

    "serialize and deserialize symmetrically" in {
      val reason = EndRelationshipReason(
        endReason = "Death of Partner",
        dateOfDivorce = Some(LocalDate.of(2023, 5, 20)),
        timestamp = Some("2023-05-20T09:15:00")
      )

      val json = Json.toJson(reason)
      val deserializedReason = json.as[EndRelationshipReason]

      deserializedReason shouldBe reason
    }

    "fail to deserialize when mandatory fields are missing" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "dateOfDivorce": "01/08/2023",
          |  "timestamp": "2023-08-01T12:34:56"
          |}""".stripMargin
      )

      val result = invalidJson.validate[EndRelationshipReason]

      result.isError mustBe true
      result match {
        case JsError(errors) =>
          errors.nonEmpty mustBe true
        case _ =>
          fail("Expected JsError for invalid JSON")
      }
    }

    "fail to deserialize from invalid date format" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "endReason": "Divorce",
          |  "dateOfDivorce": "2023/08/01",
          |  "timestamp": "2023-08-01T12:34:56"
          |}""".stripMargin
      )

      val result = invalidJson.validate[EndRelationshipReason]

      result.isError mustBe true
      result match {
        case JsError(errors) =>
          errors.head.toString should include("error.expected.date.isoformat")
        case _ =>
          fail("Expected JsError for invalid date format")
      }
    }
  }

}
