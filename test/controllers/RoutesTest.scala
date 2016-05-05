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

import org.jsoup.Jsoup
import config.ApplicationConfig
import models.CacheData
import models.CitizenName
import models.Gender
import models.NotificationRecord
import models.RecipientRecord
import models.RegistrationFormInput
import models.UserRecord
import models.RelationshipRecord
import models.LoggedInUserInfo
import models.Role
import models.UpdateRelationshipCacheData
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.FORBIDDEN
import play.api.test.Helpers.OK
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.cookies
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers.session
import play.api.test.WithApplication
import play.api.test.WithApplication
import test_utils.TestConstants
import test_utils.TestUtility
import test_utils.UpdateRelationshipTestUtility
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import test_utils.TestConstants
import org.joda.time.LocalDate
import test_utils.TestData.Ninos
import test_utils.TestData.Cids

class RoutesTest extends UnitSpec with TestUtility {

  "Hitting home endpoint directly" should {
    "redirect to landing page (with passcode)" in new WithApplication(fakeApplication) {
      val request = FakeRequest()
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
    }

    "redirect to landing page (without passcode)" in new WithApplication(fakeApplication) {
      val request = FakeRequest()
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.home()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
    }
  }

  "GDS Journey enforcer" should {
    "set GDS journey if no journey is present" in new WithApplication(fakeApplication) {
      val request = FakeRequest()
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }

    "set GDS journey if invalid journey is present" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "ABC"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }

