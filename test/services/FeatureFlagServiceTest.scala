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

package services

import models.admin.{FeatureFlag, NoExistantToggle, PertaxBackendToggle}
import org.scalatest.BeforeAndAfterAll
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import services.admin.FeatureFlagService
import utils.BaseTest

class FeatureFlagServiceTest extends BaseTest with BeforeAndAfterAll{

  val featureFlagService: FeatureFlagService = app.injector.instanceOf[FeatureFlagService]

  override def fakeApplication(): Application = GuiceApplicationBuilder().build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    featureFlagService.set(PertaxBackendToggle, enabled = true)
  }

  "getFlagValue when flag does not exist " should {
    "return requested toggle with enabled false " in {
      val result = featureFlagService.get(NoExistantToggle)
      await(result) shouldBe FeatureFlag(NoExistantToggle, isEnabled = false, None)
    }
  }


  "getFlagValue for PertaxBackendToggle " should {
    "return true when set " in {
      val result = featureFlagService.get(PertaxBackendToggle)
      await(result) shouldBe FeatureFlag(PertaxBackendToggle, isEnabled = true, None)
    }
  }

}
