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

package controllers

import java.util.concurrent.TimeUnit

import controllers.actions.{AuthenticatedActionRefiner, UnauthenticatedActionTransformer}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.words.MustVerb
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import test_utils._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

trait ControllerBaseSpec extends UnitSpec
  with MustVerb
  with I18nSupport
  with GuiceOneAppPerSuite
  with MockitoSugar {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction])
    .overrides(bind[UnauthenticatedActionTransformer].to[MockUnauthenticatedAction])
    .overrides(bind[TemplateRenderer].toInstance(MockTemplateRenderer))
    .overrides(bind[FormPartialRetriever].toInstance(MockFormPartialRetriever)
    ).configure(
    "metrics.jvm" -> false,
    "metrics.enabled" -> false
  ).build()

  def instanceOf[T](implicit evidence: scala.reflect.ClassTag[T]): T = app.injector.instanceOf[T]

  implicit val request: Request[AnyContent] = FakeRequest()

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  def awaitResult(result: Future[Result]): Future[Result] = {
    Future.successful(Await.result(result, Duration(5, TimeUnit.SECONDS)))
  }

  def getContactHMRCText(testCase: String): String = {
    testCase match  {
      case "changeOfIncome" =>
        (messagesApi("general.helpline.enquiries.link.pretext") + " "
          + messagesApi("general.helpline.enquiries.link") + " "
          + messagesApi("pages.changeOfIncome.enquiries.link.paragraph"))
      case "bereavement" =>
        (messagesApi("general.helpline.enquiries.link.pretext") + " "
          + messagesApi("general.helpline.enquiries.link") + " "
          + messagesApi("pages.bereavement.enquiries.link.paragraph"))
      case _ => throw new RuntimeException("asd")
    }

  }

}
