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

import org.scalatest.matchers.must.Matchers.mustEqual
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, EmailAddress}

import java.time.LocalDate

class UserAnswersCacheDataTest extends BaseTest {
  "UserAnswersCacheData" should {

    "serialize to JSON correctly" in {
      val userAnswers = UserAnswersCacheData(
        transferor = Some(UserRecord(12345L, "2023-10-12T10:15:30")),
        recipient = Some(RecipientRecord(
          record = UserRecord(54321L, "2023-10-11T09:30:00"),
          data = RegistrationFormInput("John", "Doe", Gender("M"), Nino("AA123456A"), LocalDate.of(2020, 10, 12)),
          availableTaxYears = List(TaxYear(2023), TaxYear(2024))
        )),
        notification = Some(NotificationRecord(EmailAddress("john.doe@example.com"))),
        relationshipCreated = Some(true),
        selectedYears = Some(List(2023, 2024)),
        recipientDetailsFormData = Some(RecipientDetailsFormInput("Jane", "Doe", Gender("F"), Nino("BB987654B"))),
        dateOfMarriage = Some(DateOfMarriageFormInput(LocalDate.of(2015, 7, 20)))
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "recipient": {
          |    "record": {
          |      "cid": 54321,
          |      "timestamp": "2023-10-11T09:30:00"
          |    },
          |    "data": {
          |      "nino": "AA123456A",
          |      "name": "John",
          |      "lastName": "Doe",
          |      "dateOfMarriage": "12/10/2020",
          |      "gender": "M"
          |    },
          |    "availableTaxYears": [
          |      {
          |        "year": 2023
          |      },
          |      {
          |        "year": 2024
          |      }
          |    ]
          |  },
          |  "relationshipCreated": true,
          |  "selectedYears": [
          |    2023,
          |    2024
          |  ],
          |  "dateOfMarriage": {
          |    "dateOfMarriage": "20/07/2015"
          |  },
          |  "transferor": {
          |    "cid": 12345,
          |    "timestamp": "2023-10-12T10:15:30"
          |  },
          |  "notification": {
          |    "transferor_email": "john.doe@example.com"
          |  },
          |  "recipientDetailsFormData": {
          |    "name": "Jane",
          |    "lastName": "Doe",
          |    "gender": "F",
          |    "nino": "BB987654B"
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(userAnswers) shouldEqual expectedJson
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "recipient": {
          |    "record": {
          |      "cid": 54321,
          |      "timestamp": "2023-10-11T09:30:00"
          |    },
          |    "data": {
          |      "nino": "AA123456A",
          |      "name": "John",
          |      "lastName": "Doe",
          |      "dateOfMarriage": "12/10/2020",
          |      "gender": "M"
          |    },
          |    "availableTaxYears": [
          |      {
          |        "year": 2023
          |      },
          |      {
          |        "year": 2024
          |      }
          |    ]
          |  },
          |  "relationshipCreated": true,
          |  "selectedYears": [
          |    2023,
          |    2024
          |  ],
          |  "dateOfMarriage": {
          |    "dateOfMarriage": "20/07/2015"
          |  },
          |  "transferor": {
          |    "cid": 12345,
          |    "timestamp": "2023-10-12T10:15:30"
          |  },
          |  "notification": {
          |    "transferor_email": "john.doe@example.com"
          |  },
          |  "recipientDetailsFormData": {
          |    "name": "Jane",
          |    "lastName": "Doe",
          |    "gender": "F",
          |    "nino": "BB987654B"
          |  }
          |}
          |""".stripMargin
      )

      val expectedUserAnswers = UserAnswersCacheData(
        transferor = Some(UserRecord(12345L, "2023-10-12T10:15:30")),
        recipient = Some(RecipientRecord(
          record = UserRecord(54321L, "2023-10-11T09:30:00"),
          data = RegistrationFormInput("John", "Doe", Gender("M"), Nino("AA123456A"), LocalDate.of(2020, 10, 12)),
          availableTaxYears = List(TaxYear(2023), TaxYear(2024))
        )),
        notification = Some(NotificationRecord(EmailAddress("john.doe@example.com"))),
        relationshipCreated = Some(true),
        selectedYears = Some(List(2023, 2024)),
        recipientDetailsFormData = Some(RecipientDetailsFormInput("Jane", "Doe", Gender("F"), Nino("BB987654B"))),
        dateOfMarriage = Some(DateOfMarriageFormInput(LocalDate.of(2015, 7, 20)))
      )

      json.as[UserAnswersCacheData] shouldEqual expectedUserAnswers
    }

    "handle missing optional fields during deserialization" in {
      val minimalJson: JsValue = Json.parse(
        """
           {
             "notification": {
               "transferor_email": "john.doe@example.com"
             }
           }
         """
      )

      val expectedUserAnswers = UserAnswersCacheData(
        transferor = None,
        recipient = None,
        notification = Some(NotificationRecord(EmailAddress("john.doe@example.com"))),
        relationshipCreated = None,
        selectedYears = None,
        recipientDetailsFormData = None,
        dateOfMarriage = None
      )

      minimalJson.as[UserAnswersCacheData] shouldEqual expectedUserAnswers
    }

    "fail deserialization when mandatory fields are missing" in {
      val invalidJson: JsValue = Json.parse(
        """
           {
             "recipient": {
               "record": {
                 "cid": 54321,
                 "timestamp": "2023-10-11T09:30:00"
               }
             }
           }
         """
      )

      an[Exception] should be thrownBy invalidJson.as[UserAnswersCacheData]
    }

    "serialize and deserialize mixed-composition UserAnswersCacheData" in {
      val userAnswers = UserAnswersCacheData(
        transferor = None,
        recipient = Some(RecipientRecord(
          record = UserRecord(54321L, "2023-10-11T09:30:00"),
          data = RegistrationFormInput("John", "Doe", Gender("M"), Nino("AA123456A"), LocalDate.of(2000, 3, 25)),
          availableTaxYears = List()
        )),
        notification = None,
        relationshipCreated = Some(false),
        selectedYears = Some(List(2022)),
        recipientDetailsFormData = None,
        dateOfMarriage = Some(DateOfMarriageFormInput(LocalDate.of(2010, 6, 15)))
      )

      val expectedJson: JsValue = Json.parse(
        """
          |{
          |  "recipient": {
          |    "record": {
          |      "cid": 54321,
          |      "timestamp": "2023-10-11T09:30:00"
          |    },
          |    "data": {
          |      "nino": "AA123456A",
          |      "name": "John",
          |      "lastName": "Doe",
          |      "dateOfMarriage": "25/03/2000",
          |      "gender": "M"
          |    },
          |    "availableTaxYears": []
          |  },
          |  "relationshipCreated": false,
          |  "selectedYears": [
          |    2022
          |  ],
          |  "dateOfMarriage": {
          |    "dateOfMarriage": "15/06/2010"
          |  }
          |}
          |""".stripMargin
      )

      Json.toJson(userAnswers) shouldEqual expectedJson
    }
  }
  "EligibilityCheckCacheData" should {

    val eligibilityCheckData = EligibilityCheckCacheData(
      loggedInUserInfo = Some(LoggedInUserInfo(12345, "user@example.com")),
      roleRecord = Some("Recipient"),
      activeRelationshipRecord = Some(RelationshipRecord("Recipient", "creationTimestamp", "20220101", None, None, "otherPaticipant", "otherParticipantupdateTimestamp")),
      historicRelationships = Some(Seq(
        RelationshipRecord("Recipient", "creationTimestamp", "20220101", None, None, "otherPaticipant", "otherParticipantupdateTimestamp"),
        RelationshipRecord("Recipient", "creationTimestamp", "20220101", None, None, "otherPaticipant", "otherParticipantupdateTimestamp")
      )),
      notification = Some(NotificationRecord(EmailAddress("notify@example.com"))),
      relationshipEndReasonRecord = Some(EndRelationshipReason("DIV")),
      relationshipUpdated = Some(true)
    )

    "serialize to JSON correctly" in {


      val json = Json.toJson(eligibilityCheckData)
      val expectedJson = Json.parse(
        """{
          |  "relationshipEndReasonRecord": {
          |    "endReason": "DIV"
          |  },
          |  "relationshipUpdated": true,
          |  "activeRelationshipRecord": {
          |    "participant": "Recipient",
          |    "creationTimestamp": "creationTimestamp",
          |    "otherParticipantUpdateTimestamp": "otherParticipantupdateTimestamp",
          |    "participant1StartDate": "20220101",
          |    "otherParticipantInstanceIdentifier": "otherPaticipant"
          |  },
          |  "loggedInUserInfo": {
          |    "cid": 12345,
          |    "timestamp": "user@example.com"
          |  },
          |  "roleRecord": "Recipient",
          |  "notification": {
          |    "transferor_email": "notify@example.com"
          |  },
          |  "historicRelationships": [
          |    {
          |      "participant": "Recipient",
          |      "creationTimestamp": "creationTimestamp",
          |      "otherParticipantUpdateTimestamp": "otherParticipantupdateTimestamp",
          |      "participant1StartDate": "20220101",
          |      "otherParticipantInstanceIdentifier": "otherPaticipant"
          |    },
          |    {
          |      "participant": "Recipient",
          |      "creationTimestamp": "creationTimestamp",
          |      "otherParticipantUpdateTimestamp": "otherParticipantupdateTimestamp",
          |      "participant1StartDate": "20220101",
          |      "otherParticipantInstanceIdentifier": "otherPaticipant"
          |    }
          |  ]
          |}""".stripMargin
      )

      json mustEqual expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "relationshipEndReasonRecord": {
          |    "endReason": "DIV"
          |  },
          |  "relationshipUpdated": true,
          |  "activeRelationshipRecord": {
          |    "participant": "Recipient",
          |    "creationTimestamp": "creationTimestamp",
          |    "otherParticipantUpdateTimestamp": "otherParticipantupdateTimestamp",
          |    "participant1StartDate": "20220101",
          |    "otherParticipantInstanceIdentifier": "otherPaticipant"
          |  },
          |  "loggedInUserInfo": {
          |    "cid": 12345,
          |    "timestamp": "user@example.com"
          |  },
          |  "roleRecord": "Recipient",
          |  "notification": {
          |    "transferor_email": "notify@example.com"
          |  },
          |  "historicRelationships": [
          |    {
          |      "participant": "Recipient",
          |      "creationTimestamp": "creationTimestamp",
          |      "otherParticipantUpdateTimestamp": "otherParticipantupdateTimestamp",
          |      "participant1StartDate": "20220101",
          |      "otherParticipantInstanceIdentifier": "otherPaticipant"
          |    },
          |    {
          |      "participant": "Recipient",
          |      "creationTimestamp": "creationTimestamp",
          |      "otherParticipantUpdateTimestamp": "otherParticipantupdateTimestamp",
          |      "participant1StartDate": "20220101",
          |      "otherParticipantInstanceIdentifier": "otherPaticipant"
          |    }
          |  ]
          |}""".stripMargin
      )

      json.as[EligibilityCheckCacheData] mustEqual eligibilityCheckData
    }

    "handle missing optional fields during deserialization" in {
      val json = Json.parse(
        """
           {
             "roleRecord": "Recipient"
           }
         """
      )

      val expectedData = EligibilityCheckCacheData(
        loggedInUserInfo = None,
        roleRecord = Some("Recipient"),
        activeRelationshipRecord = None,
        historicRelationships = None,
        notification = None,
        relationshipEndReasonRecord = None,
        relationshipUpdated = None
      )

      json.as[EligibilityCheckCacheData] mustEqual expectedData
    }

    "handle empty optional fields during serialization" in {
      val eligibilityCheckData = EligibilityCheckCacheData(
        loggedInUserInfo = None,
        roleRecord = None,
        activeRelationshipRecord = None,
        historicRelationships = None,
        notification = None,
        relationshipEndReasonRecord = None,
        relationshipUpdated = None
      )

      val json = Json.toJson(eligibilityCheckData)
      val expectedJson = Json.parse("{}")

      json mustEqual expectedJson
    }
  }
}
