/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.test.Helpers.baseApplicationBuilder.injector
import utils.UnitSpec

class ApplicationConfigSpec extends UnitSpec {

  val applicationConfig: ApplicationConfig = injector().instanceOf[ApplicationConfig]

  "check rates for earliest valid year" when {
    "return valid years prior" in {
      applicationConfig.TAMC_VALID_YEARS should be(4)
    }
  }

  "ggSignInUrl" must {
    "build ggSignInUrl and encode continue url" in {
      applicationConfig.ggSignInUrl shouldBe "http://localhost:9553/bas-gateway/sign-in?continue_url=http%3A%2F%2Flocalhost%3A9900%2Fmarriage-allowance-application%2Fhistory"
    }
  }

}
