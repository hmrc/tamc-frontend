/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.transfer

import controllers.ControllerBaseSpec
import controllers.actions.AuthenticatedActionRefiner
import models.{Gender, RecipientDetailsFormInput}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import services.{CachingService, TimeService}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time



class PartnersDetailsControllerTest  extends ControllerBaseSpec {

  "onPageLoad" should {
    "return success" in {
      val result = controller().onPageLoad()(request)
      status(result) shouldBe OK
    }
  }

  "onSubmit" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val recipientDetails: RecipientDetailsFormInput = RecipientDetailsFormInput("Test", "User", Gender("M"), Nino(Ninos.nino2))
        when(mockCachingService.saveRecipientDetails(ArgumentMatchers.eq(recipientDetails))(any(), any()))
          .thenReturn(recipientDetails)
        val result = controller().onSubmit()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect the user" when {
      "a valid form is submitted" in {
        val recipientDetails: RecipientDetailsFormInput = RecipientDetailsFormInput("Test", "User", Gender("M"), Nino(Ninos.nino2))
        val request = FakeRequest().withFormUrlEncodedBody(
          "name" -> "Test",
          "last-name" -> "User",
          "gender" -> "M",
          "nino" -> Ninos.nino2
        )
        when(mockCachingService.saveRecipientDetails(ArgumentMatchers.eq(recipientDetails))(any(), any()))
          .thenReturn(recipientDetails)
        val result = controller().onSubmit()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.transfer.routes.DateOfMarriageController.onPageLoad().url)
      }
    }
  }

  val currentTaxYear: Int = time.TaxYear.current.startYear

  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  def controller(authAction: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner]) = new PartnersDetailsController(
    messagesApi,
    authAction,
    mockCachingService,
    mockTimeService
  )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  when(mockTimeService.getCurrentDate) thenReturn LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) thenReturn currentTaxYear
}
