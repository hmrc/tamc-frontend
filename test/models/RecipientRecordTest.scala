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

import java.time.LocalDate

class RecipientRecordTest extends BaseTest {
  val validDate = LocalDate.of(2023, 9, 5)
  "RegistrationFormInput" should {

    "serialize to JSON correctly" in {
      val input = RegistrationFormInput(
        name = "John",
        lastName = "Doe",
        gender = Gender("M"),
        nino = Nino("AA123456A"),
        dateOfMarriage = validDate
      )

      val json: JsValue = Json.toJson(input)

      json mustBe Json.parse(
        s"""{
           |  "nino": "AA123456A",
           |  "name": "John",
           |  "lastName": "Doe",
           |  "dateOfMarriage": "05/09/2023",
           |    "gender": "M"
           |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        s"""{
           |  "nino": "AA123456A",
           |  "name": "John",
           |  "lastName": "Doe",
           |  "dateOfMarriage": "05/09/2023",
           |    "gender": "M"
           |}""".stripMargin
      )

      val result = json.validate[RegistrationFormInput]

      result.isSuccess mustBe true
      result.get mustBe RegistrationFormInput(
        name = "John",
        lastName = "Doe",
        gender = Gender("M"),
        nino = Nino("AA123456A"),
        dateOfMarriage = validDate
      )
    }

    "fail to deserialize with an invalid date format" in {
      val json: JsValue = Json.parse(
        s"""{
           |  "nino": "AA123456A",
           |  "name": "John",
           |  "lastName": "Doe",
           |  "dateOfMarriage": "2023-09-05",
           |    "gender": "M"
           |}""".stripMargin
      )

      val result = json.validate[RegistrationFormInput]

      result.isError mustBe true
      result match {
        case JsError(errors) =>
          errors.head._2.head.message should include("error.expected.date.isoformat")
        case _ =>
          fail("Expected a JsError due to invalid date format")
      }
    }
  }

  "RecipientRecord" should {
    "serialize to JSON correctly" in {
      val recipientRecord = RecipientRecord(
        record = UserRecord(12345, "2023-10-06T12:00:00Z"),
        data = RegistrationFormInput(
          name = "John",
          lastName = "Doe",
          gender = Gender("M"),
          nino = Nino("AA123456A"),
          dateOfMarriage = validDate
        ),
        availableTaxYears = List(TaxYear(2023), TaxYear(2022))
      )

      val json: JsValue = Json.toJson(recipientRecord)

      json mustBe Json.parse(
        """{
          |"record": {
          |"cid":12345,
          |"timestamp":"2023-10-06T12:00:00Z"
          |},
          |"data": {
          |"nino":"AA123456A",
          |"name":"John",
          |"lastName":"Doe",
          |"dateOfMarriage":"05/09/2023",
          |"gender":"M"
          |},
          |"availableTaxYears": [
          |{ "year":2023 },
          |{ "year":2022 }
          |]}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |"record": {
          |"cid":12345,
          |"timestamp":"2023-10-06T12:00:00Z"
          |},
          |"data": {
          |"nino":"AA123456A",
          |"name":"John",
          |"lastName":"Doe",
          |"dateOfMarriage":"05/09/2023",
          |"gender":"M"
          |},
          |"availableTaxYears": [
          |{ "year":2023 },
          |{ "year":2022 }
          |]}""".stripMargin
      )

      val result = json.validate[RecipientRecord]

      result.isSuccess mustBe true
      result.get mustBe RecipientRecord(
        record = UserRecord(12345, "2023-10-06T12:00:00Z"),
        data = RegistrationFormInput(
          name = "John",
          lastName = "Doe",
          gender = Gender("M"),
          nino = Nino("AA123456A"),
          dateOfMarriage = validDate
        ),
        availableTaxYears = List(TaxYear(2023), TaxYear(2022))
      )
    }

    "serialize and deserialize symmetrically" in {
      val originalRecipientRecord = RecipientRecord(
        record = UserRecord(12345, "2023-10-06T12:00:00Z"),
        data = RegistrationFormInput(
          name = "John",
          lastName = "Doe",
          gender = Gender("M"),
          nino = Nino("AA123456A"),
          dateOfMarriage = validDate
        ),
        availableTaxYears = List(TaxYear(2019))
      )

      val json = Json.toJson(originalRecipientRecord)
      val deserialized = json.as[RecipientRecord]

      deserialized mustBe originalRecipientRecord
    }

    "handle empty availableTaxYears list correctly" in {
      val recipientRecord = RecipientRecord(
        record = UserRecord(12345, "2023-10-06T12:00:00Z"),
        data = RegistrationFormInput(
          name = "John",
          lastName = "Doe",
          gender = Gender("M"),
          nino = Nino("AA123456A"),
          dateOfMarriage = validDate
        ),
        availableTaxYears = List.empty
      )

      val json = Json.toJson(recipientRecord)

      json mustBe Json.parse(
        """{
          |"record": {
          |"cid":12345,
          |"timestamp":"2023-10-06T12:00:00Z"
          |},
          |"data": {
          |"nino":"AA123456A",
          |"name":"John",
          |"lastName":"Doe",
          |"dateOfMarriage":"05/09/2023",
          |"gender":"M"
          |},
          |"availableTaxYears": []}
          |""".stripMargin
      )

      val deserialized = json.as[RecipientRecord]

      deserialized mustBe recipientRecord
    }

    "fail to deserialize when required fields are missing" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "record": {
          |    "id": "123"
          |  },
          |  "data": {
          |    "email": "missing.fields@example.com",
          |    "firstName": "Missing"
          |  }
          |}""".stripMargin
      )

      val result = invalidJson.validate[RecipientRecord]

      result.isError mustBe true
    }
  }
}
