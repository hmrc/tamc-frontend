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

import config.ApplicationConfig._
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, contentAsString, defaultAwaitTimeout}
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

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
      when(mockTransferService.getConfirmationData(any())(any(), any()))
        .thenReturn(ConfirmationModel(None, EmailAddress("example@example.com"), "", "", Nino(Ninos.nino1), Nil, DateOfMarriageFormInput(LocalDate.now())))
      val result = transferController.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("create").text() shouldBe "Confirm your application"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.TransferController.confirmYourEmail().url
    }


    "have 'Confirm your application' " in {
      val result = transferController.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("create").text() shouldBe "Confirm your application"
    }

    "have link to edit email page" in {
      val result = transferController.confirm(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val signout = document.getElementById("edit-email")
      signout shouldNot be(null)
      signout.attr("href") shouldBe "/marriage-allowance-application/confirm-your-email"
    }
    "have link to edit partner details and edit marriage details" in {
      val result = transferController.confirm(request)

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
    "have check your marriage allowance link" in {
      when(mockTransferService.getFinishedData(any())(any(), any()))
        .thenReturn(NotificationRecord(EmailAddress("example@example.com")))

      val result = transferController.finished(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))

      val ptaLink = document.getElementById("paragraph-5")
      ptaLink shouldNot be(null)
      ptaLink.getElementById("pta-link").attr("href") shouldBe "/personal-account"
    }
  }

  "PTA Eligibility check page for multi year" should {
    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING

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
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"
    }
  }

  "PTA date of birth check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.dateOfBirthCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe "Were you and your partner born after 5 April 1935? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.EligibilityController.eligibilityCheck().url
    }
  }

  "PTA partners income check page for multi year" should {

    "display errors as no radio buttons is selected for English resident" in {
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE() + 1)
      val higherThreshold = formatter.format(MAX_LIMIT())
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe s"Confirm if your partner has an annual income of between £$lowerThreshold and £$higherThreshold"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.EligibilityController.lowerEarnerCheck().url
    }

    "display errors as no radio buttons is selected for Scottish resident" in {
      val request = FakeRequest().withSession("scottish_resident" -> "true")
      val formatter = java.text.NumberFormat.getIntegerInstance
      val lowerThreshold = formatter.format(PERSONAL_ALLOWANCE() + 1)
      val higherScotThreshold = formatter.format(MAX_LIMIT_SCOT())
      val result = eligibilityController.partnersIncomeCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.title() shouldBe s"Is your partner’s income between £$lowerThreshold and £$higherScotThreshold a year? - Marriage Allowance eligibility - GOV.UK"
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING
      document.getElementById("partners-income-error").text() shouldBe s"Confirm if your partner has an annual income of between £$lowerThreshold and £$higherScotThreshold"
      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe controllers.routes.EligibilityController.lowerEarnerCheck().url
    }
  }

  "GDS Eligibility check page for multi year" should {

    "diplay errors as no radio buttons is selected " in {
      val result = eligibilityController.eligibilityCheckAction()(request)
      status(result) shouldBe BAD_REQUEST

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING

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
      document.getElementById("form-error-heading").text() shouldBe ERROR_HEADING

      document.getElementById("marriage-criteria-error").text() shouldBe "Confirm if you are married or in a legally registered civil partnership"

      val back = document.getElementsByClass("link-back")
      back shouldNot be(null)
      back.attr("href") shouldBe "https://www.gov.uk/apply-marriage-allowance"
    }
  }

  val ERROR_HEADING = "There is a problem"

  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]
  def eligibilityController: EligibilityController = instanceOf[EligibilityController]
  def transferController: TransferController = new TransferController(
    messagesApi,
    instanceOf[AuthenticatedActionRefiner],
    mockTransferService,
    mockCachingService,
    mockTimeService
  )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])
}
