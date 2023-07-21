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

package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.pertaxAuth.PertaxAuthResponseModel
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import play.utils.UriEncoding

trait PertaxAuthMockingHelper {
  this: WireMockHelper with GuiceOneAppPerSuite =>

  def mockPertaxAuth(returnedValue: PertaxAuthResponseModel, nino: String = "AA000000A"): StubMapping = {
    server.stubFor(
      get(urlMatching(s"/pertax/$nino/authorise"))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withBody(Json.stringify(Json.toJson(returnedValue)))
        )
    )
  }

  def mockPertaxAuthFailure(returnedValue: JsValue, nino: String = "AA000000A"): StubMapping = {
    server.stubFor(
      get(urlMatching(s"/pertax/$nino/authorise"))
        .willReturn(
          aResponse()
            .withStatus(BAD_REQUEST)
            .withBody(Json.stringify(returnedValue))
        )
    )
  }

  def mockPertaxPartial(body: String, title: Option[String], status: Int = OK, partialUrl: String = "partial"): StubMapping = {
    val response = aResponse()
      .withStatus(status)
      .withBody(body)

    server.stubFor(
      get(urlMatching(s"/pertax/partials/$partialUrl"))
        .willReturn(
          title.fold(response) { unwrappedTitle =>
            response.withHeader("X-Title", UriEncoding.encodePathSegment(unwrappedTitle, "UTF-8"))
          }
        )
    )
  }
}