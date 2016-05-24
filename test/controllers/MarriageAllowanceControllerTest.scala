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

import scala.concurrent.Future
import org.jsoup.Jsoup
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
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import events.CreateRelationshipSuccessEvent
import play.api.mvc.Cookie
import test_utils.TestConstants
import org.joda.time.LocalDate
import test_utils.TestData.Ninos
import test_utils.TestData.Cids
class MarriageAllowanceControllerTest extends UnitSpec with TestUtility {

  "Calling transfer Form page" should {
    "display registration page if transferor exits and doesn't have existing relationship" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.transfer()(request)

      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
    }
  }

  "Calling transfer Submit page" should {
    "display form with error if recipient form data is not provided" in new WithApplication(fakeApplication) {
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
      form.getElementsByClass("error-notification").first() shouldNot be(null)
    }

    "not store data if recipient form data is not provided" in new WithApplication(fakeApplication) {
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
      form.getElementsByClass("error-notification").first() shouldNot be(null)
      controllerToTest.cachingTransferorRecordToTestCount shouldBe 0
      controllerToTest.cachingTransferorRecordToTest shouldBe None
      controllerToTest.cachingRecipientRecordToTestCount shouldBe 0
      controllerToTest.cachingRecipientRecordToTest shouldBe None
    }

    "accept NINO with spaces and mixed case and save it in cannonical form (no spaces, upper case)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 3, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, aivailableTaxYears = List(TaxYear(2014)))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe OK
    }

    "store data if recipient exists and is not in relationship" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2016, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015)),
        recipientDetailsFormData = cacheRecipientFormData))

      val loggedInUser = LoggedInUserInfo(999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "98765", "20130101", Some(""), Some("20140101"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), relationshipUpdated = Some(false))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData, testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligible-years")

      controllerToTest.cachingTransferorRecordToTestCount shouldBe 0
      controllerToTest.cachingTransferorRecordToTest shouldBe None
      controllerToTest.cachingRecipientRecordToTestCount shouldBe 1
      controllerToTest.cachingRecipientRecordToTest shouldBe Some(UserRecord(cid = Cids.cid2, timestamp = "2015", has_allowance = Some(false)))
      controllerToTest.cachingRecipientDataToTest shouldBe Some(RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
    }

    "display noeligibleyears page if user user is eligible for none years on eligibleYears page" in new WithApplication(fakeApplication) {

      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 3, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, aivailableTaxYears = List())
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.eligibleYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error").text() shouldBe "We were unable to process your Marriage Allowance application."
    }

    "showextrayears page if user is eligible for one historic year on eligibleYears page" in new WithApplication(fakeApplication) {

      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 3, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, aivailableTaxYears = List(TaxYear(2014)))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.eligibleYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "Confirm the earlier years you want to apply for"
    }

  }

  "Calling confirm and apply page" should {

    "read keystore" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1
    }

    "redirect if no year is selected" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = None
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("message").text() shouldBe "You haven't selected any tax years to apply for"
    }

    "redirect if no year is selected (empty list)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List[Int]())
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("message").text() shouldBe "You haven't selected any tax years to apply for"
    }

    "retrieve correct keystore data for female recipient" in new WithApplication(fakeApplication) {
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
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name").text() shouldBe "Foo Bar"
      document.getElementById("recipient-name").text() shouldBe "foo bar"
      document.getElementById("transferor-email").text() shouldBe "example@example.com"
      document.getElementById("recipient-nino").text() shouldBe Ninos.ninoWithLOA1Spaces

      document.getElementById("year-2014").text() shouldBe "Previous tax year: 6 April 2014 to 5 April 2015"
      document.getElementById("year-2015").text() shouldBe "Previous tax year: 6 April 2015 to 5 April 2016"

      document.getElementById("outcome-2014").text() shouldBe "HMRC will check the details you've supplied before sending foo a cheque by post for up to £212."
      document.getElementById("outcome-2015").text() shouldBe "HMRC will check the details you've supplied before sending foo a cheque by post for up to £212."

      document.getElementById("change-2014").attr("href") shouldBe "/marriage-allowance-application/eligible-years"
      document.getElementById("change-2015").attr("href") shouldBe "/marriage-allowance-application/eligible-years"
    }

    "retrieve correct keystore data for recipient when only first name is avaliable" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = Some(CitizenName(Some("Foo"), None)))
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name").text() shouldBe "Foo"
    }

    "retrieve correct keystore data for recipient when only last name is avaliable" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = Some(CitizenName(None, Some("Bar"))))
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name").text() shouldBe "Bar"
    }

    "retrieve correct keystore data for recipient when first name and last name is not avaliable" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = Some(CitizenName(None, None)))
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name") shouldBe null
    }

    "retrieve correct keystore data for male recipient" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("M"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("transferor-name").text() shouldBe "Foo Bar"
      document.getElementById("recipient-name").text() shouldBe "foo bar"
      document.getElementById("transferor-email").text() shouldBe "example@example.com"
      document.getElementById("recipient-nino").text() shouldBe Ninos.ninoWithLOA1Spaces
    }

    "redirect to transfer page if transferor data is missing in cache" in new WithApplication(fakeApplication) {
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("M"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = None, recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.cachingRetrievalCount shouldBe 1
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to transfer page if recipient data is missing in cache" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.cachingRetrievalCount shouldBe 1
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to confirm email page if notification is missing in cache" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("M"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = None))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.cachingRetrievalCount shouldBe 1
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-your-email")
    }

    "accept application form if user has successfully submitted application (GDS journey)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished")

      controllerToTest.cachingRetrievalCount shouldBe 1
      controllerToTest.cachingLockCreateRelationship shouldBe 1
      controllerToTest.cachingLockCreateValue shouldBe Some(true)
      controllerToTest.createRelationshipUrl shouldBe Some("foo/paye/" + Ninos.ninoHappyPath + "/create-multi-year-relationship/GDS")
    }

    "accept application form if user has successfully submitted application (PTA journey)" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished")

      controllerToTest.cachingRetrievalCount shouldBe 1
      controllerToTest.cachingLockCreateRelationship shouldBe 1
      controllerToTest.cachingLockCreateValue shouldBe Some(true)
      controllerToTest.createRelationshipUrl shouldBe Some("foo/paye/" + Ninos.ninoHappyPath + "/create-multi-year-relationship/PTA")
    }

    "redirect to transfer page if transferor details are not in cache" in new WithApplication(fakeApplication) {
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = None, recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
      controllerToTest.cachingRetrievalCount() shouldBe 1
    }

    "redirect to transfer page if recipient details are not in cache" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = Some(NotificationRecord(EmailAddress("example123@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
      controllerToTest.cachingRetrievalCount() shouldBe 1
    }

    "redirect to confirm email page if transferor notification details are not in cache" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = None))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-your-email")
      controllerToTest.cachingRetrievalCount() shouldBe 1
    }

    "send audit event if user has successfully created relationship when journey is through GDS" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished")

      controllerToTest.auditEventsToTest.size shouldBe 1
      val event = controllerToTest.auditEventsToTest.head
      val detailsToCheck = Map(
        "event" -> "create-relationship-GDS",
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,None)),Some(RecipientRecord(UserRecord(" + Cids.cid2 + ",2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoWithLOA1 + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),None,Some(List(2015)),None)"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxSuccessful", detailsToCheck, tags)
    }

    "send audit event if user has successfully created relationship when journey is through PTA" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished")

      controllerToTest.auditEventsToTest.size shouldBe 1
      val event = controllerToTest.auditEventsToTest.head
      val detailsToCheck = Map(
        "event" -> "create-relationship-PTA",
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,None)),Some(RecipientRecord(UserRecord(" + Cids.cid2 + ",2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoWithLOA1 + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),None,Some(List(2015)),None)"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxSuccessful", detailsToCheck, tags)
    }
