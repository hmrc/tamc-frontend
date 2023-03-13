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

package views.multiyear.transfer

import akka.util.Timeout
import controllers.TransferController
import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import forms.RecipientDetailsForm
import models.{CitizenName, ConfirmationModel, CurrentAndPreviousYearsEligibility, DateOfMarriageFormInput, Gender, NotificationRecord, RecipientRecord, RegistrationFormInput, TaxYear, UserRecord}
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.http.Status.{OK, BAD_REQUEST}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import services.TransferService
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.time
import utils.{BaseTest, MockAuthenticatedAction, MockUnauthenticatedAction, NinoGenerator}
import views.html.multiyear.transfer.transfer

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.duration._
import scala.language.postfixOps


class TransferTest extends BaseTest with NinoGenerator {

  lazy val nino = generateNino().nino
  lazy val transferView = instanceOf[transfer]
  lazy val transferForm = instanceOf[RecipientDetailsForm]
  implicit val request = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))
  val mockTransferService: TransferService = mock[TransferService]
  def transferController: TransferController = app.injector.instanceOf[TransferController]

  implicit val duration: Timeout = 20 seconds


  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction],
      bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction]
    )
    .build()


  "Transfer page" should {
    "display the correct page title of transfer page" in {

      val document = Jsoup.parse(transferView(transferForm.recipientDetailsForm(LocalDate.now, Nino(nino))).toString())
      val title = document.title()
      val expected = messages("title.application.pattern", messages("pages.form.h1"))

      title shouldBe expected
    }

    "display lower income content" in {

      val document = Jsoup.parse(transferView(transferForm.recipientDetailsForm(LocalDate.now, Nino(nino))).toString())
      val paragraphTag = document.getElementsByTag("p").toString
      val expected = messages("pages.form.details")

      paragraphTag should include(expected)

    }
  }

  "Calling Transfer Submit page" should {

    "display form error message (first name and last name missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "gender" -> "M",
        "nino" -> Ninos.nino1,
        "transferor-email" -> "example@example.com"
      )

      val result = transferController.transferAction()(request)
      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (request body missing form data)" in {
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text() shouldBe "Error: Enter your partner’s National Insurance number"
    }
  }

  "Calling Transfer Submit page with error in name field" should {
    "display form error message (first name missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (first name is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (first name is blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> " ",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Enter your partner’s first name"
    }

    "display form error message (first name contains more than 35 characters)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "a" * 36,
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Your partner’s first name must be 35 characters or less"
    }

    "display form error message (first name contains numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "12345",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Your partner’s first name must only include letters a to z and hyphens"
    }

    "display form error message (first name contains letters and numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "abc123",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Error: Your partner’s first name must only include letters a to z and hyphens"
    }

    "display form error message (last name missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Enter your partner’s last name"
    }

    "display form error message (last name is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Enter your partner’s last name"
    }

    "display form error message (last name is blank)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> " ",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Enter your partner’s last name"
    }

    "display form error message (last name contains more than 35 characters)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "a" * 36,
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Your partner’s last name must be 35 characters or less"
    }

    "display form error message (last name contains numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "12345",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Your partner’s last name must only include letters a to z and hyphens"
    }

    "display form error message (last name contains letters and numbers)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "abc123",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("last-name-error").text() shouldBe "Error: Your partner’s last name must only include letters a to z and hyphens"

    }

    "display form error message when recipient nino equals transferor nino" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "abc",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text() shouldBe "Error: You cannot enter your own details"
    }
  }

  "Calling Transfer Submit page with error in gender field" should {
    "display form error message (gender missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("gender-error").text() shouldBe "Error: Select your partner’s gender"
    }

    "display form error message (gender code is invalid)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "X",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("gender-error").text() shouldBe "Error: Select your partner’s gender"
    }
  }

  "Calling Transfer Submit page with error in NINO field" should {
    "display form error message (NINO missing from request)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M"
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text shouldBe "Error: Enter your partner’s National Insurance number"
    }

    "display form error message (NINO is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> ""
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text shouldBe "Error: Enter your partner’s National Insurance number"
    }

    "display form error message (NINO is invalid)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> "ZZ"
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/transfer-allowance")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("nino-error").text shouldBe "Error: Enter a real National Insurance number"
    }
  }

  "Calling Date Of Marriage page with error in dom field" should {

    "display form error message (date of marriage is before 1900)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "1",
        "dateOfMarriage.month" -> "1",
        "dateOfMarriage.year" -> "1899"
      )
      val result = transferController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/date-of-marriage")

      val form = document.getElementById("date-of-marriage-form")
      form.toString should include("/marriage-allowance-application/date-of-marriage")

      val field = form.getElementById("dateOfMarriageFieldset")
      field.text should include("When did you marry or form a civil partnership with your partner?")

      val error = field.getElementsByClass("govuk-error-message")
      error.size() shouldBe 1
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("dateOfMarriage-error").text shouldBe "Error: The date of your marriage or civil partnership must be after 1 01 1900"
      document.getElementById("backLink").attr("href") shouldBe controllers.routes.TransferController.transfer.url
    }

    "display form error message (date of marriage is after today’s date)" in {
      val todayPlusOneDay = LocalDate.now().plusDays(1)
      val pattern = "d MM YYYY"
      val todayPlusOneDayFormatted = todayPlusOneDay.format(DateTimeFormatter.ofPattern(pattern))
      val localDate = LocalDate.now().plusYears(1)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.day" -> s"${localDate.getDayOfMonth}",
        "dateOfMarriage.month" -> s"${localDate.getMonthValue}",
        "dateOfMarriage.year" -> s"${localDate.getYear}"
      )
      val result = transferController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/date-of-marriage")

      val form = document.getElementById("date-of-marriage-form")
      form.toString should include("/marriage-allowance-application/date-of-marriage")
      form.getElementById("dateOfMarriageFieldset").text should include("When did you marry or form a civil partnership with your partner?")

      val error = form.getElementsByClass("govuk-error-message")
      error.size() shouldBe 1
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("dateOfMarriage-error").text shouldBe s"Error: The date of your marriage or civil partnership must be before $todayPlusOneDayFormatted"
    }

    "display form error message (date of marriage is left empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "",
        "dateOfMarriage.month" -> "",
        "dateOfMarriage.year" -> ""
      )
      val result = transferController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementsByAttribute("action").toString should include("/marriage-allowance-application/date-of-marriage")

      val form = document.getElementById("date-of-marriage-form")
      form.toString should include("/marriage-allowance-application/date-of-marriage")

      val field = form.getElementById("dateOfMarriageFieldset")
      field.text should include("When did you marry or form a civil partnership with your partner?")

      val error = field.getElementsByClass("govuk-error-message")
      val labelName = form.select("fieldset[id=dateOfMarriageFieldset]").first()
      error.size() shouldBe 1
      labelName
        .getElementsByClass("govuk-error-message")
        .first()
        .text() shouldBe "Error: Enter the date of your marriage or civil partnership"
      document
        .getElementById("dateOfMarriage-error")
        .text() shouldBe "Error: Enter the date of your marriage or civil partnership"
    }
  }

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

    "display dynamic message " in {
      when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
        .thenReturn(
          CurrentAndPreviousYearsEligibility(
            true,
            recrecord.availableTaxYears,
            recrecord.data,
            recrecord.availableTaxYears
          )
        )
      when(
        mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(time.TaxYear.current.startYear)))(any(), any())
      )
        .thenReturn(Nil)
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "applyForCurrentYear" -> "true")
      val result = transferController.eligibleYearsAction(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("firstNameOnly").text() shouldBe "foo"
      document.getElementById("marriageDate").text() shouldBe "10 April 2011"
      document.getElementsByClass("link-back").attr("href") shouldBe controllers.routes.TransferController.eligibleYears.url
    }

    "display form error message (no year choice made )" in {
      when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
        .thenReturn(
          CurrentAndPreviousYearsEligibility(
            true,
            recrecord.availableTaxYears,
            recrecord.data,
            recrecord.availableTaxYears
          )
        )
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "year" -> "List(0)")
      val result = transferController.extraYearsAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "Confirm the earlier years you want to apply for"
      document.getElementById("eligible-years-form").toString should include("/marriage-allowance-application/extra-years")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("selectedYear-error").text() shouldBe "Error: Select yes if you would like to apply for earlier tax years"
    }
  }

  "Calling Confirm email page with error in email field" should {
    "display form error message (transferor email missing from request)" in {
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter your email address"
      document.getElementsByClass("link-back").attr("href") shouldBe controllers.routes.TransferController.eligibleYears.url
    }

    "display form error message (transferor email is empty)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter your email address"
    }

    "display form error message (transferor email contains only spaces)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "  ")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter your email address"
    }

    "display form error message (transferor email contains more than 100 characters)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("transferor-email" -> s"${"a" * 90}@bbbb.ccccc")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter no more than 100 characters"
    }

    "display form error message (transferor email is invalid)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "example")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }

    "display form error message (transferor email has consequent dots)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("transferor-email" -> "ex..ample@example.com")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }

    "display form error message (transferor email has symbols). Please note, this email actually is valid" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "check[example.com")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }

    "display form error message (transferor email does not include TLD)" in {
      val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(data = "transferor-email" -> "example@example")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("register-form").toString should include("/marriage-allowance-application/confirm-your-email")
      document.getElementsByClass("govuk-error-summary__title").text shouldBe "There is a problem"
      document.getElementById("transferor-email-error").text() shouldBe "Error: Enter an email address in the correct format, like name@example.com"
    }
  }

  "Calling non-pta finished page" should {

    "successfully authenticate the user and have finished page and content" in {
      when(mockTransferService.getFinishedData(any())(any(), any()))
        .thenReturn(NotificationRecord(EmailAddress("example@example.com")))
      val result = transferController.finished(request)

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
      when(mockTransferService.getConfirmationData(any())(any(), any()))
        .thenReturn(confirmData)

      val result = transferController.confirm(request)

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
