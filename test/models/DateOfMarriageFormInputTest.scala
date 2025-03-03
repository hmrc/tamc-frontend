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
import play.api.libs.json.{JsError, JsValue, Json}
import utils.BaseTest

import java.time.LocalDate

class DateOfMarriageFormInputTest extends BaseTest {

  "DateOfMarriageFormInput" should {
    "serialize to JSON correctly" in {
      val formInput = DateOfMarriageFormInput(
        dateOfMarriage = LocalDate.of(2023, 8, 1)
      )

      val json: JsValue = Json.toJson(formInput)

      json mustBe Json.parse(
        """{
          |  "dateOfMarriage": "01/08/2023"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "dateOfMarriage": "01/08/2023"
          |}""".stripMargin
      )

      val result = json.validate[DateOfMarriageFormInput]

      result.isSuccess mustBe true
      result.get mustBe DateOfMarriageFormInput(
        dateOfMarriage = LocalDate.of(2023, 8, 1)
      )
    }

    "serialize and deserialize symmetrically" in {
      val originalFormInput = DateOfMarriageFormInput(
        dateOfMarriage = LocalDate.of(2021, 5, 12)
      )

      val json = Json.toJson(originalFormInput)
      val deserializedFormInput = json.as[DateOfMarriageFormInput]

      deserializedFormInput mustBe originalFormInput
    }

    "fail to deserialize when the dateOfMarriage field is missing" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "somethingElse": "value"
          |}""".stripMargin
      )

      val result = invalidJson.validate[DateOfMarriageFormInput]

      result.isError mustBe true
      result match {
        case e: JsError =>
          e.errors.nonEmpty mustBe true
          e.errors.head._1.toString() should include("dateOfMarriage")
        case _ =>
          fail("Expected JsError for missing required field")
      }
    }

    "fail to deserialize when the dateOfMarriage field is in an invalid format" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "dateOfMarriage": "2023-08-01"
          |}""".stripMargin
      )

      val result = invalidJson.validate[DateOfMarriageFormInput]

      result.isError mustBe true
      result match {
        case JsError(errors) =>
          errors.head._2.head.message should include("error.expected.date.isoformat")
        case _ =>
          fail("Expected JsError for invalid date format")
      }
    }
  }
}
