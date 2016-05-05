/*
 * Copyright 2016 HM Revenue & Customs
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

package controllers

import scala.concurrent.Future
import org.jsoup.Jsoup
import models.CacheData
import models.CitizenName
import models.Gender
import models.RecipientRecord
import models.RegistrationFormInput
import models.UserRecord
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.OK
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers.session
import play.api.test.WithApplication
import test_utils.TestUtility
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import models.NotificationRecord
import events.CreateRelationshipSuccessEvent
import play.api.mvc.Cookie
import test_utils.TestConstants
import test_utils.TestData.Cids

class BreadcrumbTest extends UnitSpec with TestUtility {

  "When PTA journey is enabled, Breadcrumb" should {

    /*
     * FIXME instead of checking individual breadcrumb item, we should check if whole breadcrumb
     * component item is available on the page i.e.
     * `val breadcrumbToTest = <...>`
     * `document.getElementById("global-breadcrumb") shouldBe breadcrumbToTest`
     */
    val accountLink = "<li><a href=\"/personal-account\">Account Home</a></li>"
    val incomeTaxLink = "<li><a href=\"/check-income-tax/income-tax\">Income Tax</a></li>"
    val tfaLink = "<li><a href=\"#\">Tax-free allowance</a></li>"

    "be visible on tranfer page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("global-breadcrumb").toString() should include(accountLink)
      document.getElementById("global-breadcrumb").toString() should include(incomeTaxLink)
    }
  }
}
