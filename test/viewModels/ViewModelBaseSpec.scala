/*
 * Copyright 2020 HM Revenue & Customs
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

package viewModels

import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import test_utils.{MockAuthenticatedAction, MockFormPartialRetriever, MockTemplateRenderer, MockUnauthenticatedAction}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.TamcViewModelTest

//TODO can this be removed
class ViewModelBaseSpec extends TamcViewModelTest {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction])
    .overrides(bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction])
    .overrides(bind[TemplateRenderer].toInstance(MockTemplateRenderer))
    .overrides(bind[FormPartialRetriever].toInstance(MockFormPartialRetriever)
    ).configure(
    "metrics.jvm" -> false,
    "metrics.enabled" -> false
  ).build()
}
