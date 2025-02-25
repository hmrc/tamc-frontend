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

import models.pertaxAuth.PertaxErrorView
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.{JsValue, Json, JsError}
import utils.BaseTest

class PertaxErrorViewTest extends BaseTest {

  "PertaxErrorView" should {

    "serialize to JSON correctly" in {
      val errorView = PertaxErrorView(statusCode = 404, url = "/unexpected-error")

      val json: JsValue = Json.toJson(errorView)

      json mustBe Json.parse(
        """{
          |  "statusCode": 404,
          |  "url": "/unexpected-error"
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "statusCode": 500,
          |  "url": "/internal-server-error"
          |}""".stripMargin
      )

      val result = json.validate[PertaxErrorView]

      result.isSuccess mustBe true
      result.get mustBe PertaxErrorView(statusCode = 500, url = "/internal-server-error")
    }

    "serialize and deserialize symmetrically" in {
      val originalErrorView = PertaxErrorView(statusCode = 400, url = "/bad-request")

      val json = Json.toJson(originalErrorView)
      val deserializedErrorView = json.as[PertaxErrorView]

      deserializedErrorView mustBe originalErrorView
    }

    "fail to deserialize with invalid JSON" in {
      val invalidJson: JsValue = Json.parse(
        """{
          |  "status": 403,
          |  "link": "/forbidden"
          |}""".stripMargin
      )

      val result = invalidJson.validate[PertaxErrorView]

      result.isError mustBe true
      result match {
        case error: JsError =>
          error.errors.nonEmpty mustBe true
        case _ =>
          fail("Expected JsError for invalid JSON")
      }
    }
  }
}

