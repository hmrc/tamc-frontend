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

package views.multiyear.transfer

import controllers.actions.{AuthRetrievals, UnauthenticatedActionTransformer}
import controllers.auth.PertaxAuthAction
import controllers.transfer.FinishedController
import helpers.FakePertaxAuthAction
import models._
import models.auth.AuthenticatedUserRequest
import org.apache.pekko.util.Timeout
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import services.TransferService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import utils.{BaseTest, MockAuthenticatedAction, MockUnauthenticatedAction, NinoGenerator}

import scala.concurrent.duration._
import scala.language.postfixOps


class FinishedTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  val finishedController: FinishedController = app.injector.instanceOf[FinishedController]

  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    )
    .build()

  "Calling non-pta finished page" should {

    "successfully authenticate the user and have finished page and content" in {
      when(mockTransferService.getFinishedData(any())(any(), any()))
        .thenReturn(NotificationRecord(EmailAddress("example@example.com")))
      val result = finishedController.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Application confirmed - Marriage Allowance application - GOV.UK"
      document.getElementById("govuk-box").text shouldBe "Marriage Allowance application successful"
      document
        .getElementById("paragraph-1")
        .text shouldBe "An email with full details acknowledging your application will be " +
        "sent to you at example@example.com from noreply@tax.service.gov.uk within 24 hours."
    }
  }
}
