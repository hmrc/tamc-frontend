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

package config

import connectors.{ApplicationAuditConnector, TamcAuthConnector}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

class TamcModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[AuthConnector].to[TamcAuthConnector],
      bind[HttpClient].to[DefaultHttpClient],
      bind[AuditConnector].toInstance(ApplicationAuditConnector),
      bind[TimeService].toInstance(TimeService),
      bind[TransferService].toInstance(TransferService),
      bind[UpdateRelationshipService].toInstance(UpdateRelationshipService),
      bind[CachingService].toInstance(CachingService),
      bind[EligibilityCalculatorService].toInstance(EligibilityCalculatorService),
      bind[TemplateRenderer].toInstance(LocalTemplateRenderer),
      bind[FormPartialRetriever].toInstance(TamcFormPartialRetriever)
    )
}
