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

import models._
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.Helpers.OK
import play.api.test.Helpers.SEE_OTHER
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.redirectLocation
import play.api.test.Helpers.session
import play.api.test.WithApplication
import test_utils.TestUtility
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.domain.Nino
import org.jsoup.Jsoup
import uk.gov.hmrc.emailaddress.EmailAddress
import scala.concurrent.Future
import config.ApplicationConfig
import play.api.mvc.Cookie
import test_utils.TestConstants
import test_utils.UpdateRelationshipTestUtility
import org.joda.time.LocalDate
import test_utils.TestData.Ninos
import test_utils.TestData.Cids

class Signout extends UnitSpec with TestUtility {

  "PTA Sign out and status" should {

    "be on 'How It Works' page for returning user" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_returning")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, you last signed in 9:00am, Friday 13 November 2015"
    }

    "be on 'How It Works' page" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "be on 'Calculator' page" in new WithApplication(fakeApplication) {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "be on 'Eligibility Check' page" in new WithApplication(fakeApplication) {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "be on 'Transfer' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "be on 'Confirm Email' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = None))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"

    }

    "be on 'Confirm' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val selectedYears = Some(List(2014, 2015))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "be on 'Finished' page" in new WithApplication(fakeApplication) {
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
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "show sign-out on 'Recipient details not found' page" in new WithApplication(fakeApplication) {
      val loggedInUser = LoggedInUserInfo(999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testParams = makeTestComponent("user_happy_path", testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoTransferorNotFound), ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "show sign-out on 'Technical Exceptions' page" in new WithApplication(fakeApplication) {

      val loggedInUser = LoggedInUserInfo(999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testParams = makeTestComponent("user_happy_path", testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoError), ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }
    "show sign-out on 'Cannot Create Relationship' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), selectedYears = Some(List(2015))))

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "show 'Technical Exception' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = 999999, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoError), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), selectedYears = Some(List(2015))))

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error").text() shouldBe "We're experiencing technical difficulties"
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "show sign-out on 'Technical Exception' pages" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = 999999, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoError), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), selectedYears = Some(List(2015))))

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

  }

  "GDS Sign out" should {
    "be on 'How It Works' page" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "be on 'Calculator' page" in new WithApplication(fakeApplication) {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "be on 'Eligibility Check' page" in new WithApplication(fakeApplication) {
      val testComponent = makePtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.calculator()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "be on 'Transfer' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.transfer()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "be on 'Confirm Email' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = None))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "be on 'Confirm' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "be on 'Finished' page" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }
  }

  "PTA Sign out and status for multi year" should {

    "be on 'How It Works' page for returning user" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_returning")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, you last signed in 9:00am, Friday 13 November 2015"
    }

    "be on 'How It Works' page" in new WithApplication(fakeApplication) {
      val testComponent = makeMultiYearPtaEligibilityTestComponent("user_happy_path")
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val controllerToTest = testComponent.controller
      val result = controllerToTest.howItWorks()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }
  }
}
