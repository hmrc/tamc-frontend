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

import uk.gov.hmrc.play.test.UnitSpec
import test_utils.TestUtility
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers.header
import play.api.test.Helpers.redirectLocation
import scala.concurrent.Future
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.WithApplication

class LanguageControllerTest extends UnitSpec with TestUtility {

  "Hitting language selection endpoint on GDS journey eligibility page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshEligibilityCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishEligibilityCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on GDS journey date of birth page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshDateOfBirthCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-birth-check")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishDateOfBirthCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-birth-check")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on GDS journey lower earner page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshLowerEarnerCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/lower-earner")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishLowerEarnerCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/lower-earner")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on GDS journey partner's income page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshPartnersIncomeCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/partners-income")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishPartnersIncomeCheck(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/partners-income")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on history/status page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshHistory(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishHistory(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/history")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting change of income page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshIncomeChange(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/change-of-income")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishIncomeChange(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/change-of-income")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting bereavement page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshBereavement(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/bereavement")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishBereavement(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/bereavement")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on GDS journey date of marriage page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshDateOfMarriage(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-marriage")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishDateOfMarriage(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-marriage")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on change of circs confirm email page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshConfirmEmail(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishConfirmEmail(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-email")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on change of circs confirm update page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshConfirmUpdate(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-change")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishConfirmUpdate(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-change")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on change of circs cancel page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshConfirmCancel(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/cancel")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishConfirmCancel(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/cancel")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on change of circs reject page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshConfirmReject(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/reject")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishConfirmReject(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/reject")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on change of circs finish page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshFinishUpdate(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishFinishUpdate(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on transfer page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshTransfer(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/transfer-allowance")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishTransfer(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/transfer-allowance")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on eligible years page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshEligibleYears(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligible-years")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishEligibleYears(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligible-years")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on previous years page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshPreviousYears(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/previous-years")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishPreviousYears(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/previous-years")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on confirm your email page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshConfirmYourEmail(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-your-email")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishConfirmYourEmail(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-your-email")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on final confirm page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshConfirm(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishConfirm(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on finished page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshFinished(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishFinished(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on PTA journey how it works page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshHowItWorks(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/how-it-works")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishHowItWorks(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/how-it-works")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on PTA journey eligibility page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshEligibilityCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishEligibilityCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/eligibility-check-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }


  "Hitting language selection endpoint on PTA journey date of birth page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshDateOfBirthCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-birth-check-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishDateOfBirthCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/date-of-birth-check-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on PTA journey lower earner page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshLowerEarnerCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/lower-earner-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishLowerEarnerCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/lower-earner-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on PTA journey partner's income page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshPartnersIncomeCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/partners-income-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishPartnersIncomeCheckPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/partners-income-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on GDS journey benefit calculator page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshCalculator(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/benefit-calculator")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishCalculator(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/benefit-calculator")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }

  "Hitting language selection endpoint on PTA journey benefit calculator page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshCalculatorPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/benefit-calculator-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishCalculatorPta(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/benefit-calculator-pta")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }
  }
}