    "leave PTA journey unchanged when PTA feature is enabled" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }
  }

  "PTA Journey enforcer" should {
    "set PTA journey if no journey is present" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "set PTA journey if invalid journey is present" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "ABC"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "leave PTA journey unchanged" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }
  }

  "Hitting calculator page" should {
    "have a 'previous' and 'next' links to gov.ukpage" in new WithApplication(fakeApplication) {
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
      next.attr("href") shouldBe "https://www.gov.uk/marriage-allowance-guide/how-to-apply"
    }
  }

  "Hitting verify page" should {
    "have a link to register page (if user has seen eligibility check page)" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.verify()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val continue = document.getElementById("verify")
      continue shouldNot be(null)
      continue.text() shouldBe "Start now"
      continue.attr("href") shouldBe "/marriage-allowance-application/history"
    }
  }

  "Transfer page" should {
    "redirect to status page if relationship creation is locked" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None, relationshipCreated = Some(true)))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }


    "redirect to status page if transferor is not in cache" in new WithApplication(fakeApplication) {
      val trRecipientData = Some(CacheData(transferor = None, recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to status page if cache is empty " in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = None)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transfer(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to verify page if user tries to hit any authorized page directly and user has not seen eligibility or verify page" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent(dataId = "not_logged_in", riskTriageRouteBiasPercentageParam = 100)
      val controllerToTest = testComponent.controller
      val request = FakeRequest()
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
    }

    "redirect to IV if user tries to hit any authorized page directly and user has seen eligibility page" in new WithApplication(fakeApplication) {
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

    "redirect to IDA login page if user has insufficient Level of Assurance (LOA 1)" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_LOA_1")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("jazz")
    }

    "redirect to transfer page if user has LOA 100 access level" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_LOA_1_5")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")

      controllerToTest.auditEventsToTest.size shouldBe 0
    }

    "redirect to transfer page if user has LOA 500 access level" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")

      controllerToTest.auditEventsToTest.size shouldBe 0
    }

    "have correct action and method to marriage-allowance-application/transfer-allowance" in new WithApplication(fakeApplication) {
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

    "have beta feedback link" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val feedback = document.getElementById("feedback-link")
      feedback shouldNot be(null)
      feedback.attr("href") shouldBe "http://localhost:9250/contact/beta-feedback-unauthenticated?service=TAMC"
    }

  }

  "Transfer Action page" should {

    "redirect to status page if relationship creation is locked" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None, relationshipCreated = Some(true)))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to status page if transferor is not in cache" in new WithApplication(fakeApplication) {
      val trRecipientData = Some(CacheData(transferor = None, recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect transferAction to status page if transferor is not in cache" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }
  }

  "Confirmation page" should {
    "have correct action and method to finish page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("create").text() shouldBe "Confirm your application"
    }


    "have signout link" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("sign-out")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "have link to edit email page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

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
  }

  "Finished page" should {

    "have check your marriage allowance and survey link for non-PTA journey" in new WithApplication(fakeApplication) {
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

      val surveyLink = document.getElementById("paragraph-6")
      surveyLink shouldNot be(null)
      surveyLink.getElementById("survey-link").attr("href") shouldBe "https://www.gov.uk/done/marriage-allowance"
    }

    "have signout link and check your marriage allowance and survey link for PTA journey" in new WithApplication(fakeApplication) {
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
      val signout = document.getElementById("sign-out")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/marriage-allowance-application/logout"

      val ptaLink = document.getElementById("paragraph-5")
      ptaLink shouldNot be(null)
      ptaLink.getElementById("pta-link").attr("href") shouldBe "https://www.gov.uk/personal-tax-account"

      val surveyLink = document.getElementById("paragraph-6")
      surveyLink shouldNot be(null)
      surveyLink.getElementById("survey-link").attr("href") shouldBe "https://www.gov.uk/done/marriage-allowance"
    }

    "redirect to transfer-allowance if relation is not locked" in new WithApplication(fakeApplication) {
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

    "redirect to transfer-allowance if relation lock is not present" in new WithApplication(fakeApplication) {
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

  "No tax years for transferor error page" should {
    "have back and finish buttons with correct destinations" in new WithApplication(fakeApplication) {
      val loggedInUser = LoggedInUserInfo(cid = 999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "98764", "20160410", Some(""), Some("20160415"), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "20100401", Some(""), Some("20100403"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), historicRelationships = Some(Seq(historic1Record)), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), relationshipUpdated = Some(false))

      val testParams = makeTestComponent("user_happy_path", testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("dateOfMarriage.day" -> "10"), ("dateOfMarriage.month" -> "04"), ("dateOfMarriage.year" -> "2016"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val finished = document.getElementById("button-finished")
      finished shouldNot be(null)
      finished.attr("href") shouldBe "https://www.gov.uk/marriage-allowance-guide/how-it-works"

      val back = document.getElementById("back")
      back shouldNot be(null)
      back.attr("href") shouldBe "/marriage-allowance-application/history"
    }
  }


  "Signout page" should {
    "redirect to IDA signout" in new WithApplication(fakeApplication) {
      val controllerToTest = makeFakeHomeController()
      val result = controllerToTest.logout(FakeRequest())
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("baz")
    }
  }

  "Session timeout page" should {
    "have link to PTA page" in new WithApplication(fakeApplication) {
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

    "have link to PTA page (with PTA cookie)" in new WithApplication(fakeApplication) {
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

    "have link to register page (GDS)" in new WithApplication(fakeApplication) {

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

    "authenticate the user " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
    }

    "successfully authenticate the user " in new WithApplication(fakeApplication) {
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

    "authenticate the user " in new WithApplication(fakeApplication) {
      val testComponent = makePtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
    }

    "successfully authenticate the user " in new WithApplication(fakeApplication) {
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

    "authenticate the user " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
    }

    "go to finish in 'nyn' scenario" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("marriage-criteria" -> "false", "recipient-income-criteria" -> "true", "transferor-income-criteria" -> "false")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - You are not eligible"

      val finish = document.getElementById("button-finished")
      finish shouldNot be(null)
      finish.attr("href") shouldBe ApplicationConfig.ptaFinishedUrl
      val continue = document.getElementById("back")
      continue shouldNot be(null)
      continue.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
    }

  }

  "PTA Journey enforcer for multiyear" should {
    "set PTA journey if no journey is present" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "set PTA journey if invalid journey is present" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "ABC"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "PTA", None, "/", None, false, true))
    }

    "leave PTA journey unchanged" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      cookies(result).get("TAMC_JOURNEY") shouldBe Some(Cookie("TAMC_JOURNEY", "GDS", None, "/", None, false, true))
    }
  }
  
  "PTA Eligibility check page for multi year" should {

    "authenticate the user " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("not_logged_in")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
    }

    "successfully authenticate the user and have eligibility-check page action" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheck()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val eligibilityForm = document.getElementById("eligibility-form")
      eligibilityForm shouldNot be(null)
      eligibilityForm.attr("action") shouldBe marriageAllowanceUrl("/eligibility-check-pta")

      val civilPartnership = document.getElementById("civil-partnership")
      civilPartnership shouldNot be(null)
      civilPartnership.attr("href") shouldBe "https://www.gov.uk/marriages-civil-partnerships/overview"
    }

    "diplay errors as no radio buttons is selected " in new WithApplication(fakeApplication) {
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

    "diplay errors as wrong input is provided by selected radio button" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"
    }

    "redirect to lower earner if answer is yes" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("marriage-criteria" -> "true")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/lower-earner-pta"))
    }

    "go to not eligible page (finish page) if no is selected" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("marriage-criteria" -> "false")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.eligibilityCheckAction()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - You are not eligible"

      val finish = document.getElementById("button-finished")
      finish shouldNot be(null)
      finish.attr("href") shouldBe ApplicationConfig.ptaFinishedUrl
      val continue = document.getElementById("back")
      continue shouldNot be(null)
      continue.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
    }
  }  

  "PTA lower earner check page for multi year" should {

    "diplay errors as no radio buttons is selected " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT
    }

    "redirect to who should transfer page irrespective of selection" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("lower-earner" -> "true")
       val controllerToTest = testComponent.controller
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/partners-income-pta"))
    }

  }

  "PTA partners income check page for multi year" should {

    "diplay errors as no radio buttons is selected " in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request
      val controllerToTest = testComponent.controller
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT
      document.getElementById("partners-income-error").text() shouldBe "Confirm if your spouse or civil partner has an annual income of between £11,001 and £43,000"
    }

    "redirect to transfer controller page irrespective of selection" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withFormUrlEncodedBody("partners-income" -> "false")
      val controllerToTest = testComponent.controller
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/transfer-allowance"))
    }

  }
  
  "GDS Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in new WithApplication(fakeApplication) {
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

    "diplay errors as wrong input is provided by selected radio button" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"
    }

    "redirect to lower earner page if answer is yes" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("marriage-criteria" -> "true")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/lower-earner"))
    }

    "go to not eligible page (finish page) if no is selected" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("marriage-criteria" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.eligibilityCheckAction()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - You are not eligible"

      val finish = document.getElementById("button-finished")
      finish shouldNot be(null)
      finish.attr("href") shouldBe "https://www.gov.uk/marriage-allowance-guide"
      val continue = document.getElementById("back")
      continue shouldNot be(null)
      continue.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check")
    }
  }

  "GDS lower earner page for multi year" should {

    "diplay errors as no radio buttons is selected " in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT

      document.getElementById("lower-earner-error").text shouldBe "Confirm if you are the lower earner in the relationship"
    }

    "redirect to who should transfer page irrespective of selection" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("lower-earner" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.lowerEarnerCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/partners-income"))
    }

  }

  "GDS partners income page for multi year" should {

    "diplay errors as no radio buttons is selected " in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Marriage Allowance - Eligibility Questions"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("form-error-message").text() shouldBe TestConstants.ERROR_MANDATORY_DATA_TEXT

      document.getElementsByClass("partners-inc-error").text shouldBe "You're not eligible for Marriage Allowance in this tax year because your partner's income is too high or too low. You can still continue to check your eligibility for previous years."
    }

    "redirect to verify page irrespective of selection" in new WithApplication(fakeApplication) {
      val request = FakeRequest().withCookies(Cookie("TAMC_JOURNEY", "GDS")).withFormUrlEncodedBody("partners-income" -> "false")
      val controllerToTest = makeMultiYearGdsEligibilityController()
      val result = controllerToTest.partnersIncomeCheckAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/verify"))
    }
  }
}
