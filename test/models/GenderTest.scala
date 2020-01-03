/*
 * Copyright 2020 HM Revenue & Customs
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

import controllers.ControllerBaseSpec
import play.api.libs.json.{JsResultException, JsString, Json}

class GenderTest extends ControllerBaseSpec {

  "Gender" should {
    "accept M as gender" in {
      Gender("M").gender shouldBe "M"
    }

    "accept F as gender" in {
      Gender("F").gender shouldBe "F"
    }

    "throw an exception" when {
      "a letter other than m or f is used" in {
        intercept[Exception] {
          Gender("A")
        }
      }
    }
  }

  "Gender reads" should {
    "return an exception" when {
      "an invalid gender is passed" in {
        intercept[JsResultException] {
          Json.parse("""{"gender":"B"}""").as[Gender]
        }
      }
    }

    "parse json" when {
      "a valid json is passed" in {
        Json.parse("""{"gender":"M"}""").as[Gender].gender shouldBe "M"
      }
    }
  }

  "Gender writes" should {
    "return gender in json" in {
      Json.toJson(Gender("F")) shouldBe JsString("F")
    }
  }
}
