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
import controllers.transfer.{EligibleYearsController, ExtraYearsController}
import helpers.FakePertaxAuthAction
import models._
import models.auth.AuthenticatedUserRequest
import org.apache.pekko.util.Timeout
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.http.Status.BAD_REQUEST
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import services.TransferService
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, MockAuthenticatedAction, MockUnauthenticatedAction, NinoGenerator}

import java.time.LocalDate
import scala.concurrent.duration._
import scala.language.postfixOps


class PreviousYearsTest extends BaseTest with NinoGenerator {

  lazy val nino: String = generateNino().nino
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  val eligibleYearsController: EligibleYearsController = app.injector.instanceOf[EligibleYearsController]
  val extraYearsController: ExtraYearsController = app.injector.instanceOf[ExtraYearsController]

  implicit val duration: Timeout = 20 seconds

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction],
    )
    .build()

  "Calling Previous year page " should {
    val rcrec = UserRecord(cid = 123456, timestamp = "2015")
    val rcdata = RegistrationFormInput(
      name = "foo",
      lastName = "bar",
      gender = Gender("M"),
      nino = Nino(Ninos.ninoWithLOA1),
      dateOfMarriage = LocalDate.of(2011, 4, 10)
    )
    val recrecord = RecipientRecord(
      record = rcrec,
      data = rcdata,
      availableTaxYears = List(TaxYear(2014), TaxYear(2015), TaxYear(2016))
    )

    "display form error message (no year choice made )" in {
      when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
        .thenReturn(
          CurrentAndPreviousYearsEligibility(
            currentYearAvailable = true,
            recrecord.availableTaxYears,
            recrecord.data,
            recrecord.availableTaxYears
          )
        )
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "year" -> "List(0)")
      val result = extraYearsController.extraYearsAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "Confirm the earlier years you want to apply for"
      document.getElementById("eligible-years-form").toString should include("/marriage-allowance-application/extra-years")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("selectedYear-error").text() shouldBe "Error: Select yes if you would like to apply for earlier tax years"
    }
  }

}
