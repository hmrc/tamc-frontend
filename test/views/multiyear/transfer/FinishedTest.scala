/*
 * Copyright 2025 HM Revenue & Customs
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

package views.multiyear.transfer

import controllers.actions.{AuthRetrievals, UnauthenticatedActionTransformer}
import controllers.auth.PertaxAuthAction
import controllers.transfer.FinishedController
import helpers.FakePertaxAuthAction
import models.*
import models.auth.AuthenticatedUserRequest
import org.apache.pekko.util.Timeout
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.contentAsString
import play.api.test.{FakeRequest, Helpers}
import services.TransferService
import uk.gov.hmrc.domain.Nino
import utils.*

import scala.concurrent.Future
import scala.concurrent.duration.*
import scala.language.postfixOps


class FinishedTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  implicit val request: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  val finishedController: FinishedController = app.injector.instanceOf[FinishedController]


  "Calling non-pta finished page" should {

    "successfully authenticate the user and have finished page and content" in {
      when(mockTransferService.getRecipientDetailsFormData())
        .thenReturn(Future.successful(RecipientDetailsFormInput("Alex", "Smith", Gender("M"), Nino(nino))))

      when(mockTransferService.getFinishedData(any())(any(), any()))
        .thenReturn(Future.successful(NotificationRecord(EmailAddress("example@example.com"))))
      val result = finishedController.finished(FakeRequest())


      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      verify(mockTransferService).getRecipientDetailsFormData()
      document.title() shouldBe "Application confirmed - Marriage Allowance application - GOV.UK"
      document.getElementById("govuk-box").text shouldBe "Marriage Allowance application complete"
      document
        .getElementById("paragraph-1")
        .text shouldBe "A confirmation email will be sent to " +
        "example@example.com from noreply@tax.service.gov.uk within 24 hours."
    }
  }
}
