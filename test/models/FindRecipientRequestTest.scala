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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino
import utils.BaseTest

class FindRecipientRequestTest extends BaseTest {
  "FindRecipientRequest" should {

    "serialize to JSON correctly" in {
      val request = FindRecipientRequest(
        name = "John",
        lastName = "Doe",
        gender = Gender("M"),
        nino = Nino("AB123456C")
      )

      val json: JsValue = Json.toJson(request)

      json mustBe Json.parse(
        """{
          |  "name": "John",
          |  "lastName": "Doe",
          |  "gender": "M",
          |  "nino": "AB123456C"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "name": "Jane",
          |  "lastName": "Smith",
          |  "gender": "F",
          |  "nino": "AB123456C"
          |}""".stripMargin
      )

      val request = json.as[FindRecipientRequest]

      request mustBe FindRecipientRequest(
        name = "Jane",
        lastName = "Smith",
        gender = Gender("F"),
        nino = Nino("AB123456C")
      )
    }
  }
}
