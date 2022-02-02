/*
 * Copyright 2022 HM Revenue & Customs
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

import models.auth.AuthenticatedUserRequest
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.{MockFormPartialRetriever, MockTemplateRenderer, UnitSpec}

import scala.concurrent.Future

trait ControllerViewTestHelper extends UnitSpec {

  implicit val authRequest: AuthenticatedUserRequest[AnyContent] = {
    val request: Request[AnyContent] = FakeRequest()
    val nino = new Generator().nextNino

    AuthenticatedUserRequest(request, None, isSA = true, None, nino)
  }

  implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
  implicit val partialRetriever: FormPartialRetriever = mock[MockFormPartialRetriever]

  implicit class ViewMatcherHelper(result: Future[Result]) {

    def rendersTheSameViewAs(expected: Html): Unit =
      contentAsString(result) should equal(expected.toString)
  }
}
