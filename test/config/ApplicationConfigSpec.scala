/*
 * Copyright 2021 HM Revenue & Customs
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
      ApplicationConfig.appConfig.TAMC_BEGINNING_YEAR should be(2016)
    }
  }

  "ggSignInUrl" must {
    "build ggSignInUrl and encode continue url" in {
      ApplicationConfig.appConfig.ggSignInUrl shouldBe "http://localhost:9025/gg/sign-in?continue=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fhistory"
    }
  }

}
