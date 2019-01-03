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

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import test_utils.{MockAuthenticatedAction, MockUnauthenticatedAction, TestUtility}
import uk.gov.hmrc.http.HeaderCarrier

trait ControllerBaseSpec extends TestUtility with GuiceOneAppPerSuite with MockitoSugar {

  val defaultBuilder: GuiceApplicationBuilder = new GuiceApplicationBuilder()
    .overrides(bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction])
    .overrides(bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction])

  override implicit lazy val app: Application = defaultBuilder.build()

  def instanceOf[T](implicit evidence: scala.reflect.ClassTag[T]): T = app.injector.instanceOf[T]

  implicit val request: Request[AnyContent] = FakeRequest()
  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
}