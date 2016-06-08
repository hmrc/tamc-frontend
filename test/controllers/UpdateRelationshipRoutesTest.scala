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

import org.joda.time.LocalDate
import org.jsoup.Jsoup
import play.api.test.Helpers.{ OK, SEE_OTHER, BAD_REQUEST, contentAsString, defaultAwaitTimeout, redirectLocation }
import play.api.test.WithApplication
import test_utils.TestData.{Ninos, Cids}
import test_utils.{TestConstants, UpdateRelationshipTestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.WithApplication
import models._

class UpdateRelationshipRoutesTest extends UnitSpec with UpdateRelationshipTestUtility {

  "call list relationship " should {

    "get redirected to how it works page when no record exist " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_no_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/how-it-works"))
    }

    "list the relationship on the same page " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
    }

  }

  "Update relationship action " should {

    "show error if no option is selected " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "redirect to cancel page when user selects cancel option " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.CANCEL)
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/cancel"))
    }

    "redirect to reject page when user selects reject option " in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.REJECT)
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("REJECT"))
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/reject"))
    }

    "redirect to reject page when user selects reject option and save timestamp " in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.REJECT, "creationTimestamp" -> "1234567890")
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason(endReason = "REJECT", timestamp = Some("1234567890")))
      redirectLocation(result) shouldBe Some(marriageAllowanceUrl("/reject"))
    }

    "display user divorce page when user selects divorce option " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "endReason" -> EndReasonCode.DIVORCE, "historicActiveRecord" -> "false")
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe OK
    }

    "display user earnings changed page when user selects divorce option " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.EARNINGS)
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe OK
    }

    "show bereavement for recipient" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.BEREAVEMENT, "role" -> Role.RECIPIENT)
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val sorry = document.getElementById("bereavement-recipient") shouldNot be(null)
    }

    "show bereavement for transferor" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.BEREAVEMENT, "role" -> Role.TRANSFEROR)
      val result = controllerToTest.updateRelationshipAction()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val sorry = document.getElementById("bereavement-transferor") shouldNot be(null)
    }

  }

  "Confirm your selection " should {

    "show error if no reason is provided" in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false", "dateOfDivorce.day" -> "1", "dateOfDivorce.month" -> "1", "dateOfDivorce.year" -> "2015")
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "confirm divorce action for previous year " in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_PY)
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")
    }

    "confirm divorce action for current year " in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_CY)
      val result = controllerToTest.divorceAction()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")
    }

    "confirm cancellation " in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmCancel()(request)
      status(result) shouldBe OK
    }
  }

  "Date of divorce page" should {
    "display error entering no date on date of divorce page" in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(
        "role" -> "Transferor",
        "endReason" -> EndReasonCode.DIVORCE_PY,
        "dateOfDivorce.day" -> "",
        "dateOfDivorce.month" -> "",
        "dateOfDivorce.year" -> "")
      val result = controllerToTest.divorceSelectYear()(request)

      status(result) shouldBe BAD_REQUEST
    }

    "display divorce info page if correct but invalid date entered" in new WithApplication(fakeApplication) {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(
        "role" -> Role.TRANSFEROR,
        "endReason" -> EndReasonCode.DIVORCE_CY,
        "historicActiveRecord" -> "true",
        "dateOfDivorce.day" -> "1",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2016")
      val result = controllerToTest.divorceSelectYear()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val hiddenDay = document.getElementsByAttributeValue("name", "dateOfDivorce.day") shouldNot be(null)
    }
  }

  "Update relationship make changes" should {

    "return successful on active record for transferror" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK
    }

    "return successful on active record for recipient " in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "false")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK
    }

    "return successful on histrocial active record for transferror" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "true")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK
    }

    "return successful redirection on histrocial active record for recipient" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_historically_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.RECIPIENT, "historicActiveRecord" -> "true")
      val result = controllerToTest.makeChange()(request)
      status(result) shouldBe OK
    }
  }

}
