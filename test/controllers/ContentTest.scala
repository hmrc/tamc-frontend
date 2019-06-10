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

package controllers

import java.text.NumberFormat

import _root_.services.{CachingService, TimeService, TransferService}
import config.ApplicationConfig._
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, contentAsString, defaultAwaitTimeout}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time

class ContentTest extends ControllerBaseSpec {

  private val lowerEarnerHelpText =
    "This is your total earnings from all employment, pensions, benefits, trusts, " +
      "rental income, including dividend income above your Dividend Allowance – before any tax and National " +
      "Insurance is taken off."

  val ERROR_HEADING = "There is a problem"

  "Calling Transfer Submit page" should {

    "display form error message (first name and last name missing from request)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "gender" -> "M",
        "nino" -> Ninos.nino1,
        "transferor-email" -> "example@example.com"
      )
      val result = transferController.transferAction()(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (request body missing form data)" in {
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("nino-error").text() shouldBe "Confirm your partner’s National Insurance number"
    }
  }

  "Calling Transfer Submit page with error in name field" should {
    "display form error message (first name missing from request)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s first name"
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (first name is empty)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s first name"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (first name is blank)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> " ",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s first name"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (first name contains more than 35 characters)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "a" * 36,
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use up to or no more than 35 letters"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (first name contains numbers)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "12345",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (first name contains letters and numbers)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "abc123",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only"
      document.getElementById("name-error").text() shouldBe "Confirm your partner’s first name"
    }

    "display form error message (last name missing from request)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s last name"
      document.getElementById("last-name-error").text() shouldBe "Confirm your partner’s last name"
    }

    "display form error message (last name is empty)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s last name"
      document.getElementById("last-name-error").text() shouldBe "Confirm your partner’s last name"
    }

    "display form error message (last name is blank)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> " ",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s last name"
      document.getElementById("last-name-error").text() shouldBe "Confirm your partner’s last name"
    }

    "display form error message (last name contains more than 35 characters)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "a" * 36,
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use up to or no more than 35 letters"
      document.getElementById("last-name-error").text() shouldBe "Confirm your partner’s last name"
    }

    "display form error message (last name contains numbers)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "12345",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only"
      document.getElementById("last-name-error").text() shouldBe "Confirm your partner’s last name"
    }

    "display form error message (last name contains letters and numbers)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "abc123",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only"
      document.getElementById("last-name-error").text() shouldBe "Confirm your partner’s last name"
    }

    "display form error message when recipient nino equals transferor nino" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "abc",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelNino = form.select("label[for=nino]").first()
      labelNino.getElementsByClass("error-message").first() shouldNot be(null)
      labelNino.getElementsByClass("error-message").first().text() shouldBe "You cannot enter your own details"
      document.getElementById("nino-error").text() shouldBe "Confirm your partner’s National Insurance number"
    }
  }

  "Calling Transfer Submit page with error in gender field" should {
    "display form error message (gender missing from request)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("fieldset[id=gender]").first()
      labelName.getElementsByClass("error-notification").first() shouldNot be(null)
      labelName.getElementsByClass("error-notification").first().text() shouldBe "Tell us your partner’s gender"
      document.getElementById("gender-error").text() shouldBe "Confirm your partner’s gender"
    }

    "display form error message (gender code is invalid)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "X",
        "nino" -> Ninos.nino1
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("fieldset[id=gender]").first()
      labelName.getElementsByClass("error-notification").first() shouldNot be(null)
      labelName.getElementsByClass("error-notification").first().text() shouldBe "Tell us your partner’s gender"
      document.getElementById("gender-error").text() shouldBe "Confirm your partner’s gender"
    }
  }

  "Calling Transfer Submit page with error in NINO field" should {
    "display form error message (NINO missing from request)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M"
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=nino]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s National Insurance number"
      document.getElementById("nino-error").text() shouldBe "Confirm your partner’s National Insurance number"
    }

    "display form error message (NINO is empty)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> ""
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=nino]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your partner’s National Insurance number"
      document.getElementById("nino-error").text() shouldBe "Confirm your partner’s National Insurance number"
    }

    "display form error message (NINO is invalid)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "name" -> "foo",
        "last-name" -> "bar",
        "gender" -> "M",
        "nino" -> "ZZ"
      )
      val result = transferController.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=nino]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Check their National Insurance number and enter it correctly"
      document.getElementById("nino-error").text() shouldBe "Confirm your partner’s National Insurance number"
    }
  }

  "Calling Date Of Marriage page with error in dom field" should {

    "display form error message (date of marriage is before 1900)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "1",
        "dateOfMarriage.month" -> "1",
        "dateOfMarriage.year" -> "1899"
      )
      val result = transferController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("date-of-marriage-form")
      form shouldNot be(null)

      val field = form.getElementById("dateOfMarriage")
      field shouldNot be(null)

      val err = field.getElementsByClass("client-error-notification")
      err.size() shouldBe 1

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.TransferController.transfer().url
    }

    "display form error message (date of marriage is after today’s date)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "1",
        "dateOfMarriage.month" -> "1",
        "dateOfMarriage.year" -> "2020"
      )
      val result = transferController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("date-of-marriage-form")
      form shouldNot be(null)

      val field = form.getElementById("dateOfMarriage")
      field shouldNot be(null)

      val err = field.getElementsByClass("client-error-notification")
      err.size() shouldBe 1
    }

    "display form error message (date of marriage is left empty)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfMarriage.day" -> "",
        "dateOfMarriage.month" -> "",
        "dateOfMarriage.year" -> ""
      )
      val result = transferController.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("date-of-marriage-form")
      form shouldNot be(null)

      val field = form.getElementById("dateOfMarriage")
      field shouldNot be(null)

      val err = field.getElementsByClass("client-error-notification")
      val labelName = form.select("fieldset[id=dateOfMarriage]").first()
      err.size() shouldBe 1
      labelName.getElementsByClass("error-notification").first().text() shouldBe "Tell us your date of marriage"
      document.getElementById("dateOfMarriage-error").text() shouldBe "Confirm your date of marriage"
    }
  }

  "Calling Previous year page " should {
    val rcrec = UserRecord(cid = 123456, timestamp = "2015")
    val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2011, 4, 10))
    val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2014), TaxYear(2015), TaxYear(2016)))
    "display dynamic message " in {
      when(mockTransferService.getCurrentAndExtraYearEligibility(any(), any())).thenReturn((true, recrecord.availableTaxYears, recrecord))
      when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(recrecord), ArgumentMatchers.eq(List(time.TaxYear.current.startYear)))(any(), any()))
        .thenReturn(Nil)
      val request = FakeRequest().withFormUrlEncodedBody(data = "applyForCurrentYear" -> "true")
      val result = transferController.eligibleYearsAction(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("firstNameOnly").text() shouldBe "foo"
      document.getElementById("marriageDate").text() shouldBe "10 April 2011"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.TransferController.eligibleYears().url
    }

    "display form error message (no year choice made )" in {
      when(mockTransferService.getCurrentAndExtraYearEligibility(any(), any())).thenReturn((true, recrecord.availableTaxYears, recrecord))
      val request = FakeRequest().withFormUrlEncodedBody(data = "year" -> "List(0)")
      val result = transferController.extraYearsAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "Confirm the earlier years you want to apply for"
      val form = document.getElementById("eligible-years-form")
      form shouldNot be(null)
      val labelName = form.select("fieldset[id=selectedYear]").first()
      labelName.getElementsByClass("error-notification").first() shouldNot be(null)
      labelName.getElementsByClass("error-notification").first().text() shouldBe "Select an answer"
      document.getElementById("selectedYear-error").text() shouldBe "You need to select an answer"
    }
  }

  "Calling Confirm email page with error in email field" should {
    "display form error message (transferor email missing from request)" in {
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.TransferController.eligibleYears().url
    }

    "display form error message (transferor email is empty)" in {
      val request = FakeRequest().withFormUrlEncodedBody(data = "transferor-email" -> "")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email contains only spaces)" in {
      val request = FakeRequest().withFormUrlEncodedBody(data = "transferor-email" -> "  ")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email contains more than 100 characters)" in {
      val request = FakeRequest().withFormUrlEncodedBody("transferor-email" -> s"${"a" * 90}@bbbb.ccccc")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Enter no more than 100 characters"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email is invalid)" in {
      val request = FakeRequest().withFormUrlEncodedBody(data = "transferor-email" -> "example")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email has consequent dots)" in {
      val request = FakeRequest().withFormUrlEncodedBody("transferor-email" -> "ex..ample@example.com")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email has symbols). Please note, this email actually is valid" in {
      val request = FakeRequest().withFormUrlEncodedBody(data = "transferor-email" -> "check$%^&&@example.com")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email does not include TLD)" in {
      val request = FakeRequest().withFormUrlEncodedBody(data = "transferor-email" -> "example@example")
      val result = transferController.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address"
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
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
      document.getElementsByClass("heading-large").text shouldBe "Marriage Allowance application successful"
      document.getElementById("paragraph-1").text shouldBe "An email with full details acknowledging your application will be " +
        "sent to you at example@example.com from noreply@tax.service.gov.uk within 24 hours."
    }
  }

  "PTA Benefit calculator page " should {

    "successfully load the calculator page " in {
      val result = eligibilityController.ptaCalculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Eligibility Criteria - Marriage Allowance - GOV.UK"

      val heading = document.getElementsByClass("heading-xlarge").text
      heading shouldBe "Marriage Allowance calculator"
    }
  }

  "PTA How It Works page for multi year " should {
    "successfully loaded " in {
      val result = eligibilityController.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Apply for Marriage Allowance - Marriage Allowance - GOV.UK"

      val heading = document.getElementsByClass("heading-xlarge").text
      heading shouldBe "Apply for Marriage Allowance"

      val button = document.getElementById("get-started")
      button.text shouldBe "Start now to see if you are eligible for Marriage Allowance"
    }

  }

  "PTA Eligibility check page for multiyear" should {

    "successfully authenticate the user and have eligibility-check page action" in {
      val result = eligibilityController.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      val elements = document.getElementById("eligibility-form").getElementsByTag("p")
      elements shouldNot be(null)
    }

    "diplay errors as none of the radio buttons are selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Select yes if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Select yes if you are married or in a legally registered civil partnership"

    }
  }

  "GDS Eligibility check page for multiyear" should {

    "successfully authenticate the user and have eligibility-check page action" in {
      val result = eligibilityController.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      val elements = document.getElementById("eligibility-form").getElementsByTag("p")
      elements shouldNot be(null)
    }

    "diplay errors as none of the radio buttons are selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Select yes if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Select yes if you are married or in a legally registered civil partnership"

    }
  }

  "PTA date of birth check page for multiyear" should {

    "successfully authenticate the user and have date of birth page and content" in {
      val result = eligibilityController.dateOfBirthCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "PTA lower earner check page for multiyear" should {

    "successfully authenticate the user and have income-check page and content" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE())
      val result = eligibilityController.lowerEarnerCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your income less than £$lowerThreshold a year? - Marriage Allowance eligibility - GOV.UK"

      document.getElementsByClass("bold-small").text shouldBe lowerEarnerHelpText
    }
  }

  "PTA partners income check page for multiyear" should {

    "have partners-income page and content for English resident" in {
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = NumberFormat.getIntegerInstance().format(MAX_LIMIT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByTag("p").text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document.getElementsByClass("heading-xlarge").text shouldBe s"Check your eligibility Is your partner’s income between £$lowerThreshold and £$higherThreshold a year?"

    }

    "have partners-income page and content for Scottish resident" in {
      val request = FakeRequest().withSession("scottish_resident" -> "true")
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(PERSONAL_ALLOWANCE() + 1)
      val higherThresholdScot = NumberFormat.getIntegerInstance().format(MAX_LIMIT_SCOT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByTag("p").text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document.getElementsByClass("heading-xlarge").text shouldBe s"Check your eligibility Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year?"

    }
  }

  "PTA do you want to apply page for multiyear" should {
    "successfully authenticate the user and have do-you-want-to-apply page and content" in {
      val result = eligibilityController.doYouWantToApply()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you want to apply for Marriage Allowance? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS date of birth page for multiyear" should {

    "successfully authenticate the user and have date of birth page and content" in {
      val result = eligibilityController.dateOfBirthCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS do you live in scotland page for multiyear" should {

    "successfully authenticate the user and have do you live in scotland page and content" in {
      val result = eligibilityController.doYouLiveInScotland()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you live in Scotland? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS do you want to apply page for multiyear" should {

    "successfully authenticate the user and have do you want to apply page and content" in {
      val result = eligibilityController.doYouWantToApply()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you want to apply for Marriage Allowance? - Marriage Allowance eligibility - GOV.UK"
    }
  }

  "GDS lower earner page for multiyear" should {

    "successfully authenticate the user and have lower earner page and content" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE())
      val result = eligibilityController.lowerEarnerCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your income less than £$lowerThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByClass("bold-small").text shouldBe lowerEarnerHelpText
    }
  }

  "GDS partners income page for multiyear" should {
    "have partners-income page and content for English resident" in {
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = NumberFormat.getIntegerInstance().format(MAX_LIMIT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByTag("p").text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document.getElementsByClass("heading-xlarge").text shouldBe s"Check your eligibility Is your partner’s income between £$lowerThreshold and £$higherThreshold a year?"

    }

    "have partners-income page and content for Scottish resident" in {
      val request = FakeRequest().withSession("scottish_resident" -> "true")
      val result = eligibilityController.partnersIncomeCheck()(request)

      val lowerThreshold = NumberFormat.getIntegerInstance().format(PERSONAL_ALLOWANCE() + 1)
      val higherThresholdScot = NumberFormat.getIntegerInstance().format(MAX_LIMIT_SCOT())

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementsByTag("p").text shouldBe "This is their total earnings from all employment, pensions, benefits, trusts, rental income, including dividend income above their Dividend Allowance – before any tax and National Insurance is taken off."
      document.getElementsByClass("heading-xlarge").text shouldBe s"Check your eligibility Is your partner’s income between £$lowerThreshold and £$higherThresholdScot a year?"

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
        DateOfMarriageFormInput(new LocalDate(2015, 1, 1))
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

  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  def eligibilityController: EligibilityController = instanceOf[EligibilityController]

  def transferController: TransferController = new TransferController(
    messagesApi,
    instanceOf[AuthenticatedActionRefiner],
    mockTransferService,
    mockCachingService,
    mockTimeService
  )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  val currentTaxYear: Int = time.TaxYear.current.startYear
  when(mockTimeService.getCurrentDate).thenReturn(LocalDate.now())
  when(mockTimeService.getStartDateForTaxYear(ArgumentMatchers.eq(currentTaxYear))).thenReturn(time.TaxYear.current.starts)
  when(mockTimeService.getCurrentTaxYear).thenReturn(currentTaxYear)
}
