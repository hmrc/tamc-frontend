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

package controllers

import org.jsoup.Jsoup
import play.api.http.Status.OK
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ControllerBaseTest

class HowItWorksControllerTest extends ControllerBaseTest {

  def howItWorksController: HowItWorksController = instanceOf[HowItWorksController]

  "HowItWorksController" should {
    "direct a PTA user to How It Works page" in {
      val result = await(howItWorksController.howItWorks()(request))
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Apply for Marriage Allowance - Marriage Allowance - GOV.UK"

      val heading = document.getElementById("pageHeading").text
      heading shouldBe "Apply for Marriage Allowance"

      val button = document.getElementById("get-started")
      button.text shouldBe "Apply now"
    }

    "redirect a Govuk signed in user to Transfer Allowance page" in {
      val request: Request[AnyContent] = FakeRequest("GET", "/marriage-allowance-application/how-it-works")
        .withHeaders("Referer" -> "https://www.gov.uk/")

      val result = await(howItWorksController.howItWorks()(request))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.TransferController.transfer.url)
    }
  }

  "home" should {
    "redirect the user" in {
      val result = howItWorksController.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.HowItWorksController.howItWorks.url)
    }
  }
}
