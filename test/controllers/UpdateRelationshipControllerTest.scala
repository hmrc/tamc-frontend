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
import play.api.test.Helpers.{OK, SEE_OTHER, contentAsString, defaultAwaitTimeout, redirectLocation}
import test_utils.TestData.Cids
import test_utils.{TestConstants, UpdateRelationshipTestUtility}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class UpdateRelationshipControllerTest extends UnitSpec with UpdateRelationshipTestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

  "Update relationship email notification " should {

    "save a valid email and redirect to confirmation page" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(data = ("transferor-email" -> "example@example.com"))
      val result = controllerToTest.confirmYourEmailActionUpdate(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-change")
      controllerToTest.saveNotificationCount shouldBe 1
      controllerToTest.notificationToTest shouldBe Some(NotificationRecord(EmailAddress("example@example.com")))
    }

    "read from keystore and display email field" in {
      val loggedInUser = LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015")
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = None,
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmEmail(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      controllerToTest.cachingRetrievalCount() shouldBe 1
      document.getElementById("transferor-email").attr("value") shouldBe ""
    }
  }

  "Update relationship caching data " should {

    "have a logged in user in cached" in {
      val loggedInUser = Some(LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015"))

      val testComponent = makeUpdateRelationshipTestComponent("coc_no_relationship", loggedInUserInfo = loggedInUser)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.loggedInUserInfoCount shouldBe 1
    }

    "have divorce previous year in end relationship cache " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_PY)
      val result = controllerToTest.divorceAction()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("DIVORCE_PY"))
    }

    "have divorce previous year in end relationship cache (with divorce date)" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(
        "endReason" -> EndReasonCode.DIVORCE_PY,
        "dateOfDivorce.day" -> "20",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2015")
      val result = controllerToTest.divorceAction()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("DIVORCE_PY", Some(new LocalDate(2015, 1, 20))))
    }

    "have divorce current year in end relationship cache " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_CY)
      val result = controllerToTest.divorceAction()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("DIVORCE_CY"))
    }

    "have cancel in end relationship cache " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmCancel()(request)

      status(result) shouldBe OK
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason(EndReasonCode.CANCEL))
    }

    "have update relationship details in cache with Cancel end reason" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "20150101", Some(""), Some("20160101"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser), roleRecord = Some(""),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK

    }

    "have update relationship details in cache with rejection reason and end date from service call" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "", "20150101", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser), roleRecord = Some(""),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.REJECT)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK
    }

    "list correct tax year and have update relationship details in cache with rejection reason and history" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "", Some(""), Some(""), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20100406", Some(""), Some("20150405"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.RECIPIENT),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("confirm-page").text() shouldBe "Confirm removal of a previous Marriage Allowance claim"
      document.getElementById("confirm-note").text() shouldBe "You have asked us to remove your Marriage Allowance from tax year 2010 to 2015. This means:"
    }

    "have update relationship action details in cache " in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
    }

    "reject active relationship" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "20120101", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
    }

    "reject historic relationship" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "", Some(""), Some(""), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20130101", Some(""), Some("1-01-2014"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
    }

    "confirm rejection of relationship" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "", Some(""), Some(""), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20130101", Some(""), Some("1-01-2014"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmReject()(request)

      status(result) shouldBe OK
    }

    "Finish update after change of circumstances journey for recipient rejection" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finishUpdate()(request)

      status(result) shouldBe OK
    }

    "Finish update after change of circumstances journey for transferor rejects" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "123456", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.CANCEL, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finishUpdate()(request)

      status(result) shouldBe OK
    }

    "select divorce year for update relationship" in {
      val transferorRecipientData = Some(UpdateRelationshipCacheData(None, None,
        Some(RelationshipRecord(Role.RECIPIENT, "1234567890", "20150101", None, None, Role.TRANSFEROR, "1234567890")),
        None, None, None, None))

      val testComponent = makeUpdateRelationshipTestComponent(dataId = "coc_active_relationship", transferorRecipientData = transferorRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(
        "role" -> Role.RECIPIENT,
        "endReason" -> EndReasonCode.DIVORCE_PY,
        "historicActiveRecord" -> "false",
        "creationTimestamp" -> "1234567890",
        "dateOfDivorce.day" -> "20",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2015")

      val result = controllerToTest.divorceSelectYear()(request)
      status(result) shouldBe OK
    }

  }

  "Calling history page" should {

    "show 'Cancel Marriage Allowance' button on ’History’ page with PTA journey" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }

    "show 'Cancel Marriage Allowance' button on ’History’ page with GDS journey" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }

    "show sign-out on ’History’ page along with message" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val article = document.getElementsByTag("article")
      article.text().contains("Marriage Allowance Foo Bar") shouldBe true
    }
  }
}
