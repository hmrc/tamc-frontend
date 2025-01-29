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
import controllers.transfer.ConfirmController
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
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, EmailAddress, MockAuthenticatedAction, MockUnauthenticatedAction, NinoGenerator}

import java.time.LocalDate
import scala.concurrent.duration._
import scala.language.postfixOps


class ConfirmTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  val confirmController: ConfirmController = app.injector.instanceOf[ConfirmController]

  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    )
    .build()

  "Display Confirm page " should {
    "have marriage date and name displayed" in {
      val confirmData = ConfirmationModel(
        Some(CitizenName(Some("JIM"), Some("FERGUSON"))),
        EmailAddress("example@example.com"),
        "foo",
        "bar",
        Nino(Ninos.ninoWithLOA1),
        List(TaxYear(2014, Some(false)), TaxYear(2015, Some(false))),
        DateOfMarriageFormInput(LocalDate.of(2015, 1, 1))
      )
      when(mockTransferService.getConfirmationData(any())(any(), any(), any()))
        .thenReturn(confirmData)

      val result = confirmController.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val applicantName = document.getElementById("transferor-name")
      val recipientName = document.getElementById("recipient-name")
      val marriageDate = document.getElementById("marriage-date")
      applicantName.ownText() shouldBe "Jim Ferguson"
      recipientName.ownText() shouldBe "foo bar"
      marriageDate.ownText() shouldBe "1 January 2015"
    }
  }
}
