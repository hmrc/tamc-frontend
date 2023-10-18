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

import com.google.inject.Inject
import play.api.Logging
import play.api.mvc.{Action, AnyContent, InjectedController}
import uk.gov.hmrc.mongoFeatureToggles.services.FeatureFlagService

import scala.concurrent.ExecutionContext

class AcceptanceTestHelperController @Inject()
    (val featureFlagService: FeatureFlagService)
    (implicit ec: ExecutionContext)
    extends InjectedController
    with Logging {

  def disableAdminFeatureFlags: Action[AnyContent] = Action.async {
    logger.warn("disabling admin feature flags")
    for {
      featureFlags <- featureFlagService.getAll
      disabledFlags = featureFlags.map(flag => (flag.name, false)).toMap
      _ <- featureFlagService.setAll(disabledFlags)
    } yield {
      Ok
    }
  }
}
