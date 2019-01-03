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

import play.api.test.Helpers.OK

class AuthorisationControllerTest extends ControllerBaseSpec {

  lazy val controller: AuthorisationController = app.injector.instanceOf[AuthorisationController]

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

  //TODO controller.logout

}
