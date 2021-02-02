/*
 * Copyright 2021 HM Revenue & Customs
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
import config.TamcFormPartialRetriever
import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.renderer.TemplateRenderer

class ErrorHandler @Inject()(
                             val messagesApi: MessagesApi,
                             formPartialRetriever: TamcFormPartialRetriever
                            )
                            (implicit
                             templateRender: TemplateRenderer
                            ) extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    views.html.templates.error_template(pageTitle, heading, message)

  override def notFoundTemplate(implicit request: Request[_]): Html = views.html.templates.page_not_found_template(formPartialRetriever)
}