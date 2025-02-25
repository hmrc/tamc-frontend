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
import utils.BaseTest

class TaxBandTest extends BaseTest {
  "TaxBand" should {
    "serialize to JSON correctly" in {
      val taxBand = TaxBand(name = "Basic", lowerThreshold = 12000, upperThreshold = 50000, rate = 20.0)

      val json: JsValue = Json.toJson(taxBand)

      json mustBe Json.parse(
        """{
          |  "name": "Basic",
          |  "lowerThreshold": 12000,
          |  "upperThreshold": 50000,
          |  "rate": 20.0
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "name": "Higher",
          |  "lowerThreshold": 50001,
          |  "upperThreshold": 100000,
          |  "rate": 40.0
          |}""".stripMargin
      )

      val taxBand = json.as[TaxBand]

      taxBand mustBe TaxBand(name = "Higher", lowerThreshold = 50001, upperThreshold = 100000, rate = 40.0)
    }

    "calculate the diffBetweenLowerAndUpperThreshold correctly" in {
      val taxBand = TaxBand(name = "Basic", lowerThreshold = 10000, upperThreshold = 40000, rate = 20.0)

      taxBand.diffBetweenLowerAndUpperThreshold mustBe 30000
    }
  }

  "CountryTaxBands" should {
    "serialize to JSON correctly" in {
      val countryTaxBands = CountryTaxBands(
        taxBands = List(
          TaxBand(name = "Basic", lowerThreshold = 0, upperThreshold = 12500, rate = 10.0),
          TaxBand(name = "Higher", lowerThreshold = 12501, upperThreshold = 50000, rate = 20.0)
        )
      )

      val json: JsValue = Json.toJson(countryTaxBands)

      json mustBe Json.parse(
        """{
          |  "taxBands": [
          |    {
          |      "name": "Basic",
          |      "lowerThreshold": 0,
          |      "upperThreshold": 12500,
          |      "rate": 10.0
          |    },
          |    {
          |      "name": "Higher",
          |      "lowerThreshold": 12501,
          |      "upperThreshold": 50000,
          |      "rate": 20.0
          |    }
          |  ]
          |}""".stripMargin
      )
    }

    "deserialize from JSON correctly" in {
      val json: JsValue = Json.parse(
        """{
          |  "taxBands": [
          |    {
          |      "name": "Basic",
          |      "lowerThreshold": 0,
          |      "upperThreshold": 12500,
          |      "rate": 10.0
          |    },
          |    {
          |      "name": "Higher",
          |      "lowerThreshold": 12501,
          |      "upperThreshold": 50000,
          |      "rate": 20.0
          |    }
          |  ]
          |}""".stripMargin
      )

      val countryTaxBands = json.as[CountryTaxBands]

      countryTaxBands mustBe CountryTaxBands(
        taxBands = List(
          TaxBand(name = "Basic", lowerThreshold = 0, upperThreshold = 12500, rate = 10.0),
          TaxBand(name = "Higher", lowerThreshold = 12501, upperThreshold = 50000, rate = 20.0)
        )
      )
    }

    "serialize and deserialize symmetrically" in {
      val original = CountryTaxBands(
        taxBands = List(
          TaxBand(name = "Basic", lowerThreshold = 0, upperThreshold = 12500, rate = 10.0),
          TaxBand(name = "Higher", lowerThreshold = 12501, upperThreshold = 50000, rate = 20.0)
        )
      )

      val json = Json.toJson(original)
      val deserialized = json.as[CountryTaxBands]

      deserialized mustBe original
    }
  }
}
