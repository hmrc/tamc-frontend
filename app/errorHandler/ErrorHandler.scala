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

package errorHandler

import com.google.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, RequestHeader}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import scala.concurrent.{ExecutionContext, Future}

class ErrorHandler @Inject()(
                              override implicit val messagesApi: MessagesApi,
                              errorTemplate: views.html.templates.error_template,
                              pageNotFoundTemplate: views.html.templates.page_not_found_template)
                            (
                              implicit val ec: ExecutionContext
                            ) extends FrontendErrorHandler {

  //FIXME sca-wrapper > 9.0.0 will have some breaking changes, views will be based on RequestHeader instead of Request[_]
  private def rhToRequest(rh: RequestHeader): Request[_] = Request(rh, "")


  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: RequestHeader): Future[Html] = {
    implicit val req: Request[_] = rhToRequest(request)
    Future.successful(errorTemplate(pageTitle, heading, message))
  }

  override def notFoundTemplate(implicit request: RequestHeader): Future[Html] = {
    implicit val req: Request[_] =  rhToRequest(request)
    Future.successful(pageNotFoundTemplate())
  }


}
