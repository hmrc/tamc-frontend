/*
 * Copyright 2018 HM Revenue & Customs
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

import models.{CacheData, UserRecord}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.mvc.Cookie
import play.api.test.Helpers.{OK, contentAsString, defaultAwaitTimeout}
import test_utils.TestData.Cids
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.play.test.UnitSpec

class BreadcrumbTest extends UnitSpec with TestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

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

    "be visible on tranfer page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
/*      document.getElementById("global-breadcrumb").toString() should include(accountLink)
      document.getElementById("global-breadcrumb").toString() should include(incomeTaxLink)*/
    }
  }
}
