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
import utils.{BaseTest, EmailAddress}

class CreateRelationshipRequestHolderTest extends BaseTest {
  "CreateRelationshipRequestHolder" should {

    "serialize to JSON correctly" in {
      val request = CreateRelationshipRequest(
        transferor_cid = 12345L,
        transferor_timestamp = "2023-10-01T12:00:00",
        recipient_cid = 67890L,
        recipient_timestamp = "2023-10-01T12:30:00",
        taxYears = List(2023, 2024)
      )

      val notification = CreateRelationshipNotificationRequest(
        full_name = "John Doe",
        email = EmailAddress("john.doe@example.com"),
        welsh = true
      )

      val holder = CreateRelationshipRequestHolder(request, notification)

      val expectedJson: JsValue = Json.parse(
        """
          {
             "request": {
                "transferor_cid": 12345,
                "transferor_timestamp": "2023-10-01T12:00:00",
                "recipient_cid": 67890,
                "recipient_timestamp": "2023-10-01T12:30:00",
                "taxYears": [2023, 2024]
             },
             "notification": {
                "full_name": "John Doe",
                "email": "john.doe@example.com",
                "welsh": true
             }
          }
        """
      )

      Json.toJson(holder) shouldEqual expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          {
             "request": {
                "transferor_cid": 12345,
                "transferor_timestamp": "2023-10-01T12:00:00",
                "recipient_cid": 67890,
                "recipient_timestamp": "2023-10-01T12:30:00",
                "taxYears": [2023, 2024]
             },
             "notification": {
                "full_name": "John Doe",
                "email": "john.doe@example.com",
                "welsh": true
             }
          }
        """
      )

      val expectedHolder = CreateRelationshipRequestHolder(
        request = CreateRelationshipRequest(
          transferor_cid = 12345L,
          transferor_timestamp = "2023-10-01T12:00:00",
          recipient_cid = 67890L,
          recipient_timestamp = "2023-10-01T12:30:00",
          taxYears = List(2023, 2024)
        ),
        notification = CreateRelationshipNotificationRequest(
          full_name = "John Doe",
          email = EmailAddress("john.doe@example.com"),
          welsh = true
        )
      )

      json.as[CreateRelationshipRequestHolder] shouldEqual expectedHolder
    }

    "fail deserialization when JSON is missing fields" in {
      val invalidJson: JsValue = Json.parse(
        """
          {
             "request": {
                "transferor_cid": 12345,
                "transferor_timestamp": "2023-10-01T12:00:00",
                "recipient_cid": 67890
             },
             "notification": {
                "full_name": "John Doe",
                "email": "john.doe@example.com",
                "welsh": true
             }
          }
        """
      )

      an[Exception] should be thrownBy invalidJson.as[CreateRelationshipRequestHolder]
    }
  }
}
