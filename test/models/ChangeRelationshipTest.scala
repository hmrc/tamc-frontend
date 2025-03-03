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
import play.api.libs.json.Json
import utils.BaseTest

import java.time.LocalDate

class ChangeRelationshipTest extends BaseTest {
  "ChangeRelationship" should {

    "serialize to JSON correctly" in {
      val changeRelationship = ChangeRelationship(
        role = Some("Transferor"),
        endReason = Some("Divorce"),
        historicActiveRecord = Some(true),
        creationTimestamp = Some("2023-11-01T10:15:30"),
        dateOfDivorce = Some(LocalDate.of(2023, 11, 1))
      )

      val json = Json.toJson(changeRelationship)
      val expectedJson = Json.parse(
        """
          {
            "role": "Transferor",
            "endReason": "Divorce",
            "historicActiveRecord": true,
            "creationTimestamp": "2023-11-01T10:15:30",
            "dateOfDivorce": "2023-11-01"
          }
        """
      )

      json mustEqual expectedJson
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          {
            "role": "Transferor",
            "endReason": "Divorce",
            "historicActiveRecord": true,
            "creationTimestamp": "2023-11-01T10:15:30",
            "dateOfDivorce": "2023-11-01"
          }
        """
      )

      val expectedChangeRelationship = ChangeRelationship(
        role = Some("Transferor"),
        endReason = Some("Divorce"),
        historicActiveRecord = Some(true),
        creationTimestamp = Some("2023-11-01T10:15:30"),
        dateOfDivorce = Some(LocalDate.of(2023, 11, 1))
      )

      json.as[ChangeRelationship] mustEqual expectedChangeRelationship
    }

    "handle missing optional fields during deserialization" in {
      val json = Json.parse(
        """
          {
            "role": "Transferor",
            "endReason": "Divorce"
          }
        """
      )

      val expectedChangeRelationship = ChangeRelationship(
        role = Some("Transferor"),
        endReason = Some("Divorce"),
        historicActiveRecord = Some(false),
        creationTimestamp = None,
        dateOfDivorce = None
      )

      json.as[ChangeRelationship] mustEqual expectedChangeRelationship
    }

    "handle empty optional fields during serialization" in {
      val changeRelationship = ChangeRelationship(
        role = None,
        endReason = None,
        historicActiveRecord = None,
        creationTimestamp = None,
        dateOfDivorce = None
      )

      val json = Json.toJson(changeRelationship)
      val expectedJson = Json.parse(
        """
          {}
        """
      )

      json mustEqual expectedJson
    }
  }
}
