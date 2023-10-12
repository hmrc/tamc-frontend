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
import play.api.mvc.Result
import play.api.test.Helpers._
import utils.ControllerBaseTest

import scala.concurrent.Future

class HowItWorksControllerTest extends ControllerBaseTest {

  def howItWorksController: HowItWorksController = instanceOf[HowItWorksController]

  "HowItWorksController" should {
    "direct the user to How It Works page" in {
      val result = howItWorksController.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Apply for Marriage Allowance - Marriage Allowance - GOV.UK"

      val heading = document.getElementById("pageHeading").text
      heading shouldBe "Apply for Marriage Allowance"

      val button = document.getElementById("get-started")
      button.text shouldBe "Apply now"
    }

    //TODO: - to be removed when URLs deprecated completely
    "Redirect deprecated eligibility end points to /how-it-works" in {
      val eligibilityRedirects: List[Future[Result]] = List(
        howItWorksController.eligibilityCheck()(request),
        howItWorksController.lowerEarner()(request),
        howItWorksController.partnersIncome()(request),
        howItWorksController.dateOfBirthCheck()(request),
        howItWorksController.doYouLiveInScotland()(request),
        howItWorksController.doYouWantToApply()(request)
      )
      eligibilityRedirects.foreach { redirectAction =>
        val result: Future[Result] = redirectAction
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.HowItWorksController.howItWorks.url)
      }
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
