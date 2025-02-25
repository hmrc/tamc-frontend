/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors.httpParsers

import connectors.httpParsers.PertaxAuthenticationHttpParser.PertaxAuthenticationHttpReads
import models.pertaxAuth.PertaxAuthResponseModel
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}
import utils.BaseTest

class PertaxAuthenticationHttpParserSpec extends BaseTest {
  "PertaxAuthenticationHttpReads" should {
    "return Right(PertaxAuthResponseModel) when valid JSON is returned" in {
      val validJson = Json.parse(
        """
          {
            "code": "ACCESS_GRANTED",
            "message": "Authentication successful",
            "redirect": "/home",
            "errorView": null
          }
        """
      )
      
      val httpResponse = mock[HttpResponse]
      when(httpResponse.json).thenReturn(validJson)

      val expectedResult = PertaxAuthResponseModel("ACCESS_GRANTED", "Authentication successful", Some("/home"), None)

      val result = PertaxAuthenticationHttpReads.read("GET", "/some-url", httpResponse)

      result mustBe Right(expectedResult)
    }

    "return Left(UpstreamErrorResponse) when invalid JSON is returned" in {
      val invalidJson: JsValue = Json.parse(
        """
            {
              "invalidField": "InvalidValue"
            }
          """
      )
      val httpResponse = mock[HttpResponse]
      when(httpResponse.json).thenReturn(invalidJson)

      val result = PertaxAuthenticationHttpReads.read("GET", "/some-url", httpResponse)

      result mustBe Left(UpstreamErrorResponse(
        "[PertaxAuthenticationHttpParser][read] There was an issue parsing the response from Pertax Auth.",
        INTERNAL_SERVER_ERROR
      ))
    }
  }
}
