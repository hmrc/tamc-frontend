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

import play.api.libs.json.{JsValue, Json}
import utils.BaseTest

class RecipientInformationTest extends BaseTest {
  "RecipientInformation" should {

    "serialize to JSON correctly" in {
      val recipientInformation = RecipientInformation(
        instanceIdentifier = "12345",
        updateTimestamp = "2023-10-12T14:30:00"
      )

      val expectedJson: JsValue = Json.parse(
        """
            {
              "instanceIdentifier": "12345",
              "updateTimestamp": "2023-10-12T14:30:00"
            }
          """
      )

      Json.toJson(recipientInformation) shouldEqual expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
            {
              "instanceIdentifier": "12345",
              "updateTimestamp": "2023-10-12T14:30:00"
            }
          """
      )

      val expectedRecipientInformation = RecipientInformation(
        instanceIdentifier = "12345",
        updateTimestamp = "2023-10-12T14:30:00"
      )

      json.as[RecipientInformation] shouldEqual expectedRecipientInformation
    }

    "fail deserialization when JSON is missing fields" in {
      val invalidJson: JsValue = Json.parse(
        """
            {
              "instanceIdentifier": "12345"
            }
          """
      )

      an[Exception] should be thrownBy invalidJson.as[RecipientInformation]
    }

    "fail deserialization when JSON has invalid field types" in {
      val invalidJson: JsValue = Json.parse(
        """
            {
              "instanceIdentifier": 12345,
              "updateTimestamp": 2023
            }
          """
      )

      an[Exception] should be thrownBy invalidJson.as[RecipientInformation]
    }
  }
}
