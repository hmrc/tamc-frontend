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

package services.admin

import akka.Done
import config.ApplicationConfig
import models.admin.PertaxBackendToggle
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.cache.AsyncCacheApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Injecting
import repositories.admin.FeatureFlagRepository
import utils.UnitSpec
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

class FeatureFlagServiceSpec extends UnitSpec with Injecting with BeforeAndAfterEach with GuiceOneAppPerSuite {

  val mockAppConfig             = mock[ApplicationConfig]
  val mockFeatureFlagRepository = mock[FeatureFlagRepository]
  val mockCache                 = mock[AsyncCacheApi]

  override implicit lazy val app = GuiceApplicationBuilder()
    .overrides(
      bind[ApplicationConfig].toInstance(mockAppConfig),
      bind[FeatureFlagRepository].toInstance(mockFeatureFlagRepository),
      bind[AsyncCacheApi].toInstance(mockCache)
    )
    .build()

  override def beforeEach(): Unit = {
    reset(mockAppConfig)
    reset(mockFeatureFlagRepository)
    reset(mockCache)
  }

  val featureFlagService = inject[FeatureFlagService]

  "set" must {
    "set a feature flag" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlag(any(), any())).thenReturn(Future.successful(true))

      val result = featureFlagService.set(PertaxBackendToggle, true).futureValue

      result shouldBe true

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlag(any(), any())

      val arguments: List[String] = eventCaptor.getAllValues.asScala.toList
      arguments.sorted shouldBe List(
        PertaxBackendToggle.toString,
        "*$*$allFeatureFlags*$*$"
      ).sorted
    }
  }

  "setAll" must {
    "set all the feature flags provided" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlags(any()))
        .thenReturn(
          Future.successful(
            ()
          )
        )

      val result = featureFlagService
        .setAll(
          Map(PertaxBackendToggle -> true)
        )
        .futureValue

      result shouldBe ()

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlags(any())

      val arguments: List[String] = eventCaptor.getAllValues.asScala.toList
      arguments.sorted shouldBe List(
        PertaxBackendToggle.toString,
        "*$*$allFeatureFlags*$*$"
      ).sorted
    }

    "return false when failing to set all the feature flags provided" in {
      when(mockCache.remove(any())).thenReturn(Future.successful(Done))
      when(mockFeatureFlagRepository.setFeatureFlags(any()))
        .thenReturn(
          Future.successful(())
        )

      val result = featureFlagService
        .setAll(
          Map(PertaxBackendToggle -> true)
        )
        .futureValue

      result shouldBe ()

      val eventCaptor = ArgumentCaptor.forClass(classOf[String])
      verify(mockCache, times(2)).remove(eventCaptor.capture())
      verify(mockFeatureFlagRepository, times(1)).setFeatureFlags(any())
    }
  }
}
