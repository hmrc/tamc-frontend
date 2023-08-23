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

package controllers.testonly

import models.admin.PertaxBackendToggle
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService
import utils.ControllerBaseTest

import scala.concurrent.Future

class AcceptanceTestHelperControllerTest extends ControllerBaseTest {

  private val mockFeatureFlagService = mock[FeatureFlagService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(bind[FeatureFlagService].toInstance(mockFeatureFlagService))
    .build()

  private val systemUnderTest = app.injector.instanceOf[AcceptanceTestHelperController]

  "AcceptanceTestHelperController" should {
    "retrieve, disable, and save admin features" in {

      val enabledPertaxBackendToggle = FeatureFlag(PertaxBackendToggle, true)
      when(mockFeatureFlagService.getAll) thenReturn Future.successful(List(enabledPertaxBackendToggle))

      val expectedDisabledFeatures: Map[FeatureFlagName, Boolean] = Map(PertaxBackendToggle -> false)
      when(mockFeatureFlagService.setAll(ArgumentMatchers.eq(expectedDisabledFeatures))).thenReturn(Future.successful(()))

      val result = systemUnderTest.disableAdminFeatureFlags()(request)
      status(result) shouldBe OK
    }
  }

}
