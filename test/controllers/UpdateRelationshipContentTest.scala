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

import models.{ EndReasonCode, Role }
import org.jsoup.Jsoup
import play.api.test.Helpers.{ OK, SEE_OTHER, contentAsString, defaultAwaitTimeout }
import play.api.test.WithApplication
import test_utils.UpdateRelationshipTestUtility
import uk.gov.hmrc.play.test.UnitSpec
import play.api.mvc.Cookie
import play.api.test.Helpers.redirectLocation
import models.UserRecord
import test_utils.TestConstants
import models.CitizenName
import details.PersonDetails
import details.Person
import org.joda.time.LocalDate
import org.joda.time.DateTimeZone
import org.joda.time.DateTime

class UpdateRelationshipContentTest extends UnitSpec with UpdateRelationshipTestUtility {

  "list relationship page " should {

   "display signout for PTA" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Test_name, this is the first time you have logged in"
    }

    "display signout for PTA when CitizenDetails are in cache" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", cachePd = Some(PersonDetails(Person(Some("cached_name")))))
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
      document.getElementById("user-status").getElementsByTag("p").text() shouldBe "Cached_name, this is the first time you have logged in"
    }

    "display signout for GDS" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      document.getElementById("sign-out").attr("href") shouldBe "/marriage-allowance-application/logout"
    }

    "display only active relationship details " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val activeRecord = document.getElementById("activeRecord")
      activeRecord shouldNot be(null)

      val historicRecord = document.getElementById("historicRecord")
      historicRecord should be(null)
    }

    "display only historic relationship details and link to how-it-works" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val activeRecord = document.getElementById("activeRecord")
      activeRecord should be(null)

      val start = document.getElementById("start-now")
      start shouldNot be(null)
      start.attr("href") shouldBe "/marriage-allowance-application/how-it-works"

      val historicRecord = document.getElementById("historicRecords")
      historicRecord shouldNot be(null)

      document.getElementById("line0-start").text shouldBe "30-12-2001"
      document.getElementById("line0-end").text shouldBe "30-12-2010"
      document.getElementById("line0-action").text shouldBe "Received Marriage Allowance"
      document.getElementById("line0-reason").text shouldBe "Bereavement"
      document.getElementById("line0-remove") shouldBe null
    }

    "display reject button when it should be displayed" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historic_rejectable_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val activeRecord = document.getElementById("activeRecord")
      activeRecord should be(null)

      val start = document.getElementById("start-now")
      start shouldNot be(null)
      start.attr("href") shouldBe "/marriage-allowance-application/how-it-works"

      val historicRecord = document.getElementById("historicRecords")
      historicRecord shouldNot be(null)

      document.getElementById("line0-start").text shouldBe "30-12-2013"
      document.getElementById("line0-end").text shouldBe "30-12-2014"
      document.getElementById("line0-action").text shouldBe "Transferred Marriage Allowance"
      document.getElementById("line0-reason").text shouldBe "Divorce or end of Civil Partnership"
      document.getElementById("line0-remove") shouldBe null

      document.getElementById("line1-start").text shouldBe "30-12-2002"
      document.getElementById("line1-end").text shouldBe "30-12-2012"
      document.getElementById("line1-action").text shouldBe "Received Marriage Allowance"
      document.getElementById("line1-reason").text shouldBe "Divorce or end of Civil Partnership"
      document.getElementById("line1-remove") shouldNot be(null)
    }
    
    "don't display apply for previous years button when previous years are available" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historic_rejectable_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val prevYearsButton = document.getElementById("previousYearsApply")
      prevYearsButton shouldNot be(null)
    }

    "display 'apply for previous years' button if historicyear is available" in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_gap_in_years")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val prevYearsButton = document.getElementById("previousYearsApply")
      prevYearsButton shouldNot be(null)
    }

    "display apply for previous years button when previous years are available" in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val prevYearsButton = document.getElementById("previousYearsApply")
      prevYearsButton should be(null)
    }

    "display active and historic relationship details " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val activeRecord = document.getElementById("activeRecord")
      activeRecord shouldNot be(null)

      val historicRecord = document.getElementById("historicRecords")
      historicRecord shouldNot be(null)

      historicRecord.toString().contains("30-12-2001") should be(true)
      historicRecord.toString().contains("30-12-2010") should be(true)
    }

    "not display active or historic relationship details " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_no_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe SEE_OTHER
      val document = Jsoup.parse(contentAsString(result))

      controllerToTest.cachingTransferorRecordToTestCount shouldBe 1
      controllerToTest.cachingTransferorRecordToTest shouldBe Some(UserRecord(cid = 999700100, timestamp = "2015", name = Some(CitizenName(Some("Foo"), Some("Bar")))))
      controllerToTest.cachingRecipientRecordToTestCount shouldBe 0
      controllerToTest.cachingRecipientRecordToTest shouldBe None

      val activeRecord = document.getElementById("activeRecord")
      activeRecord should be(null)

      val historicRecord = document.getElementById("historicRecords")
      historicRecord should be(null)
    }

    "display historical active relationship details " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val historicActiveMessage = document.getElementById("historicActiveMessage").text()
      historicActiveMessage should be("You'll stop receiving Marriage Allowance from your spouse or civil partner at end of the tax year (5 April 2017).")

      val historicRecord = document.getElementById("historicRecords")
      historicRecord shouldNot be(null)
    }
  }

  "Update relationship make changes page " should {

    "show transferor data when user is trasnferor " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonCancel = endReason.getElementById("endReason-cancel")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")
      val endReasonEarnings = endReason.getElementById("endReason-earnings")

      endReasonCancel shouldNot be(null)
      endReasonDivorce shouldNot be(null)
      endReasonEarnings shouldNot be(null)

      endReasonCancel.toString.contains(EndReasonCode.CANCEL) should be(true)
      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonEarnings.toString.contains(EndReasonCode.EARNINGS) should be(true)
    }

    "show transferor data when user is transferor " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonReject = endReason.getElementById("endReason-reject")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")
      val endReasonEarnings = endReason.getElementById("endReason-earnings")

      endReasonReject shouldNot be(null)
      endReasonDivorce shouldNot be(null)
      endReasonEarnings shouldNot be(null)

      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonEarnings.toString.contains(EndReasonCode.EARNINGS) should be(true)
    }
  }

  "Update relationship confirmation page " should {

    "confirm cancellation " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmCancel()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val cancelHeading = document.getElementById("cancel-heading")
      val cancelContent = document.getElementById("cancel-content")

      cancelHeading shouldNot be(null)
      cancelContent shouldNot be(null)

      cancelHeading.toString contains ("Cancelling Marriage Allowance") should be(true)
      cancelContent.text() shouldBe "Marriage Allowance will be cancelled, but will remain in place until the end of the current tax year (5 April 2017). Your Personal Allowance will be adjusted at the start of the new tax year on 6 April 2017."

    }
    
    "confirm cancellation with future date" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship",
          testingTime = new DateTime(2017, 1, 1, 0, 0, DateTimeZone.forID("Europe/London")))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmCancel()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val cancelHeading = document.getElementById("cancel-heading")
      val cancelContent = document.getElementById("cancel-content")

      cancelHeading shouldNot be(null)
      cancelContent shouldNot be(null)

      cancelHeading.toString contains ("Cancelling Marriage Allowance") should be(true)
      cancelContent.text() shouldBe "Marriage Allowance will be cancelled, but will remain in place until the end of the current tax year (5 April 2017). Your Personal Allowance will be adjusted at the start of the new tax year on 6 April 2017."

    }

  }

  "Confirm your selection " should {

    "confirm email after divorce action (PY) " in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_PY)
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")

    }

    "confirm email after divorce action (CY)" in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_CY)
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")
    }
  }

  "Update relationship make changes" should {

    "return successful on histrocial active record for transferror" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "true")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonCancel = endReason.getElementById("endReason-cancel")
      val endReasonEarnings = endReason.getElementById("endReason-earnings")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")
      val endReasonBereavement = endReason.getElementById("endReason-bereavement")

      endReasonCancel should be(null)
      endReasonEarnings shouldNot be(null)
      endReasonDivorce shouldNot be(null)
      endReasonBereavement shouldNot be(null)

      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonEarnings.toString.contains(EndReasonCode.EARNINGS) should be(true)
      endReasonBereavement.toString.contains(EndReasonCode.BEREAVEMENT) should be(true)
    }

    "return successful on non historically active record for transferror" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonCancel = endReason.getElementById("endReason-cancel")
      val endReasonEarnings = endReason.getElementById("endReason-earnings")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")
      val endReasonBereavement = endReason.getElementById("endReason-bereavement")

      endReasonCancel shouldNot be(null)
      endReasonEarnings shouldNot be(null)
      endReasonDivorce shouldNot be(null)
      endReasonBereavement shouldNot be(null)

      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonCancel.toString.contains(EndReasonCode.CANCEL) should be(true)
      endReasonEarnings.toString.contains(EndReasonCode.EARNINGS) should be(true)
      endReasonBereavement.toString.contains(EndReasonCode.BEREAVEMENT) should be(true)
    }

    "return successful on histrocial active record for recipient" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "true")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonReject = endReason.getElementById("endReason-reject")
      val endReasonEarnings = endReason.getElementById("endReason-earnings")
      val endReasonBereavement = endReason.getElementById("endReason-bereavement")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")

      endReasonDivorce should be(null)
      endReasonReject shouldNot be(null)
      endReasonEarnings shouldNot be(null)
      endReasonBereavement shouldNot be(null)

      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
      endReasonEarnings.toString.contains(EndReasonCode.EARNINGS) should be(true)
      endReasonBereavement.toString.contains(EndReasonCode.BEREAVEMENT) should be(true)
    }

    "return successful on non histrocial active record for recipient" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonReject = endReason.getElementById("endReason-reject")
      val endReasonEarnings = endReason.getElementById("endReason-earnings")
      val endReasonBereavement = endReason.getElementById("endReason-bereavement")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")

      endReasonDivorce shouldNot be(null)
      endReasonReject shouldNot be(null)
      endReasonEarnings shouldNot be(null)
      endReasonBereavement shouldNot be(null)

      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
      endReasonEarnings.toString.contains(EndReasonCode.EARNINGS) should be(true)
      endReasonBereavement.toString.contains(EndReasonCode.BEREAVEMENT) should be(true)
    }
  }
}
