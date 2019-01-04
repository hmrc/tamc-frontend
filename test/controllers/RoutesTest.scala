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

import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, cookies, defaultAwaitTimeout, redirectLocation}
import test_utils.TestData.{Cids, Ninos}
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.test.UnitSpec

class RoutesTest extends ControllerBaseSpec {

  "Hitting calculator page" should {
    "have a ’previous’ and ’next’ links to gov.ukpage" in {
      val result = eligibilityController.gdsCalculator()(request)
      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      val previous = document.getElementById("previous")
      previous shouldNot be(null)
      previous.attr("href") shouldBe "https://www.gov.uk/marriage-allowance-guide/how-it-works"
      val next = document.getElementById("next")
      next shouldNot be(null)
      next.attr("href") shouldBe "https://www.gov.uk/marriage-allowance/how-to-apply"
    }
  }

  "Confirmation page" should {
    "have correct action and method to finish page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("create").text() shouldBe "Confirm your application"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/confirm-your-email")
    }


    "have 'Confirm your application' " in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("create").text() shouldBe "Confirm your application"
    }

    "have link to edit email page" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("edit-email")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/marriage-allowance-application/confirm-your-email"
    }
    "have link to edit partner details and edit marriage details" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015", name = None)
      val rcrec = UserRecord(cid = Cids.cid5, timestamp = "2015", name = None)
      val rcdata = RegistrationFormInput("foo", "bar", Gender("F"), Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val selectedYears = Some(List(2014, 2015))
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))), selectedYears = selectedYears, dateOfMarriage = Some(DateOfMarriageFormInput(new LocalDate(2015, 1, 1)))))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val changeLink = document.getElementById("edit-partner-details")
      val marriageLink = document.getElementById("edit-marriage-date")
      changeLink shouldNot be(null)
      marriageLink shouldNot be(null)
      changeLink.attr("href") shouldBe "/marriage-allowance-application/transfer-allowance"
      marriageLink.attr("href") shouldBe "/marriage-allowance-application/date-of-marriage"
    }
  }

  "Finished page" should {

    "have check your marriage allowance link for non-PTA journey" in {
      val trrec = UserRecord(cid = Cids.cid1, timestamp = "2015")
      val rcrec = UserRecord(cid = Cids.cid2, timestamp = "2015")
      val rcdata = RegistrationFormInput(name = "foo", lastName = "bar", gender = Gender("M"), nino = Nino(Ninos.ninoWithLOA1), dateOfMarriage = new LocalDate(2015, 1, 1))
      val recrecord = RecipientRecord(record = rcrec, data = rcdata)
      val trRecipientData = Some(CacheData(transferor = Some(trrec), recipient = Some(recrecord), notification = Some(NotificationRecord(EmailAddress("example123@example.com"))), relationshipCreated = Some(true)))

      val testComponent = makeTestComponent("user_happy_path", transferorRecipientData = trRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val ptaLink = document.getElementById("paragraph-5")
      ptaLink shouldNot be(null)
      ptaLink.getElementById("pta-link").attr("href") shouldBe "https://www.gov.uk/personal-tax-account"
    }

    "have check your marriage allowance link for PTA journey" in {
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

      val ptaLink = document.getElementById("paragraph-5")
      ptaLink shouldNot be(null)
      ptaLink.getElementById("pta-link").attr("href") shouldBe "https://www.gov.uk/personal-tax-account"
    }
  }

  "PTA Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification") shouldNot be(null)
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Tell us if you are married or in a legally registered civil partnership"
    }

    "diplay errors as wrong input is provided by selected radio button" in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"
    }
  }

  "PTA date of birth check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.dateOfBirthCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/eligibility-check-pta")
    }
  }

  "PTA partners income check page for multi year" should {

    "display errors as no radio buttons is selected for English resident" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE + 1)
      val higherThreshold = formatter.format(ApplicationConfig.MAX_LIMIT)
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe s"Confirm if your partner has an annual income of between £$lowerThreshold and £$higherThreshold"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner-pta")
    }

    "display errors as no radio buttons is selected for Scottish resident" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(ApplicationConfig.PERSONAL_ALLOWANCE + 1)
      val higherScotThreshold = formatter.format(ApplicationConfig.MAX_LIMIT_SCOT)
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherScotThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe s"Confirm if your partner has an annual income of between £$lowerThreshold and £$higherScotThreshold"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe marriageAllowanceUrl("/lower-earner-pta")
    }
  }

  "GDS Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val form = document.getElementById("eligibility-form")
      val marriageFieldset = form.select("fieldset[id=marriage-criteria]").first()
      marriageFieldset.getElementsByClass("error-notification") shouldNot be(null)
      marriageFieldset.getElementsByClass("error-notification").text() shouldBe "Tell us if you are married or in a legally registered civil partnership"
    }

    "diplay errors as wrong input is provided by selected radio button" in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Are you married or in a civil partnership? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe TestConstants.ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe ("https://www.gov.uk/apply-marriage-allowance")
    }
  }

  def eligibilityController: EligibilityController = app.injector.instanceOf[EligibilityController]
}