//
    "send audit event if relationship is already created" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = (Some(true))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")

      controllerToTest.auditEventsToTest.size shouldBe 2
      val event = controllerToTest.auditEventsToTest.head
      val detailsToCheck = Map(
        "event" -> "relationship-exists",
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,None)),Some(RecipientRecord(UserRecord(" + Cids.cid2 + ",2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoWithLOA1 + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),Some(true),None,None)"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxFailed", detailsToCheck, tags)
    }

  }

  "Confirm your email page" should {

    "read from keystore and display empty email field" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = None))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      controllerToTest.cachingRetrievalCount() shouldBe 1
      document.getElementById("transferor-email").attr("value") shouldBe ""
    }

    "read from keystore and display cached email value" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(transferor_email = EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      controllerToTest.cachingRetrievalCount() shouldBe 1
      document.getElementById("transferor-email").attr("value") shouldBe "example@example.com"
    }

    "redirect ro transfer page if transferor data is missing" in new WithApplication(fakeApplication) {
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = None, recipient = Some(recrecord), notification = None))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
      controllerToTest.cachingRetrievalCount() shouldBe 1
    }

    "redirect ro transfer page if recipient data is missing" in new WithApplication(fakeApplication) {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = Some(NotificationRecord(transferor_email = EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
      controllerToTest.cachingRetrievalCount() shouldBe 1
    }

    "save a valid email and redirect to confirmation page" in new WithApplication(fakeApplication) {
      val testComponent = makeTestComponent("user_happy_path")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.confirmYourEmailAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm")
      controllerToTest.saveNotificationCount shouldBe 1
      controllerToTest.notificationToTest shouldBe Some(NotificationRecord(EmailAddress("example@example.com")))
    }
  }
}
