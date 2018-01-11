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

import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.mvc.Cookie
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, defaultAwaitTimeout, redirectLocation}
import services.TimeService
import test_utils.TestData.{Cids, Ninos}
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class MarriageAllowanceControllerTest extends UnitSpec with TestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

  "Calling transfer Form page" should {
    "display registration page if transferor exits and doesn’t have existing relationship" in {
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
    "display form with error if recipient form data is not provided" in {
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

    "Invalid gender error" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = None, notification = None))
      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "X"), ("nino" -> Ninos.ninoWithLOA1))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe BAD_REQUEST
      val document = Jsoup.parse(contentAsString(result))
      val form = document.getElementById("register-form")
      form shouldNot be(null)
      form.getElementsByClass("error-notification").first() shouldNot be(null)
    }

    "not store data if recipient form data is not provided" in {
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

    "accept NINO with spaces and mixed case and save it in cannonical form (no spaces, upper case)" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 3, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, availableTaxYears = List(TaxYear(2014)))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoWithLOA1), ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.transferAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-marriage")
    }

    "store data if recipient exists and is not in relationship" in {
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

    "allow user to apply for marriage allowance if previous or current years are available" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2016")
      val rcrec = UserRecord(cid = 123456, timestamp = "2016")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2016, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2016)),
        recipientDetailsFormData = cacheRecipientFormData))

      val loggedInUser = LoggedInUserInfo(999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), relationshipUpdated = Some(false))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData, testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("dateOfMarriage.day" -> "6"), ("dateOfMarriage.month" -> "4"), ("dateOfMarriage.year" -> TimeService.getCurrentTaxYear.toString))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligible-years")
    }

    "display no eligible years page if user user is eligible for none years on eligibleYears page" in {

      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 3, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, availableTaxYears = List())
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.eligibleYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error").text() shouldBe "We were unable to process your Marriage Allowance application."
    }

    "show extra years page if user is eligible for one historic year on eligibleYears page" in {

      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 3, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, availableTaxYears = List(TaxYear(2014)))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.eligibleYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "You can apply for earlier tax years"
    }

    "display the current eligible years page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2011, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2014), TaxYear(2015), TaxYear(2016)))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        recipientDetailsFormData = cacheRecipientFormData))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.eligibleYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/date-of-marriage")
    }

    "show No Tax Years selected page if user is only eligible for current year and chooses no" in {

      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2016, 4, 24)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get, availableTaxYears = List(TaxYear(2016)))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com")))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("applyForCurrentYear" -> "false"))
      val result = controllerToTest.eligibleYearsAction(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("message").text() shouldBe "You have not selected any tax years to apply for"
    }
  }

  "Calling previous year page" should {
    "not display previous years if user only eligible for current year" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2016, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2016)))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        recipientDetailsFormData = cacheRecipientFormData))


      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("applyForCurrentYear" -> "true"))
      val result = controllerToTest.eligibleYearsAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-your-email")
    }

    "not display current year if user only eligible for only previous year" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2010, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2015)))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        recipientDetailsFormData = cacheRecipientFormData))


      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.eligibleYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "You can apply for earlier tax years"
    }

    "display the generic previous year page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2011, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2014), TaxYear(2015), TaxYear(2016)))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        recipientDetailsFormData = cacheRecipientFormData))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("applyForCurrentYear" -> "true"))
      val result = controllerToTest.eligibleYearsAction(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "You can apply for earlier tax years"
    }

    "previous year form with error" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2011, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2014), TaxYear(2015), TaxYear(2016)))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        recipientDetailsFormData = cacheRecipientFormData))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("applyForCurrentYear" -> ""))
      val result = controllerToTest.eligibleYearsAction(request)

      status(result) shouldBe BAD_REQUEST

    }
  }

  "Calling earlier years select page" should {

    "progressing by selecting continue on previous year page " in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2011, 4, 10))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata, availableTaxYears = List(TaxYear(2014), TaxYear(2015), TaxYear(2016)))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        recipientDetailsFormData = cacheRecipientFormData))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.previousYears(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("heading").text() shouldBe "Confirm the earlier years you want to apply for"
    }
  }

  "Calling confirm and apply page" should {

    "read keystore" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1
    }

    "redirect if no year is selected" in {
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
      document.getElementById("message").text() shouldBe "You have not selected any tax years to apply for"
    }

    "redirect if no year is selected (empty list)" in {
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
      document.getElementById("message").text() shouldBe "You have not selected any tax years to apply for"
    }

    "retrieve correct keystore data for female recipient" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        selectedYears = selectedYears,
        dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

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

      document.getElementById("outcome-2014").text() shouldBe "HMRC will check the details you have supplied before sending foo a cheque by post for up to £212."
      document.getElementById("outcome-2015").text() shouldBe "HMRC will check the details you have supplied before sending foo a cheque by post for up to £212."

      document.getElementById("change-2014").attr("href") shouldBe "/marriage-allowance-application/eligible-years"
      document.getElementById("change-2015").attr("href") shouldBe "/marriage-allowance-application/eligible-years"
    }

    "retrieve correct keystore data for recipient when only first name is avaliable" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = Some(CitizenName(Some("Foo"), None)))
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name").text() shouldBe "Foo"
    }

    "retrieve correct keystore data for recipient when only last name is avaliable" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = Some(CitizenName(None, Some("Bar"))))
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name").text() shouldBe "Bar"
    }

    "retrieve correct keystore data for recipient when first name and last name is not avaliable" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = Some(CitizenName(None, None)))
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      controllerToTest.cachingRetrievalCount shouldBe 1

      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("transferor-name") shouldBe null
    }

    "retrieve correct keystore data for male recipient" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("M"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

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

    "redirect to transfer page if transferor data is missing in cache" in {
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

    "redirect to transfer page if recipient data is missing in cache" in {
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

    "redirect to confirm email page if notification is missing in cache" in {
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

    "accept application form if user has successfully submitted application (GDS journey)" in {
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

    "accept application form if user has successfully submitted application (PTA journey)" in {
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

    "redirect to transfer page if transferor details are not in cache" in {
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

    "redirect to transfer page if recipient details are not in cache" in {
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

    "redirect to confirm email page if transferor notification details are not in cache" in {
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

    "send audit event if user has successfully created relationship when journey is through GDS" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015)),
        dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

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
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,None)),Some(RecipientRecord(UserRecord(" + Cids.cid2 + ",2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoWithLOA1 + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),None,Some(List(2015)),None,Some(DateOfMarriageFormInput(2015-01-01)))"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxSuccessful", detailsToCheck, tags)
    }

    "send audit event if user has successfully created relationship when journey is through PTA" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015)),
        dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

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
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,None)),Some(RecipientRecord(UserRecord(" + Cids.cid2 + ",2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoWithLOA1 + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),None,Some(List(2015)),None,Some(DateOfMarriageFormInput(2015-01-01)))"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxSuccessful", detailsToCheck, tags)
    }

    "send audit event if relationship is already created" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = (Some(true)), dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

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
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,None)),Some(RecipientRecord(UserRecord(" + Cids.cid2 + ",2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoWithLOA1 + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),Some(true),None,None,Some(DateOfMarriageFormInput(2015-01-01)))"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxFailed", detailsToCheck, tags)
    }

    "redirect to transfer/history page if recieves 409-conflict TAMC:ERROR:RELATION-MIGHT-BE-CREATED from" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoHappyPath), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))
      val testComponent = makeTestComponent("conflict_409", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "redirect to transfer/history page if recieves 503-LTM000503 TAMC:ERROR:RELATION-MIGHT-BE-CREATED from" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoHappyPath), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))
      val testComponent = makeTestComponent("ltm000503", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

  }

  "Confirm your email page" should {

    "read from keystore and display empty email field" in {
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

    "read from keystore and display cached email value" in {
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

    "redirect ro transfer page if transferor data is missing" in {
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

    "redirect ro transfer page if recipient data is missing" in {
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

    "save a valid email and redirect to confirmation page" in {
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
