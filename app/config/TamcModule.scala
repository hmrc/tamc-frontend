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

package config

import connectors.TamcAuthConnector
import play.api.cache.AsyncCacheApi
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.mongoFeatureToggles.internal.config.AppConfig
import uk.gov.hmrc.mongoFeatureToggles.internal.repository.FeatureFlagRepository
import uk.gov.hmrc.mongoFeatureToggles.services.{FeatureFlagService, FeatureFlagServiceImpl}
import uk.gov.hmrc.time.{CurrentTaxYear, TaxYear}
import utils.{TaxBandReader, TaxBandReaderImpl}

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext


class TamcModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[AuthConnector].to[TamcAuthConnector],
      bind[CurrentTaxYear].toInstance(TaxYear),
      bind[TaxBandReader].to[TaxBandReaderImpl],
      bind[ApplicationStartUp].toSelf.eagerly(),
      bind[FeatureFlagService].toProvider[FeatureFlagServiceProvider].in[Singleton]
    )
}

class FeatureFlagServiceProvider @Inject()(appConfig: AppConfig, cache: AsyncCacheApi, repo: FeatureFlagRepository)(implicit ec: ExecutionContext)
  extends Provider[FeatureFlagService] {
  override def get(): FeatureFlagService = {
    println("---------------------------------------------")
    println("---------------------------appConfig:"+appConfig)
    println("---------------------------repo:"+repo)
    println("---------------------------cache:"+cache)
    new FeatureFlagServiceImpl(appConfig, repo, cache)(ec)
  }
}