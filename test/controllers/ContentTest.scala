/*
 * Copyright 2016 HM Revenue & Customs
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
import models._
import org.jsoup.Jsoup
import play.api.test.Helpers.{ BAD_REQUEST, OK, contentAsString, defaultAwaitTimeout, SEE_OTHER }
import play.api.test.WithApplication
import test_utils.{ TestConstants, TestUtility }
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import org.joda.time.{ LocalDate, DateTimeZone, DateTime }
import test_utils.TestData.Cids
import test_utils.TestData.Ninos

class ContentTest extends UnitSpec with TestUtility {

  "Calling Transfer page" should {
    "display transferor name" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.nino1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)
      
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val transferor = document.getElementById("transferor-name")
      transferor.text() shouldBe "(Foo Bar)"
    }
  }

  "Calling Transfer Submit page" should {

    "display transferor name on error page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.nino1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val transferor = document.getElementById("transferor-name")
      transferor.text() shouldBe "(Foo Bar)"
    }

    "display form error message (first name and last name missing from request)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.nino1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("form-error-message").text() shouldBe "Check your information is correct, in the right place and in the right format."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:transfer-erroneous(last-name,name)").size() shouldBe 1
    }

    "display form error message (request body missing form data)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("form-error-message").text() shouldBe "Check your information is correct, in the right place and in the right format."
      document.getElementById("nino-error").text() shouldBe "Confirm your spouse or civil partner's National Insurance number."

      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:transfer-erroneous(gender,last-name,name,nino)").size() shouldBe 1
    }
  }

  "Calling Transfer Submit page with error in name field" should {
    "display form error message (first name missing from request)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's first name."
      document.getElementById("form-error-heading").text() shouldBe "There is a problem"
      document.getElementById("form-error-message").text() shouldBe "Check your information is correct, in the right place and in the right format."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:transfer-erroneous(name)").size() shouldBe 1
    }

    "display form error message (first name is empty)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> ""), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's first name."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
    }

    "display form error message (first name is blank)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> " "), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's first name."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
    }

    "display form error message (first name contains more than 35 characters)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use up to or no more than 35 letters."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
    }

    "display form error message (first name contains numbers)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "12345"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
    }

    "display form error message (first name contains letters and numbers)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "abc123"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only."
      document.getElementById("name-error").text() shouldBe "Confirm your spouse or civil partner's first name"
    }

    "display form error message when recipient nino equals transferor nino" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "abc"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoHappyPath))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelNino = form.select("label[for=nino]").first()
      labelNino.getElementsByClass("error-message").first() shouldNot be(null)
      labelNino.getElementsByClass("error-message").first().text() shouldBe "You can't enter your own details."
      document.getElementById("nino-error").text() shouldBe "Confirm your spouse or civil partner's National Insurance number."
    }
    
    "display form error message when recipient nino equals transferor nino (including mixed case and spaces)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "abc"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoHappyPathWithSpaces))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelNino = form.select("label[for=nino]").first()
      labelNino.getElementsByClass("error-message").first() shouldNot be(null)
      labelNino.getElementsByClass("error-message").first().text() shouldBe "You can't enter your own details."
      document.getElementById("nino-error").text() shouldBe "Confirm your spouse or civil partner's National Insurance number."
    }
  }

  "Calling Transfer Submit page with error in last-name field" should {
    "display form error message (last name missing from request)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's last name."
      document.getElementById("last-name-error").text() shouldBe "Confirm your spouse or civil partner's last name"
      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:transfer-erroneous(last-name)").size() shouldBe 1
    }

    "display form error message (last name is empty)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> ""), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's last name."
      document.getElementById("last-name-error").text() shouldBe "Confirm your spouse or civil partner's last name"
    }

    "display form error message (last name is blank)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> " "), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's last name."
      document.getElementById("last-name-error").text() shouldBe "Confirm your spouse or civil partner's last name"
    }

    "display form error message (last name contains more than 35 characters)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use up to or no more than 35 letters."
      document.getElementById("last-name-error").text() shouldBe "Confirm your spouse or civil partner's last name"
    }

    "display form error message (last name contains numbers)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "12345"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only."
      document.getElementById("last-name-error").text() shouldBe "Confirm your spouse or civil partner's last name"
    }

    "display form error message (last name contains letters and numbers)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "abc123"), ("gender" -> "M"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=last-name]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Use letters only."
      document.getElementById("last-name-error").text() shouldBe "Confirm your spouse or civil partner's last name"
    }
  }

  "Calling Transfer Submit page with error in gender field" should {
    "display form error message (gender missing from request)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("nino" -> Ninos.nino1), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("fieldset[id=gender]").first()
      labelName.getElementsByClass("error-notification").first() shouldNot be(null)
      labelName.getElementsByClass("error-notification").first().text() shouldBe "Tell us your spouse or civil partner's gender."
      document.getElementById("gender-error").text() shouldBe "Confirm your spouse or civil partner's gender"
      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:transfer-erroneous(gender)").size() shouldBe 1
    }

    "display form error message (gender code is invalid)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "X"), ("nino" -> Ninos.nino1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("fieldset[id=gender]").first()
      labelName.getElementsByClass("error-notification").first() shouldNot be(null)
      labelName.getElementsByClass("error-notification").first().text() shouldBe "Tell us your spouse or civil partner's gender."
      document.getElementById("gender-error").text() shouldBe "Confirm your spouse or civil partner's gender"
    }
  }

  "Calling Transfer Submit page with error in NINO field" should {
    "display form error message (NINO missing from request)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=nino]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's National Insurance number."
      document.getElementById("nino-error").text() shouldBe "Confirm your spouse or civil partner's National Insurance number."
      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:transfer-erroneous(nino)").size() shouldBe 1
    }

    "display form error message (NINO is empty)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> ""))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=nino]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your spouse or civil partner's National Insurance number."
      document.getElementById("nino-error").text() shouldBe "Confirm your spouse or civil partner's National Insurance number."
    }

    "display form error message (NINO is invalid)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> "ZZ"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=nino]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Check their National Insurance number and enter it correctly."
      document.getElementById("nino-error").text() shouldBe "Confirm your spouse or civil partner's National Insurance number."
    }
  }

  "Calling Date Of Marriage page with error in dom field" should {

    "display form error message (date of marriage is before 1900)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "1899"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("date-of-marriage-form")
      form shouldNot be(null)

      val field = form.getElementById("dateOfMarriage")
      field shouldNot be(null)

      val err = field.getElementsByClass("client-error-notification")
      err.size() shouldBe 1
    }

    "display form error message (date of marriage is after today's date)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent(
        "user_happy_path",
        transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2017"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("date-of-marriage-form")
      form shouldNot be(null)

      val field = form.getElementById("dateOfMarriage")
      field shouldNot be(null)

      val err = field.getElementsByClass("client-error-notification")
      err.size() shouldBe 1
    }
  }

  "Calling Confirm email page with error in email field" should {
    "display form error message (transferor email missing from request)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your email address."
      document.getElementsByAttributeValue("data-journey", "marriage-allowance:stage:email-erroneous(transferor-email)").size() shouldBe 1
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email is empty)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> ""))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your email address."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email contains only spaces)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "  "))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Tell us your email address."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email contains more than 100 characters)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "aaaaaaaaaabbbbbbbbbbaaaaaaaaaabbbbbbbbbbaaaaaaaaaabbbbbbbbbbaaaaaaaaaabbbbbbbbbbaaaaaaaaaa@cccc.ddddd"))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Enter no more than 100 characters."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email is invalid)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "example"))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email has consequent dots)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "ex..ample@example.com"))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email has symbols). Please note, this email actually is valid" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "check$%^&&@yahoo.comm"))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }

    "display form error message (transferor email does not include TLD)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "example@example"))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      val labelName = form.select("label[for=transferor-email]").first()
      labelName.getElementsByClass("error-message").first() shouldNot be(null)
      labelName.getElementsByClass("error-message").first().text() shouldBe "Give a valid email address."
      document.getElementById("transferor-email-error").text() shouldBe "Confirm your email"
    }
  }

  "Calling non-pta finished page" should {

    "successfully authenticate the user and have finished page and content" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.nino1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        relationshipCreated = Some(true),
        selectedYears = Some(List(2015))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Marriage Allowance - Complete"
      document.getElementsByClass("heading-large").text shouldBe "Marriage Allowance application successful"
      document.getElementById("paragraph-1").text shouldBe "An email with full details acknowledging your application will be sent to you at example123@example.com from noreply@tax.service.gov.uk within 24 hours."
    }
  }

  "PTA Benefit calculator page " should {

    "successfully load the calculator page " in new WithApplication(fakeApplication) {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Criteria"

      val heading = document.getElementsByClass("heading-xlarge").text
      heading shouldBe "Marriage Allowance calculator"
    }
  }

  "PTA How It Works page for multi year " should {

    "successfully loaded " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.title() shouldBe "Marriage Allowance - How It Works"

      val heading = document.getElementsByClass("heading-xlarge").text
      heading shouldBe "Apply for Marriage Allowance"

      val button = document.getElementById("get-started")
      button shouldNot be(null)
      button.text shouldBe "Start now to see if you're eligible for Marriage Allowance"
    }

  }

  "PTA Eligibility check page for multiyear" should {

    "successfully authenticate the user and have eligibility-check page action" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      val elements = document.getElementById("eligibility-form").getElementsByTag("p")
      elements shouldNot be(null)
      elements.get(0).text shouldBe "Are you currently married or in a legally registered civil partnership?"
      val civilPartnership = document.getElementById("civil-partnership")
      civilPartnership shouldNot be(null)
      civilPartnership.attr("href") shouldBe "https://www.gov.uk/marriages-civil-partnerships/overview"
    }

    "diplay errors as none of the radio buttons are selected " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification") shouldNot be(null)
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Tell us if you are married or in a legally registered civil partnership."

    }
  }

  "GDS Eligibility check page for multiyear" should {

    "successfully authenticate the user and have eligibility-check page action" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      val elements = document.getElementById("eligibility-form").getElementsByTag("p")
      elements shouldNot be(null)
      elements.get(0).text shouldBe "Are you currently married or in a legally registered civil partnership?"
      val civilPartnership = document.getElementById("civil-partnership")
      civilPartnership shouldNot be(null)
      civilPartnership.attr("href") shouldBe "https://www.gov.uk/marriages-civil-partnerships/overview"
    }

    "diplay errors as none of the radio buttons are selected " in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification") shouldNot be(null)
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Tell us if you are married or in a legally registered civil partnership."

    }
  }
  "PTA lower earner check page for multiyear" should {

    "successfully authenticate the user and have income-check page and content" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.lowerEarnerCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"

      document.getElementsByClass("bold-small").text shouldBe "Are you the lower earner with a pre-tax income currently £11,000 or less a year?"
      document.getElementsByClass("information").text shouldBe "To apply for Marriage Allowance, you must be the lower earner in the relationship and earn £11,000 or less a year."
    }

  }

  "PTA partners income check page for multiyear" should {

    "successfully authenticate the user and have partners-income page and content" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.partnersIncomeCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"

      document.getElementsByClass("bold-small").text shouldBe "Is your spouse or civil partner's pre-tax income currently between £11,001 and £43,000 a year?"
      document.getElementsByClass("information").text shouldBe "To be eligible for Marriage Allowance, your partner must earn between £11,001 and £43,000 a year."
      document.getElementsByClass("heading-xlarge").text shouldBe "Your spouse or civil partner's income"
    }

  }

  "GDS lower earner page for multiyear" should {

    "successfully authenticate the user and have lower earner page and content" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.lowerEarnerCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementsByClass("bold-small").text shouldBe "Are you the lower earner with a pre-tax income currently £11,000 or less a year?"
      document.getElementsByClass("form-hint").text shouldBe "This is your income figure before any tax is deducted."
      document.getElementsByClass("Information").text shouldBe "To apply for Marriage Allowance, you must be the lower earner in the relationship and earn £11,000 or less a year."
    }
  }

  "GDS partners income page for multiyear" should {

    "successfully authenticate the user and have partners income page and content" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.partnersIncomeCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementsByClass("bold-small").text shouldBe "Is your spouse or civil partner's pre-tax income currently between £11,001 and £43,000 a year?"
      document.getElementsByClass("form-hint").text shouldBe "This is their income figure before any tax is deducted."
      document.getElementsByClass("Information").text shouldBe "To be eligible for Marriage Allowance, your partner must earn between £11,001 and £43,000 a year."
    }
  }
}
