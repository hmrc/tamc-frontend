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
  
  "Hitting language selection endpoint on GDS journey verify page" should {
    "redirect to Welsh translated start page if Welsh language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToWelshVerify(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=cy-GB; Path=/; HTTPOnly")
    }
    "redirect to English translated start page if English language is selected" in new WithApplication(FakeApplication()) {
      val request = FakeRequest()
      val result = Future.successful(controllers.LanguageController.switchToEnglishVerify(request))
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/verify")
      header("Set-Cookie", result) shouldBe Some("PLAY_LANG=en-GB; Path=/; HTTPOnly")
    }    
  }    
}