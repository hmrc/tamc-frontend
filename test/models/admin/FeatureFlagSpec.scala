/*
 * Copyright 2023 HM Revenue & Customs
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

package models.admin

import play.api.libs.json.{JsResultException, JsString, Json}
import utils.UnitSpec

class FeatureFlagSpec extends UnitSpec {

  val testFlag: FeatureFlagName = SCAWrapperToggle

  "read json" in {
    JsString(testFlag.toString).as[FeatureFlagName] shouldBe testFlag
  }

  "throw an exception if name is invalid" in {
    val result = intercept[JsResultException] {
      JsString("invalid").as[FeatureFlagName]
    }

    result.getMessage should include("Unknown FeatureFlagName `\"invalid\"`")
  }

  "write json" in {
    Json.toJson(testFlag).toString shouldBe s""""${testFlag.toString}""""
  }

  "String binds to a Right(FeatureFlagName)" in {
    FeatureFlagName.pathBindable.bind("key", testFlag.toString) shouldBe Right(testFlag)
  }

  "Invalid string binds to a Left" in {
    val name = "invalid"
    FeatureFlagName.pathBindable.bind("key", name) shouldBe Left(s"The feature flag `$name` does not exist")
  }

  "FeatureFlagName unbinds to a string" in {
    FeatureFlagName.pathBindable.unbind("aa", testFlag) shouldBe testFlag.toString
  }

}