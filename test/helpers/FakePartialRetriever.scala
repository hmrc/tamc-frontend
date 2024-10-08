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

package helpers

import com.google.inject.Inject
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.partials.{FormPartialRetriever, HeaderCarrierForPartialsConverter}

import scala.concurrent.ExecutionContext


class FakePartialRetriever @Inject()(
                                      override val httpClientV2: HttpClientV2,
                                      override val headerCarrierForPartialsConverter: HeaderCarrierForPartialsConverter
                                    ) extends FormPartialRetriever {

  override def getPartialContent(url: String, templateParameters: Map[String, String], errorMessage: Html)
                                (implicit ec: ExecutionContext, request: RequestHeader): Html = Html("")
}
