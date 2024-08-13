/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import cats.implicits.catsStdInstancesForFuture
import models.pertaxAuth.PertaxAuthResponseModel
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.test.Injecting
import play.twirl.api.Html
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.Constants.ACCESS_GRANTED
import utils.{BaseTest, PertaxAuthMockingHelper, WireMockHelper}

class PertaxAuthConnectorSpec extends BaseTest with GuiceOneAppPerSuite with WireMockHelper with PertaxAuthMockingHelper with Injecting {

  lazy val connector: PertaxAuthConnector = inject[PertaxAuthConnector]

  val nino = "AA000000A"

  "PertaxAuthConnector" when {
    "calling .authorise" when {
      "pertax auth returns a successful response" should {
        s"return a PertaxAuthResponseModel containing the data returned by pertax" in {
          val expectedReturnModel = PertaxAuthResponseModel(ACCESS_GRANTED, "A field", None, None)

          lazy val result = {
            mockPertaxAuth(expectedReturnModel)
            connector.authorise(nino)
          }

          await(result) shouldBe Right(expectedReturnModel)
        }
      }

      "pertax returns Json that cannot be parsed" should {
        "return an UpstreamErrorResponse" in {
          val expectedReturnModel = UpstreamErrorResponse(
            "[PertaxAuthenticationHttpParser][read] There was an issue parsing the response from Pertax Auth.",
            INTERNAL_SERVER_ERROR
          )

          val brokenJson = Json.obj(
            "value1" -> "This won't work"
          )

          lazy val result = {
            mockPertaxAuthFailure(brokenJson)
            connector.authorise(nino)
          }

          await(result) shouldBe Left(expectedReturnModel)
        }
      }
    }

    "calling .pertaxPostAuthorise" when {
      "pertax returns a successful response" should {
        "return the expected return model" in {
          val expectedReturnModel = PertaxAuthResponseModel(ACCESS_GRANTED, "A field", None, None)

          val result = {
            mockPostPertaxAuth(expectedReturnModel)
            connector.pertaxPostAuthorise
          }

          await(result).map { res =>
            res shouldBe expectedReturnModel
          }
        }
      }
    }

    "calling .loadPartial" when {
      "pertax returns a successful response" should {
        "return an HtmlPartial" in {
          val expectedPartial = HtmlPartial.Success(Some("Test Page"), Html("<html></html>"))

          val result = {
            mockPertaxPartial("""<html></html>""", Some("Test Page"))
            connector.loadPartial("partial")
          }

          await(result) shouldBe expectedPartial
        }
      }

      "pertax returns an unsuccessful response" should {
        "return a failure partial" in {
          val expectedPartial = HtmlPartial.Failure(Some(BAD_REQUEST), "<html>Failure Page</html>")

          val result = {
            mockPertaxPartial("<html>Failure Page</html>", None, BAD_REQUEST)
            connector.loadPartial("partial")
          }

          await(result) shouldBe expectedPartial
        }
      }

      "ensure url has leading /" should {
        "check char at 0" in {
          val expectedPartial = HtmlPartial.Failure(Some(BAD_REQUEST), "<html>Failure Page</html>")

          val result1 = {
            mockPertaxPartial("<html>Failure Page</html>", None, BAD_REQUEST)
            connector.loadPartial("/partial")
          }

          val result2 = {
            mockPertaxPartial("<html>Failure Page</html>", None, BAD_REQUEST)
            connector.loadPartial("partial")
          }

          await(result1) shouldBe expectedPartial
          await(result2) shouldBe expectedPartial
        }
      }
    }
  }
}
