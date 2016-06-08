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
import play.api.test.Helpers.{ BAD_REQUEST, contentAsString, defaultAwaitTimeout }
import play.api.test.WithApplication
import test_utils.TestData.{Ninos, Cids}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec
import models._
import test_utils.{ UpdateRelationshipTestUtility, TestConstants }

class UpdateRelationshipErrorTest extends UnitSpec with UpdateRelationshipTestUtility {

  "List relationship" should {

    "return an error if citizen not found" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_citizen_not_found")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.history()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))

      val heading = document.getElementsByClass("heading-large").text()
      val error = document.getElementById("error").text()

      heading should be("We cannot find your Marriage Allowance details")
      error should be("Call us to make a change to your Marriage Allowance. Have your National Insurance number ready when you call")

    }

    "return an error on bad request" in new WithApplication(fakeApplication) {

      val testComponent = makeUpdateRelationshipTestComponent("coc_bad_request")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("role" -> Role.TRANSFEROR, "historicActiveRecord" -> "false")
      val result = controllerToTest.history()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))

      val heading = document.getElementsByClass("heading-large").text()
      val error = document.getElementById("error").text()

      heading should be("There has been a technical error")
      error should be("Try again. If the problem persists, call us to make a change to your Marriage Allowance. Have your National Insurance number ready when you call.")

    }
  }

  "Update relationship" should {

    "return an error if citizen not found" in new WithApplication(fakeApplication) {

      val loggedInUser = LoggedInUserInfo(999700101, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_citizen_not_found", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))

      val heading = document.getElementsByClass("heading-large").text()
      val error = document.getElementById("error").text()

      heading should be("We cannot find your Marriage Allowance details")
      error should be("Call us to make a change to your Marriage Allowance. Have your National Insurance number ready when you call")

    }

    "return an error on bad request" in new WithApplication(fakeApplication) {

      val loggedInUser = LoggedInUserInfo(999700102, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_bad_request", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))

      val heading = document.getElementsByClass("heading-large").text()
      val error = document.getElementById("error").text()

      heading should be("There has been a technical error")
      error should be("Try again. If the problem persists, call us to make a change to your Marriage Allowance. Have your National Insurance number ready when you call.")

    }
  }
}
