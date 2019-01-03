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

import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, defaultAwaitTimeout, redirectLocation}
import test_utils.TestData.Cids
import test_utils.{TestConstants, UpdateRelationshipTestUtility}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.time.TaxYearResolver
import views.helpers.TextGenerators

class UpdateRelationshipRoutesTest extends UpdateRelationshipTestUtility {

  "call list relationship " should {

    "get redirected to how it works page when no record exist " in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_no_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/how-it-works"))
    }

    "list the relationship on the same page " in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
    }

  }

  "Confirm your selection " should {

    "confirm rejection in ended relationship in previous year" in {
      val loggedInUser = LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015", Some(false), TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "56787", "20160406", Some(""), Some("20170405"), "", "")

      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.RECIPIENT),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.REJECT)),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmReject()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val heading = document.getElementsByClass("heading-xlarge").text()
      val message = document.getElementById("reject-content").text()
      heading should be("Remove a previous Marriage Allowance claim")
      message should be("You can remove the Marriage Allowance you claimed previously. The allowance will be removed from 6 April 2016, the start of the tax year you first received it.")
    }

    "confirm cancelling in ended relationship in active year" in {
      val loggedInUser = LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015", Some(false), TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "56787", "20170406", Some(""), Some("20180405"), "", "")

      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.TRANSFEROR),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmCancel()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
     val heading = document.getElementsByClass("heading-xlarge").text()
     val message = document.getElementById("cancel-content").text()
     heading should be("Cancelling Marriage Allowance")
      message should be("We will cancel your Marriage Allowance, but it will remain in place until 5 April 2017, the end of the current tax year.")
    }

    "confirm rejection in active year" in {
      val loggedInUser = LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015", Some(false), TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "56787", "20150406", Some(""), Some(""), "", "")

      val endOfCurrentTaxYear = TextGenerators.ukDateTransformer(Some(TaxYearResolver.endOfCurrentTaxYear), false)

      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.RECIPIENT),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.REJECT)),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmReject()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val heading = document.getElementsByClass("heading-xlarge").text()
      val message = document.getElementById("reject-content").text()
      val message2 = document.getElementById("reject-partner").text()
      heading should be("Cancelling Marriage Allowance")

      message2 should be(s"If your partner cancels your Marriage Allowance, it will remain in place until $endOfCurrentTaxYear, the end of the current tax year.")
      message should be("You can only cancel your Marriage Allowance from the start of the claim, 6 April 2015, the start of the tax year you first received it.")
    }

    "confirm rejection with ended relationship in previous year" in {
      val loggedInUser = LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015", Some(false), TestConstants.GENERIC_CITIZEN_NAME)
      val historic1Record = RelationshipRecord(Role.RECIPIENT, "2015", "20120101", Some(""), Some("1-01-2013"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.RECIPIENT),
        historicRelationships = Some(Seq(historic1Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.REJECT, timestamp = Some("2015"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_historic_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmReject()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val heading = document.getElementsByClass("heading-xlarge").text()
      val message = document.getElementById("reject-content").text()
      heading should be("Remove a previous Marriage Allowance claim")
      message should be("You can remove the Marriage Allowance you claimed previously. The allowance will be removed from 6 April 2011, the start of the tax year you first received it.")
    }

    "changing divorce year using change button on confirmation page" in {
      val loggedInUser = LoggedInUserInfo(cid = Cids.cid1, timestamp = "2015", Some(false), TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "56787", "20130101", Some(""), Some("20130110"), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "20120101", Some(""), Some("1-01-2013"), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20140101", Some(""), Some("1-01-2015"), "", "")

      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.TRANSFEROR),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.DIVORCE_CY)),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.divorceYear()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val heading = document.getElementsByClass("heading-xlarge").text()
      heading should be("Date of divorce or end of civil partnership")
    }

    "confirm divorce action for recipient on current year " in {
      val currentTaxYear = TaxYearResolver.currentTaxYear
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "20130101", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.RECIPIENT),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = Some(new LocalDate(currentTaxYear, 6, 12)), timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("confirm-page").text() shouldBe "Confirm cancellation of Marriage Allowance"
      val message1 = document.getElementById("message1").text()
      val message2 = document.getElementById("message2").text()
      message1 should be(s"your Marriage Allowance will remain in place until 5 April ${currentTaxYear+1}, the end of the current tax year")
      message2 should be(s"your Personal Allowance will go back to the normal amount from 6 April ${currentTaxYear+1}, the start of the new tax year")
    }

  }
}
