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

package utils

import controllers.routes
import models.auth.{AuthenticatedUserRequest, PermanentlyAuthenticated}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import test_utils.{MockFormPartialRetriever, MockTemplateRenderer, TestData}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.test.UnitSpec
import utils.viewHelpers.JSoupMatchers

trait TamcViewTest extends UnitSpec with I18nSupport with GuiceOneAppPerSuite with JSoupMatchers {

  implicit val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val templateRenderer = MockTemplateRenderer
  implicit val partialRetriever = MockFormPartialRetriever
  implicit val request: Request[AnyContent] = FakeRequest()
  implicit val authRequest: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(
    request,
    PermanentlyAuthenticated,
    Some(ConfidenceLevel.L200),
    isSA = false,
    Some("GovernmentGateway"),
    Nino(TestData.Ninos.nino1)
  )

  def view: Html

  def doc: Document = Jsoup.parse(view.toString())

  def pageWithTitle(titleText: String): Unit = {
    "have a static title" in {
      doc.title should include(titleText)
    }
  }

  def pageWithHeader(headerText: String): Unit = {

    "have a static h1 header" in {
      doc should haveHeadingWithText(headerText)
    }
  }

  def pageWithH2Header(headerText: String): Unit = {

    "have a static h2 header" in {
      doc should haveHeadingH2WithText(headerText)
    }
  }


}
