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

package config

import uk.gov.hmrc.play.test.UnitSpec

class ApplicationConfigSpec extends UnitSpec {

  "check rates for earliest valid year" when {
    "return valid year" in {
      ApplicationConfig.TAMC_BEGINNING_YEAR should be(2016)
    }
  }

  "ggSignInUrl" must {
    "build ggSignInUrl" when {
      "has no continue" in {
        ApplicationConfig.ggSignInUrl(None) shouldBe "/gg/sign-in"
      }
      "has continue" in {
        ApplicationConfig.ggSignInUrl(Some("/tamc")) shouldBe "/gg/sign-in?continue=%2Ftamc"
      }
    }
  }

}
