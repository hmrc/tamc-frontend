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

package controllers

import config.ApplicationConfig
import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import models.{EligibilityCalculatorResult, England}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.EligibilityCalculatorService
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.{ControllerBaseTest, MockPermUnauthenticatedAction}

class EligibilityControllerTest extends ControllerBaseTest {

  val mockEligibilityCalculatorService: EligibilityCalculatorService = mock[EligibilityCalculatorService]

  def controller(unAuthAction: UnauthenticatedActionTransformer = instanceOf[UnauthenticatedActionTransformer]): EligibilityController =
    new EligibilityController(
      messagesApi,
      unAuthAction,
      instanceOf[AuthenticatedActionRefiner],
      mockEligibilityCalculatorService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  "howItWorks" should {
    "return success" in {
      val result = controller().howItWorks()(request)
      status(result) shouldBe OK
    }
  }

  "home" should {
    "redirect the user" in {
      val result = controller().home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.eligibilityCheck().url)
    }
  }

  "eligibilityCheck" should {
    "return success" in {
      val result = controller().eligibilityCheck()(request)
      status(result) shouldBe OK
    }
  }

  "eligibilityCheckAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "not a boolean"
        )
        val result = controller().eligibilityCheckAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a success" when {
      "user is not married with permanent auth state" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "false"
        )
        val result = controller(instanceOf[MockPermUnauthenticatedAction]).eligibilityCheckAction()(request)
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("button-finished").attr("href") shouldBe ApplicationConfig.ptaFinishedUrl
      }

      "user is not married with temporary auth state" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "false"
        )
        val result = controller().eligibilityCheckAction()(request)
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("button-finished").attr("href") shouldBe ApplicationConfig.gdsFinishedUrl
      }
    }

    "redirect the user" when {
      "user is married" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "marriage-criteria" -> "true"
        )
        val result = controller().eligibilityCheckAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.dateOfBirthCheck().url)
      }
    }
  }

  "dateOfBirthCheck" should {
    "return success" in {
      val result = controller().dateOfBirthCheck()(request)
      status(result) shouldBe OK
    }
  }

  "dateOfBirthCheckAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "date-of-birth" -> "not bool")
        val result = controller().dateOfBirthCheckAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to do you live in scotland" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "date-of-birth" -> "true"
        )
        val result = controller().dateOfBirthCheckAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.doYouLiveInScotland().url)
      }
    }
  }

  "doYouLiveInScotland" should {
    "return success" in {
      val result = controller().doYouLiveInScotland()(request)
      status(result) shouldBe OK
    }
  }

  "doYouLiveInScotlandAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "do-you-live-in-scotland" -> "not bool")
        val result = controller().doYouLiveInScotlandAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to lower earner" when {
      "a valid form is submitted who lives in scotland" in {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody(
          "do-you-live-in-scotland" -> "true")
        val result = controller().doYouLiveInScotlandAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.lowerEarnerCheck().url)
        result.session.data("scottish_resident") shouldBe "true"
      }

      "a valid form is submitted who does not live in scotland" in {
        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest().withFormUrlEncodedBody(
          "do-you-live-in-scotland" -> "false")
        val result = controller().doYouLiveInScotlandAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.lowerEarnerCheck().url)
        result.session.data("scottish_resident") shouldBe "false"
      }
    }
  }

  "lowerEarnerCheck" should {
    "return success" in {
      val result = controller().lowerEarnerCheck()(request)
      status(result) shouldBe OK
    }
  }

  "lowerEarnerCheckAction" should {
    "return a bad request" when {
      "a invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "lower-earner" -> "not a bool"
        )
        val result = controller().lowerEarnerCheckAction()(request)
        status(result) shouldBe BAD_REQUEST
      }

      "redirect to partners income" when {
        "a valid form is submitted" in {
          val request = FakeRequest().withFormUrlEncodedBody(
            "lower-earner" -> "true"
          )
          val result = controller().lowerEarnerCheckAction()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.partnersIncomeCheck().url)
        }
      }
    }
  }

  "partnersIncomeCheck" should {
    "return success " in {
      val result = controller().partnersIncomeCheck()(request)
      status(result) shouldBe OK
    }
  }

  "partnersIncomeCheckAction" should {
    "bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "partners-income" -> "not bool")
        val result = controller().partnersIncomeCheckAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect to want to apply" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "partners-income" -> "true",
          "is-scottish" -> "true")
        val result = controller().partnersIncomeCheckAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.doYouWantToApply().url)
      }
    }
  }

  "doYouWantToApply" should {
    "return success" in {
      val result = controller().doYouWantToApply()(request)
      status(result) shouldBe OK
    }
  }

  "doYouWantToApplyAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "do-you-want-to-apply" -> "not a bool")
        val result = controller().doYouWantToApplyAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a redirect to transfer" when {
      "the user wants to apply" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "do-you-want-to-apply" -> "true")
        val result = controller().doYouWantToApplyAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.transfer().url)
      }
    }

    "return a redirect" when {
      "the user doesnt want to apply and is permanently logged in" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "do-you-want-to-apply" -> "false")
        val result = controller(instanceOf[MockPermUnauthenticatedAction]).doYouWantToApplyAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(ApplicationConfig.ptaFinishedUrl)
      }

      "the user doesnt want to apply and is not permanently logged in" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "do-you-want-to-apply" -> "false")
        val result = controller().doYouWantToApplyAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(ApplicationConfig.gdsFinishedUrl)
      }
    }
  }

  "gdsCalculator" should {
    "return success" in {
      val result = controller().gdsCalculator()(request)
      status(result) shouldBe OK
    }
  }

  "gdsCalculatorAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "transferor-income" -> "not some income",
          "recipient-income" -> "not some income")
        val result = controller().gdsCalculatorAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a success" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "country" -> "england",
          "transferor-income" -> "£20.56",
          "recipient-income" -> "£100"
        )
        when(mockEligibilityCalculatorService.calculate(ArgumentMatchers.eq(20), ArgumentMatchers.eq(100),
          ArgumentMatchers.eq(England))
        ).thenReturn(EligibilityCalculatorResult("test_key"))
        val result = controller().gdsCalculatorAction()(request)
        status(result) shouldBe OK
      }
    }
  }

  "ptaCalculator" should {
    "return success" in {
      val result = controller().ptaCalculator()(request)
      status(result) shouldBe OK
    }
  }

  "ptaCalculatorAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "transferor-income" -> "not some income",
          "recipient-income" -> "not some income")
        val result = controller().ptaCalculatorAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a success" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "country" -> "england",
          "transferor-income" -> "£20.56",
          "recipient-income" -> "£100"
        )
        when(mockEligibilityCalculatorService.calculate(ArgumentMatchers.eq(20), ArgumentMatchers.eq(100),
          ArgumentMatchers.eq(England))
        ).thenReturn(EligibilityCalculatorResult("test_key"))
        val result = controller().ptaCalculatorAction()(request)
        status(result) shouldBe OK
      }
    }
  }
}
