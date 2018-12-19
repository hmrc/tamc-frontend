/*
 * Copyright 2018 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, cookies, defaultAwaitTimeout, redirectLocation}
import test_utils.TestData.{Cids, Ninos}
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.test.UnitSpec

class RoutesTest extends ControllerBaseSpec {


  "Hitting home endpoint directly" should {
    "redirect to landing page (with passcode)" in {
      val result = eligibilityController.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
    }

    "redirect to landing page (without passcode)" in {
      val result = eligibilityController.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
    }
  }

  "GDS Journey enforcer" should {
    "set GDS journey if no journey is present" in {
      val result = eligibilityController.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }

    "set GDS journey if invalid journey is present" in {
      val result = eligibilityController.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }

    "leave PTA journey unchanged when PTA feature is enabled" in {
      val result = eligibilityController.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }
  }

  "PTA Journey enforcer" should {
    "set PTA journey if no journey is present" in {
      val result = eligibilityController.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "set PTA journey if invalid journey is present" in {
      val result = eligibilityController.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "leave PTA journey unchanged" in {
      val result = eligibilityController.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }
    "check back link on calculator page" in {
      val result = eligibilityController.ptaCalculator()(request)
      val document = Jsoup.parse(contentAsString(result))
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/how-it-works")
    }
  }

  "Hitting calculator page" should {
    "have a ’previous’ and ’next’ links to gov.ukpage" in {
      val result = eligibilityController.gdsCalculator()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val previous = document.getElementById("previous")
      previous shouldNot be(null)
      previous.attr("href") shouldBe "https://www.gov.uk/marriage-allowance-guide/how-it-works"
      val next = document.getElementById("next")
      next shouldNot be(null)
      next.attr("href") shouldBe "https://www.gov.uk/marriage-allowance/how-to-apply"
    }
  }

  "Transfer page" should {
    "redirect to status page if relationship creation is locked" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None, relationshipCreated = Some(true)))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }


    "redirect to status page if transferor is not in cache" in {
      val trRecipientData = Some(CacheData(transferor = None, recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to status page if cache is empty " in {
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = None)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to iv page if user tries to hit any authorized page directly and user has not seen eligibility or verify page" in {
      val testComponent = makeTestComponent(dataId = "not_logged_in", riskTriageRouteBiasPercentageParam = 100)
      val controllerToTest = testComponent.controller
      val request = FakeRequest()
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("bar")
    }

    "redirect to IDA login page if user has insufficient Level of Assurance (LOA 1)" in {
      val testComponent = makeTestComponent("user_LOA_1")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("jazz")
    }

    "redirect to transfer page if user has LOA 100 access level" in {
      val testComponent = makeTestComponent("user_LOA_1_5")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")

      controllerToTest.auditEventsToTest.size shouldBe 0
    }

    "redirect to transfer page if user has LOA 500 access level" in {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")

      controllerToTest.auditEventsToTest.size shouldBe 0
    }

    "have correct action and method to marriage-allowance-application/transfer-allowance" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe "/marriage-allowance-application/transfer-allowance"
    }

  }

  "Transfer Action page" should {

    "redirect to status page if relationship creation is locked" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None, relationshipCreated = Some(true)))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to status page if transferor is not in cache" in {
      val trRecipientData = Some(CacheData(transferor = None, recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect transferAction to status page if transferor is not in cache" in {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }
  }

  "Confirmation page" should {
    "have correct action and method to finish page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("create").text() shouldBe "Confirm your application"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/confirm-your-email")
    }


    "have 'Confirm your application' " in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("create").text() shouldBe "Confirm your application"
    }

    "have link to edit email page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("edit-email")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/marriage-allowance-application/confirm-your-email"
    }
    "have link to edit partner details and edit marriage details" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val changeLink = document.getElementById("edit-partner-details")
      val marriageLink = document.getElementById("edit-marriage-date")
      changeLink shouldNot be(null)
      marriageLink shouldNot be(null)
      changeLink.attr("href") shouldBe "/marriage-allowance-application/transfer-allowance"
      marriageLink.attr("href") shouldBe "/marriage-allowance-application/date-of-marriage"
    }
  }

  "Finished page" should {

    "have check your marriage allowance link for non-PTA journey" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val ptaLink = document.getElementById("paragraph-5")
      ptaLink shouldNot be(null)
      ptaLink.getElementById("pta-link").attr("href") shouldBe "https://www.gov.uk/personal-tax-account"
    }

    "have check your marriage allowance link for PTA journey" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val ptaLink = document.getElementById("paragraph-5")
      ptaLink shouldNot be(null)
      ptaLink.getElementById("pta-link").attr("href") shouldBe "https://www.gov.uk/personal-tax-account"
    }

    "redirect to transfer-allowance if relation is not locked" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = Some(false)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.finished(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to transfer-allowance if relation lock is not present" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.finished(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }
  }

  "Signout page" should {
    "redirect to IDA signout" in {
      val controllerToTest = app.injector.instanceOf[AuthorisationController]
      val result = controllerToTest.logout(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/ida/signout")
    }
  }

  "PTA Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification") shouldNot be(null)
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Tell us if you are married or in a legally registered civil partnership"
    }

    "diplay errors as wrong input is provided by selected radio button" in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"
    }

    "redirect to date of birth if answer is yes" in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/date-of-birth-check-pta"))
    }

    "go to not eligible page (finish page) if no is selected" in {
      val result = eligibilityController.eligibilityCheckAction()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "You are not eligible in the current tax year - Marriage Allowance eligibility - GOV.UK"

      val finish = document.getElementById("button-finished")
      finish shouldNot be(null)
      finish.attr("href") shouldBe ApplicationConfig.ptaFinishedUrl
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
    }
  }

  "PTA date of birth check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.dateOfBirthCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
    }

    "redirect to who should do you live in scotland page irrespective of selection" in {
      val result = eligibilityController.dateOfBirthCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/do-you-live-in-scotland-pta"))
    }
  }

  "PTA do you live in scotland page for multi year" should {
    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.doYouLiveInScotlandAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you live in Scotland? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back.attr("href") shouldBe marriageAllowanceUrl("/date-of-birth-check-pta")
    }

    "redirect to lower earner check page irrespective of selection" in {
      val result = eligibilityController.doYouLiveInScotlandAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/lower-earner-pta"))
    }
  }

  "PTA lower earner check page for multi year" should {
    "diplay errors as no radio buttons is selected " in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE)
      val higherThreshold = formatter.format(ApplicationConfig.MAX_LIMIT)
      val result = eligibilityController.lowerEarnerCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your income less than £$lowerThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back.attr("href") shouldBe marriageAllowanceUrl("/do-you-live-in-scotland-pta")
    }

    "redirect to who should transfer page irrespective of selection" in {
      val result = eligibilityController.lowerEarnerCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/partners-income-pta"))
    }

  }

  "PTA partners income check page for multi year" should {

    "display errors as no radio buttons is selected for English resident" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE + 1)
      val higherThreshold = formatter.format(ApplicationConfig.MAX_LIMIT)
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe s"Confirm if your partner has an annual income of between £$lowerThreshold and £$higherThreshold"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner-pta")
    }

    "display errors as no radio buttons is selected for Scottish resident" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE + 1)
      val higherScotThreshold = formatter.format(ApplicationConfig.MAX_LIMIT_SCOT)
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherScotThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe s"Confirm if your partner has an annual income of between £$lowerThreshold and £$higherScotThreshold"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner-pta")
    }

    "redirect to do you want to apply page irrespective of selection" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/do-you-want-to-apply-pta"))
    }

  }


  "PTA do you want to apply page for multi year" should {
    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.doYouWantToApplyAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you want to apply for Marriage Allowance? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/partners-income-pta")
    }

    "redirect to my pta home page if no chosen" in {
      val result = eligibilityController.doYouWantToApplyAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("http://localhost:9232/personal-account")
    }

    "redirect to transfer page if yes chosen" in {
      val result = eligibilityController.doYouWantToApplyAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/transfer-allowance"))
    }
  }

  "GDS Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification") shouldNot be(null)
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Tell us if you are married or in a legally registered civil partnership"
    }

    "diplay errors as wrong input is provided by selected radio button" in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe ("https://www.gov.uk/apply-marriage-allowance")
    }

    "redirect to lower earner page if answer is yes and have back button" in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/date-of-birth-check"))
    }

    "go to not eligible page (finish page) if no is selected" in {
      val result = eligibilityController.eligibilityCheckAction()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "You are not eligible in the current tax year - Marriage Allowance eligibility - GOV.UK"

      val finish = document.getElementById("button-finished")
      finish shouldNot be(null)
      finish.attr("href") shouldBe "https://www.gov.uk/marriage-allowance-guide"
    }
  }

  "GDS lower earner page for multi year" should {

    "display errors as no radio buttons is selected " in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE )
      val higherThreshold = formatter.format(ApplicationConfig.MAX_LIMIT)
      val result = eligibilityController.lowerEarnerCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your income less than £$lowerThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("lower-earner-error").text shouldBe "Confirm if you have the lower income in the relationship"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/do-you-live-in-scotland")
    }

    "redirect to who should transfer page irrespective of selection" in {
      val result = eligibilityController.lowerEarnerCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/partners-income"))
    }

  }

  "GDS partners income page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE + 1)
      val higherThreshold = formatter.format(ApplicationConfig.MAX_LIMIT)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementsByClass("partners-inc-error").text shouldBe "You are not eligible for Marriage Allowance in this tax year because your partner’s income is too high or too low. You can still apply for previous years if their income was higher or lower in the past."

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner")
    }

    "redirect to do you want to apply page irrespective of selection" in {
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/do-you-want-to-apply"))
    }
  }

  "GDS date of birth check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.dateOfBirthCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check")
    }

    "redirect to do you live in scotland page irrespective of selection" in {
      val result = eligibilityController.dateOfBirthCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/do-you-live-in-scotland"))
    }

  }

  "GDS do you live in scotland page for multi year" should {
    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.doYouLiveInScotlandAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you live in Scotland? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back.attr("href") shouldBe marriageAllowanceUrl("/date-of-birth-check")
    }

    "redirect to who should lower earner page irrespective of selection" in {
      val result = eligibilityController.doYouLiveInScotlandAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/lower-earner"))
    }
  }

  "GDS do you want to apply page for multi year" should {
    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.doYouWantToApplyAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Do you want to apply for Marriage Allowance? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back.attr("href") shouldBe marriageAllowanceUrl("/partners-income")
    }

    "redirect to gov uk ma page if no chosen" in {
      val result = eligibilityController.doYouWantToApplyAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("https://www.gov.uk/marriage-allowance")
    }

    "redirect to confirm id page if yes chosen" in {
      val result = eligibilityController.doYouWantToApplyAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/history"))
    }
  }

  def eligibilityController: EligibilityController = app.injector.instanceOf[EligibilityController]
}
