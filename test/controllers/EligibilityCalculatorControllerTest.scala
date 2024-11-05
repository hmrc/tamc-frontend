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

import config.ApplicationConfig
import controllers.actions.UnauthenticatedActionTransformer
import models.{EligibilityCalculatorResult, England}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Injector, bind}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.Helpers.baseApplicationBuilder.injector
import services.EligibilityCalculatorService
import utils.{ControllerBaseTest, MockPermUnauthenticatedAction, MockUnauthenticatedAction}

class EligibilityCalculatorControllerTest extends ControllerBaseTest {

  val mockEligibilityCalculatorService: EligibilityCalculatorService = mock[EligibilityCalculatorService]
  val applicationConfig: ApplicationConfig = injector().instanceOf[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false
    ).overrides(
    bind[EligibilityCalculatorService].toInstance(mockEligibilityCalculatorService),
    bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
  ).build()


  def permAuthInjector: Injector = GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false
    ).overrides(
    bind[EligibilityCalculatorService].toInstance(mockEligibilityCalculatorService),
    bind[UnauthenticatedActionTransformer].to[MockPermUnauthenticatedAction],
  ).injector()

  val authController: EligibilityCalculatorController = permAuthInjector.instanceOf[EligibilityCalculatorController]

  val controller: EligibilityCalculatorController = app.injector.instanceOf[EligibilityCalculatorController]

  "gdsCalculator" should {
    "return success" in {
      val result = controller.gdsCalculator()(request)
      status(result) shouldBe OK
    }
  }

  "gdsCalculatorAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "transferor-income" -> "not some income",
          "recipient-income" -> "not some income")
        val result = controller.gdsCalculatorAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a success" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "country" -> "england",
          "transferor-income" -> "£350",
          "recipient-income" -> "£1000"
        )

        when(mockEligibilityCalculatorService.calculate(
          ArgumentMatchers.eq(BigDecimal(350)),
          ArgumentMatchers.eq(BigDecimal(1000)),
          ArgumentMatchers.eq(England),
          any()
        )).thenReturn(EligibilityCalculatorResult("test_key"))

        val result = controller.gdsCalculatorAction()(request)
        status(result) shouldBe OK
      }
    }
  }

  "ptaCalculator" should {
    "return success" in {
      val result = controller.ptaCalculator()(request)
      status(result) shouldBe OK
    }
  }

  "ptaCalculatorAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "transferor-income" -> "not some income",
          "recipient-income" -> "not some income")
        val result = controller.ptaCalculatorAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return a success" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "country" -> "england",
          "transferor-income" -> "    £20    ",
          "recipient-income" -> "    £100    "
        )
        when(mockEligibilityCalculatorService.calculate(
          ArgumentMatchers.eq(BigDecimal(20)),
          ArgumentMatchers.eq(BigDecimal(100)),
          ArgumentMatchers.eq(England),
          any()
        )).thenReturn(EligibilityCalculatorResult("test_key"))

        val result = controller.ptaCalculatorAction()(request)
        status(result) shouldBe OK
      }
    }
  }
}
