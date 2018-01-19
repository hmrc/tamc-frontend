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
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, SEE_OTHER, contentAsString, defaultAwaitTimeout, redirectLocation}
import test_utils.TestData.{Cids, Ninos}
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class ErrorsTest extends UnitSpec with TestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

  "Error handling in transfer page when submitting form" should {
    "show ’Recipient details not found’ page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound), dateOfMarriage = new LocalDate(2015, 1, 1))
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

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData, testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error").text() shouldBe "We were unable to find a HMRC record of your partner."
    }

    "show ’Technical Exception’ page" in {
      val loggedInUser = LoggedInUserInfo(999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "98765", "20130101", Some(""), Some("20140101"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), relationshipUpdated = Some(false))

      val testParams = makeTestComponent("user_happy_path", testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoError), ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error").text() shouldBe "We are experiencing technical difficulties"
    }

    "send audit event if recipient can not be found" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound), dateOfMarriage = new LocalDate(2015, 1, 1))
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

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData, testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoTransferorNotFound), ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val event = controllerToTest.auditEventsToTest.head
      val detailsToCheck = Map(
        "event" -> "recipient-error",
        "error" -> "errors.RecipientNotFound",
        "data" -> Ninos.ninoHappyPath)
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxFailed", detailsToCheck, tags)
    }

    "send audit event if there is a technical error" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val cacheRecipientFormData = Some(RecipientDetailsFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoError)))
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoError), dateOfMarriage = new LocalDate(2015, 1, 1))
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

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData, testCacheData = Some(updateRelationshipCacheData))
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("name" -> "foo"), ("last-name" -> "bar"), ("gender" -> "M"), ("nino" -> Ninos.ninoError), ("dateOfMarriage.day" -> "1"), ("dateOfMarriage.month" -> "1"), ("dateOfMarriage.year" -> "2015"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val event = controllerToTest.auditEventsToTest.head
      val detailsToCheck = Map(
        "event" -> "recipient-error",
        "error" -> "uk.gov.hmrc.http.BadGatewayException: TESTING-POST-ERROR",
        "data" -> Ninos.ninoHappyPath)
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))
      eventsShouldMatch(event, "TxFailed", detailsToCheck, tags)
    }

    "show ’no tax years for transferor’ page if transferor enters date of marriage as current tax year" in {

      val cachedRecipientData = Some(RecipientDetailsFormInput("foo", "bar", Gender("M"), Nino(Ninos.ninoWithLOA1)))
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015)),
        recipientDetailsFormData = cachedRecipientData))

      val loggedInUser = LoggedInUserInfo(cid = 999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "98764", "20160410", Some(""), Some("20170415"), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "20100401", Some(""), Some("20100403"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), historicRelationships = Some(Seq(historic1Record)), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), relationshipUpdated = Some(false))

      val testParams = makeTestComponent("user_happy_path", testCacheData = Some(updateRelationshipCacheData), transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request.withFormUrlEncodedBody(data = ("dateOfMarriage.day" -> "10"), ("dateOfMarriage.month" -> "04"), ("dateOfMarriage.year" -> "2016"))
      val result = controllerToTest.dateOfMarriageAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("description").text() shouldBe "Based on the date of marriage or civil partnership you have provided, you are not eligible for Marriage Allowance."
    }
  }

  "Error handling in confirm page" should {
    "show ’Cannot Create Relationship’ page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015))))

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("error").text() shouldBe "Cannot create relationship"
    }

    "send audit event if a relationship cannot be created when journey is from GDS" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015)),
        dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "send audit event if a relationship cannot be created when journey is from PTA" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = 123456, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoTransferorNotFound), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(
        transferor = Some(trrec),
        recipient = Some(recrecord),
        notification = Some(NotificationRecord(EmailAddress("example123@example.com"))),
        selectedYears = Some(List(2015)),
        dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testParams = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testParams.controller
      val request = testParams.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR

      controllerToTest.auditEventsToTest.size shouldBe 2
      val event = controllerToTest.auditEventsToTest.head
      val detailsToCheck = Map(
        "event" -> "create-relationship-PTA",
        "error" -> "errors.CannotCreateRelationship",
        "data" -> ("CacheData(Some(UserRecord(" + Cids.cid1 + ",2015,None,Some(CitizenName(Some(Foo),Some(Bar))))),Some(RecipientRecord(UserRecord(123456,2015,None,None),RegistrationFormInput(foo,bar,Gender(M)," + Ninos.ninoTransferorNotFound + ",2015-01-01),List())),Some(NotificationRecord(example123@example.com)),None,Some(List(2015)),None,Some(DateOfMarriageFormInput(2015-01-01)))"))
      val tags = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))

      eventsShouldMatch(event, "TxFailed", detailsToCheck, tags)

      val event2 = controllerToTest.auditEventsToTest.tail.head
      val detailsToCheck2 = Map(
        "event" -> "create-relationship",
        "error" -> "errors.CannotCreateRelationship")
      val tags2 = Map("X-Session-ID" -> ("session-ID-" + Ninos.ninoHappyPath))

      //eventsShouldMatch(event2, "TxFailed", detailsToCheck2, tags2)
    }
  }

  "Error handling after complete journey" should {
    "show relationship already created if trying edit email" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = None, relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmYourEmail()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.cachingRetrievalCount() shouldBe 1
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "show relationship already created if trying open confirmation page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = TestConstants.GENERIC_CITIZEN_NAME)
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015", name = None)
      val cachedRecipientData = Some(RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1)))
      val recrecord = RecipientRecord(record = rcrec, data = cachedRecipientData.get)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.cachingRetrievalCount() shouldBe 1
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }

    "show relationship already created if trying to confirm relationship" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmAction(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.cachingRetrievalCount() shouldBe 1
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
    }
  }
}
