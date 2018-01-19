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

import details.{Person, PersonDetails}
import models.{CitizenName, EndReasonCode, Role, UserRecord}
import org.joda.time.{DateTime, DateTimeZone}
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.mvc.Cookie
import play.api.test.Helpers.{OK, SEE_OTHER, contentAsString, defaultAwaitTimeout, redirectLocation}
import test_utils.UpdateRelationshipTestUtility
import uk.gov.hmrc.play.test.UnitSpec

class UpdateRelationshipContentTest extends UnitSpec with UpdateRelationshipTestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

  "list relationship page" should {

    "display 'Cancel Marriage Allowance' button" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }

    "display 'Cancel Marriage Allowance' button for PTA when CitizenDetails are in cache" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", cachePd = Some(PersonDetails(Person(Some("cached_name")))))
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }

    "display 'Cancel Marriage Allowance' button for GDS" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }

    "display only active relationship details" in {

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

    "display only historic relationship details and link to how-it-works" in {

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

      document.getElementById("line0-start").text shouldBe "2001 to 2011"
      document.getElementById("line0-reason").text shouldBe "Bereavement"
      document.getElementById("line0-remove") shouldBe null
    }

    "display reject button when it should be displayed" in {

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

      document.getElementById("line0-start").text shouldBe "2013 to 2015"
      document.getElementById("line0-reason").text shouldBe "Divorce or end of civil partnership"
      document.getElementById("line0-remove") shouldBe null

      document.getElementById("line1-start").text shouldBe "2002 to 2013"
      document.getElementById("line1-reason").text shouldBe "Divorce or end of civil partnership"
      document.getElementById("line1-remove") shouldNot be(null)
    }

    "don’t display apply for previous years button when previous years are available" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historic_rejectable_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val prevYearsButton = document.getElementById("previousYearsApply")
      prevYearsButton shouldNot be(null)
    }

    "display ’apply for previous years’ button if historic year is available" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_gap_in_years")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val prevYearsButton = document.getElementById("previousYearsApply")
      prevYearsButton shouldNot be(null)
    }

    "display apply for previous years button when previous years are available" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val prevYearsButton = document.getElementById("previousYearsApply")
      prevYearsButton should be(null)
    }

    "display active and historic relationship details " in {

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

      document.getElementById("active").text shouldBe "2001 to Present"
      historicRecord.toString().contains("2001 to 2011") should be(true)
    }

    "not display active or historic relationship details" in {

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

    "display historical active relationship details" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val contentAsStringFromResult = contentAsString(result)
      val document = Jsoup.parse(contentAsString(result))
      val historicActiveMessage = document.getElementById("historicActiveMessage").text()
      historicActiveMessage should be("You will stop receiving Marriage Allowance from your partner at end of the tax year (5 April 2017).")

      val historicRecord = document.getElementById("historicRecords")
      historicRecord shouldNot be(null)
    }

    "display bereavement and change of income related details" in {

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

      historicRecord.toString().contains("2001 to 2011") should be(true)

      val incomeMessage = document.getElementById("incomeMessage")
      val bereavementMessage = document.getElementById("bereavementMessage")
      val incomeLink = document.getElementById("incomeLink")
      val bereavementLink = document.getElementById("bereavementLink")
      incomeMessage.text() shouldBe "To let us know about a change in income, contact HMRC."
      bereavementMessage.text() shouldBe "To let us know about a bereavement, contact HMRC."
      incomeLink.attr("href") shouldBe "/marriage-allowance-application/change-of-income"
      bereavementLink.attr("href") shouldBe "/marriage-allowance-application/bereavement"
    }
  }

  "Update relationship make changes page" should {

    "show transferor data when user is transferor" in {

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


      endReasonCancel shouldNot be(null)
      endReasonDivorce shouldNot be(null)


      endReasonCancel.toString.contains(EndReasonCode.CANCEL) should be(true)
      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)

    }

    "show transferor data when user is recipient" in {

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

      endReasonReject shouldNot be(null)
      endReasonDivorce shouldNot be(null)

      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
    }
  }

  "Update relationship confirmation page" should {

    "confirm cancellation " in {

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
      cancelContent.text() shouldBe "We will cancel your Marriage Allowance, but it will remain in place until 5 April 2017, the end of the current tax year."

    }

    "confirm cancellation with future date" in {

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
      cancelContent.text() shouldBe "We will cancel your Marriage Allowance, but it will remain in place until 5 April 2017, the end of the current tax year."

    }

  }

  "Confirm your selection" should {

    "confirm email after divorce action (PY) " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_PY)
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")

    }

    "confirm email after divorce action (CY)" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_CY)
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")
    }
  }

  "Update relationship make changes" should {

    "return successful on historical active record for transferror" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "true")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonCancel = endReason.getElementById("endReason-cancel")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")

      endReasonCancel should be(null)
      endReasonDivorce shouldNot be(null)

      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
    }

    "return successful on non historically active record for transferror" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonCancel = endReason.getElementById("endReason-cancel")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")

      endReasonCancel shouldNot be(null)
      endReasonDivorce shouldNot be(null)

      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonCancel.toString.contains(EndReasonCode.CANCEL) should be(true)
    }

    "return successful on historical active record for recipient" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "true")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonReject = endReason.getElementById("endReason-reject")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")

      endReasonDivorce should be(null)
      endReasonReject shouldNot be(null)

      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
    }

    "return successful on non historical active record for recipient" in {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))

      val endReason = document.getElementById("endReason")
      endReason shouldNot be(null)

      val endReasonReject = endReason.getElementById("endReason-reject")
      val endReasonDivorce = endReason.getElementById("endReason-divorce")

      endReasonDivorce shouldNot be(null)
      endReasonReject shouldNot be(null)

      endReasonDivorce.toString.contains(EndReasonCode.DIVORCE) should be(true)
      endReasonReject.toString.contains(EndReasonCode.REJECT) should be(true)
    }
  }
}
