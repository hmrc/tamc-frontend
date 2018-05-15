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

import org.scalatest.Ignore
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.{cookies, defaultAwaitTimeout, header, redirectLocation}
import test_utils.TestUtility
import uk.gov.hmrc.play.test.UnitSpec
import play.api.test.Helpers._

import scala.concurrent.Future

class LanguageControllerTest extends UnitSpec with TestUtility with OneAppPerSuite {

  override implicit lazy val app: Application = fakeApplication


  trait LocalSetup {
    val c = app.injector.instanceOf[LanguageController]
  }

  "Calling LanguageController.enGb" should {
    "change the language to English and return 303" in new LocalSetup {
      val req = FakeRequest("GET", "test")
      val r = c.enGb("/test")(req)
      cookies(r).get("PLAY_LANG").get.value shouldBe "en"
      status(r) shouldBe SEE_OTHER
    }
  }

  "Calling LanguageController.cyGb" should {
    "change the language to Welsh and return 303" in new LocalSetup {
      val r = c.cyGb("/test")(FakeRequest("GET", ""))
      cookies(r).get("PLAY_LANG").get.value shouldBe "cy"
      status(r) shouldBe SEE_OTHER
    }
  }
}
