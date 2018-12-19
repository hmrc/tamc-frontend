/*
 * Copyright 2018 HM Revenue & Customs
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

package test_utils

import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.http.CoreGet
import uk.gov.hmrc.play.partials.FormPartialRetriever

object MockFormPartialRetriever extends FormPartialRetriever  {
  override val crypto: String ⇒ String = s ⇒ s
  override def httpGet: CoreGet = ???

  override def getPartialContent(url: String, templateParameters: Map[String, String], errorMessage: Html)(implicit request: RequestHeader): Html = {
    Html("")
  }
}
