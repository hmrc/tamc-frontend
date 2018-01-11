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
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, cookies, defaultAwaitTimeout, redirectLocation}
import test_utils.TestData.{Cids, Ninos}
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class RoutesTest extends UnitSpec with TestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

  "Hitting home endpoint directly" should {
    "redirect to landing page (with passcode)" in {
      val request = FakeRequest()
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
    }

    "redirect to landing page (without passcode)" in {
      val request = FakeRequest()
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
    }
  }

  "GDS Journey enforcer" should {
    "set GDS journey if no journey is present" in {
      val request = FakeRequest()
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }

    "set GDS journey if invalid journey is present" in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "ABC"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }

    "leave PTA journey unchanged when PTA feature is enabled" in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }
  }

  "PTA Journey enforcer" should {
    "set PTA journey if no journey is present" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "set PTA journey if invalid journey is present" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "ABC"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "leave PTA journey unchanged" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }
    "check back link on calculator page" in {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)
      val document = Jsoup.parse(contentAsString(result))
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/how-it-works")
    }
  }

  "Hitting calculator page" should {
    "have a ’previous’ and ’next’ links to gov.ukpage" in {
      val request = FakeRequest()
      val controllerToTest = makeEligibilityController()
      val result = controllerToTest.calculator()(request)
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

    "redirect to IV if user tries to hit any authorized page directly and user has seen eligibility page" in {
      val testComponent = makeTestComponent(dataId = "not_logged_in", riskTriageRouteBiasPercentageParam = 100)
      val controllerToTest = testComponent.controller
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("bar")

      controllerToTest.idaAuditEventsToTest.size shouldBe 1
      val event = controllerToTest.idaAuditEventsToTest.head

      val detailsToCheck = Map(
        "event" -> "authorisation-attempt",
        "data" -> "TRIAGE")
      eventsShouldMatch(event, "TxSuccessful", detailsToCheck)
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
      val controllerToTest = makeFakeHomeController()
      val result = controllerToTest.logout(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/ida/signout")
    }
  }

  "Session timeout page" should {
    "have link to PTA page" in {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.maAuthRegime.authenticationType.handleSessionTimeout(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("timed-out")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/personal-account"
    }

    "have link to PTA page (with PTA cookie)" in {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.maAuthRegime.authenticationType.handleSessionTimeout(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("timed-out")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/personal-account"
    }

    "have link to register page (GDS)" in {

      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      implicit val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.maAuthRegime.authenticationType.handleSessionTimeout

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("timed-out")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/marriage-allowance-application"
    }
  }

  "PTA How It Works page " should {

    "authenticate the user " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("bar")
    }

    "successfully authenticate the user " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val button = document.getElementById("start-now")
      button shouldNot be(null)
      button.attr("href") shouldBe marriageAllowanceUrl("/benefit-calculator-pta")
    }

  }

  "PTA Benefit calculator page " should {

    "authenticate the user " in {
      val testComponent = makePtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("bar")
    }

    "successfully authenticate the user " in {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val continue = document.getElementById("continue")
      continue shouldNot be(null)
      continue.attr("href") shouldBe "/marriage-allowance-application/eligibility-check-pta"

    }
  }

  "PTA Eligibility check page " should {

    "authenticate the user " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("bar")
    }

    "go to finish in ’nyn’ scenario" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("marriage-criteria" -> "false", "recipient-income-criteria" -> "true", "transferor-income-criteria" -> "false")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)

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

  "PTA Journey enforcer for multiyear" should {
    "set PTA journey if no journey is present" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "set PTA journey if invalid journey is present" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "ABC"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "leave PTA journey unchanged" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }
  }

  "PTA Eligibility check page for multi year" should {

    "authenticate the user " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("bar")
    }

    "successfully authenticate the user and have eligibility-check page action" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val eligibilityForm = document.getElementById("eligibility-form")
      eligibilityForm shouldNot be(null)
      eligibilityForm.attr("action") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/benefit-calculator-pta")

    }

    "diplay errors as no radio buttons is selected " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)
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
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"
    }

    "redirect to date of birth if answer is yes" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("marriage-criteria" -> "true")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/date-of-birth-check-pta"))
    }

    "go to not eligible page (finish page) if no is selected" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("marriage-criteria" -> "false")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)

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
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.dateOfBirthCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
    }

    "redirect to who should lower earner page irrespective of selection" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("date-of-birth" -> "true")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.dateOfBirthCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/lower-earner-pta"))
    }

  }

  "PTA lower earner check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Is your income less than £11,501 a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/date-of-birth-check-pta")
    }

    "redirect to who should transfer page irrespective of selection" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("lower-earner" -> "true")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/partners-income-pta"))
    }

  }

  "PTA partners income check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Is your partner’s income between £11,501 and £45,000 a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe "Confirm if your partner has an annual income of between £11,501 and £45,000 (or £43,000 if you live in Scotland)"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner-pta")
    }

    "redirect to transfer controller page irrespective of selection" in {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("partners-income" -> "false")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/transfer-allowance"))
    }

  }

  "GDS Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)
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
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)
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
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("marriage-criteria" -> "true")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/date-of-birth-check"))
    }

    "go to not eligible page (finish page) if no is selected" in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("marriage-criteria" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)

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
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Is your income less than £11,501 a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("lower-earner-error").text shouldBe "Confirm if you have the lower income in the relationship"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/date-of-birth-check")
    }

    "redirect to who should transfer page irrespective of selection" in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("lower-earner" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/partners-income"))
    }

  }

  "GDS partners income page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Is your partner’s income between £11,501 and £45,000 a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementsByClass("partners-inc-error").text shouldBe "You are not eligible for Marriage Allowance in this tax year because your partner’s income is too high or too low. You can still apply for previous years if their income was higher or lower in the past."

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner")
    }

    "redirect to verify page irrespective of selection" in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("partners-income" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/history"))
    }
  }

  "GDS date of birth check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.dateOfBirthCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check")
    }

    "redirect to who should lower earner page irrespective of selection" in {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("date-of-birth" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.dateOfBirthCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/lower-earner"))
    }

  }
}
