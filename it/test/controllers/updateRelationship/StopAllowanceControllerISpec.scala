/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.updateRelationship

import play.api.test.Helpers.*
import utils.IntegrationSpec

class StopAllowanceControllerISpec extends IntegrationSpec {

  "StopAllowanceController" must {

    "render the stop allowance page" in {
      val result = route(app, request("/stop-allowance")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Your partner needs to cancel Marriage Allowance")
      doc must include("If you want to change who benefits")
      doc must include("ask your partner to cancel Marriage Allowance")
      doc must include("apply for Marriage Allowance after 6 April")
      doc must include("Return to your Marriage Allowance summary")
    }

    "render the cancel page" in {
      val result = route(app, request("/cancel")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Cancelling Marriage Allowance")
      doc must include("We will cancel your Marriage Allowance")
    }
  }
}
