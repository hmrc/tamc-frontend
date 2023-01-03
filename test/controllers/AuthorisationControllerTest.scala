/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.test.Helpers._
import play.api.test.Helpers.baseApplicationBuilder.injector
import utils.ControllerBaseTest

class AuthorisationControllerTest extends ControllerBaseTest {

  lazy val controller: AuthorisationController = app.injector.instanceOf[AuthorisationController]
  val applicationConfig: ApplicationConfig = injector.instanceOf[ApplicationConfig]

  "Calling notAuthorised" should {
    "return OK" in {
      val result = await(controller.notAuthorised()(request))
      status(result) shouldBe OK
    }
  }

  "Calling sessionTimeout" should {
    "return OK" in {
      val result = await(controller.sessionTimeout()(request))
      status(result) shouldBe OK
    }
  }

  "Calling logout" should {
    "redirect" in {
      val result = await(controller.logout()(request))
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(applicationConfig.logoutUrl)
    }
  }

}
