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

package forms

import java.util.Locale

import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import models.{Recipient, Role, Transferor}
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{I18nSupport, Lang, Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import test_utils.{MockAuthenticatedAction, MockFormPartialRetriever, MockTemplateRenderer, MockUnauthenticatedAction}
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.TaxYear
import views.helpers.TextGenerator

class FormsBaseSpec extends UnitSpec with I18nSupport with GuiceOneAppPerSuite with MockitoSugar {

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  val messages: Messages = messagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

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
