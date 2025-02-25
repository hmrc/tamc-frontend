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
import uk.gov.hmrc.domain.Nino
import utils.BaseTest

class RecipientDetailsFormInputTest extends BaseTest {

  "RecipientDetailsFormInput" should {
    "serialize to JSON correctly" in {
      val input = RecipientDetailsFormInput(
        name = "John",
        lastName = "Doe",
        gender = Gender("M"),
        nino = Nino("AA123456A")
      )

      val json: JsValue = Json.toJson(input)

      json mustBe Json.parse(
        """{
          |  "name": "John",
          |  "lastName": "Doe",
          |  "gender": "M",
          |  "nino": "AA123456A"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "name": "John",
          |  "lastName": "Doe",
          |  "gender": "M",
          |  "nino": "AA123456A"
          |}""".stripMargin
      )

      val result = json.validate[RecipientDetailsFormInput]

      result.isSuccess mustBe true
      result.get mustBe RecipientDetailsFormInput(
        name = "John",
        lastName = "Doe",
        gender = Gender("M"),
        nino = Nino("AA123456A")
      )
    }

    "serialize and deserialize symmetrically" in {
      val originalInput = RecipientDetailsFormInput(
        name = "Jane",
        lastName = "Smith",
        gender = Gender("F"),
        nino = Nino("BB654321B")
      )

      val json = Json.toJson(originalInput)
      val deserializedInput = json.as[RecipientDetailsFormInput]

      deserializedInput mustBe originalInput
    }

    "handle invalid JSON with missing fields properly" in {
      val incompleteJson: JsValue = Json.parse(
        """{
          |  "name": "John",
          |  "gender": "M"
          |}""".stripMargin
      )

      val result = incompleteJson.validate[RecipientDetailsFormInput]

      result.isError mustBe true
      result match {
        case JsError(errors) =>
          errors.nonEmpty mustBe true
        case _ =>
          fail("Expected JsError for invalid JSON")
      }
    }

    "fail to deserialize when gender is invalid" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "name": "John",
          |  "lastName": "Doe",
          |  "gender": "Other",
          |  "nino": "AA123456A"
          |}""".stripMargin
      )

      val result = invalidJson.validate[RecipientDetailsFormInput]

      result.isError mustBe true
      result match {
        case JsError(errors) =>
          errors.head._2.head.message should include ("Other")
        case _ =>
          fail("Expected JsError for invalid gender field")
      }
    }
  }
}

