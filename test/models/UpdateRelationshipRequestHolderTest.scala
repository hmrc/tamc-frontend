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

class UpdateRelationshipRequestHolderTest extends BaseTest {
  "UpdateRelationshipRequestHolder" should {
    "serialize to JSON correctly" in {
      val recipientInfo = RecipientInformation(
        instanceIdentifier = "RECIPIENT-001",
        updateTimestamp = "2023-10-12T10:15:30"
      )

      val transferorInfo = TransferorInformation(
        updateTimestamp = "2023-10-12T10:15:30"
      )

      val relationshipInfo = RelationshipInformation(
        creationTimestamp = "2023-05-01T12:00:00",
        relationshipEndReason = "Divorce",
        actualEndDate = "2023-10-01T12:00:00"
      )

      val request = UpdateRelationshipRequest(
        participant1 = recipientInfo,
        participant2 = transferorInfo,
        relationship = relationshipInfo
      )

      val notification = UpdateRelationshipNotificationRequest(
        full_name = "John Doe",
        email = EmailAddress("john.doe@example.com"),
        role = "Transferor",
        welsh = true,
        isRetrospective = false
      )

      val holder = UpdateRelationshipRequestHolder(request, notification)

      val expectedJson: JsValue = Json.parse(
        """
          {
            "request": {
              "participant1": {
                "instanceIdentifier": "RECIPIENT-001",
                "updateTimestamp": "2023-10-12T10:15:30"
              },
              "participant2": {
                "updateTimestamp": "2023-10-12T10:15:30"
              },
              "relationship": {
                "creationTimestamp": "2023-05-01T12:00:00",
                "relationshipEndReason": "Divorce",
                "actualEndDate": "2023-10-01T12:00:00"
              }
            },
            "notification": {
              "full_name": "John Doe",
              "email": "john.doe@example.com",
              "role": "Transferor",
              "welsh": true,
              "isRetrospective": false
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
              "participant1": {
                "instanceIdentifier": "RECIPIENT-001",
                "updateTimestamp": "2023-10-12T10:15:30"
              },
              "participant2": {
                "updateTimestamp": "2023-10-12T10:15:30"
              },
              "relationship": {
                "creationTimestamp": "2023-05-01T12:00:00",
                "relationshipEndReason": "Divorce",
                "actualEndDate": "2023-10-01T12:00:00"
              }
            },
            "notification": {
              "full_name": "John Doe",
              "email": "john.doe@example.com",
              "role": "Transferor",
              "welsh": true,
              "isRetrospective": false
            }
          }
        """
      )

      val expectedHolder = UpdateRelationshipRequestHolder(
        request = UpdateRelationshipRequest(
          participant1 = RecipientInformation(
            instanceIdentifier = "RECIPIENT-001",
            updateTimestamp = "2023-10-12T10:15:30"
          ),
          participant2 = TransferorInformation(
            updateTimestamp = "2023-10-12T10:15:30"
          ),
          relationship = RelationshipInformation(
            creationTimestamp = "2023-05-01T12:00:00",
            relationshipEndReason = "Divorce",
            actualEndDate = "2023-10-01T12:00:00"
          )
        ),
        notification = UpdateRelationshipNotificationRequest(
          full_name = "John Doe",
          email = EmailAddress("john.doe@example.com"),
          role = "Transferor",
          welsh = true,
          isRetrospective = false
        )
      )

      json.as[UpdateRelationshipRequestHolder] shouldEqual expectedHolder
    }

    "fail deserialization when JSON is missing fields" in {
      val invalidJson: JsValue = Json.parse(
        """
          {
            "request": {
              "participant1": {
                "instanceIdentifier": "RECIPIENT-001"
              },
              "participant2": {
                "updateTimestamp": "2023-10-12T10:15:30"
              }
            },
            "notification": {
              "full_name": "John Doe",
              "email": "john.doe@example.com",
              "role": "Transferor"
            }
          }
        """
      )

      an[Exception] should be thrownBy invalidJson.as[UpdateRelationshipRequestHolder]
    }

    "fail deserialization when JSON has invalid field types" in {
      val invalidJson: JsValue = Json.parse(
        """
          {
            "request": {
              "participant1": {
                "instanceIdentifier": 12345,
                "updateTimestamp": 2023
              },
              "participant2": {
                "updateTimestamp": "2023-10-12T10:15:30"
              },
              "relationship": {
                "creationTimestamp": "2023-05-01T12:00:00",
                "relationshipEndReason": "Divorce",
                "actualEndDate": "2023-10-01T12:00:00"
              }
            },
            "notification": {
              "full_name": "John Doe",
              "email": "john.doe@example.com",
              "role": "Transferor",
              "welsh": "true",
              "isRetrospective": "false"
            }
          }
        """
      )

      an[Exception] should be thrownBy invalidJson.as[UpdateRelationshipRequestHolder]
    }
  }
}
