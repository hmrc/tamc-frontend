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

package connectors

import org.mockito.ArgumentMatchers.{any => mockitoAny, eq => mockitoEq}
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, MustMatchers}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.play.{PlaySpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.{BadGatewayException, HeaderCarrier, HttpGet, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class ContactFrontendConnectorSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar with BeforeAndAfterEach with ServicesConfig with MustMatchers {

  implicit override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map("Test.microservice.assets.url" -> "test-url", "Test.microservice.assets.version" -> "test-version"))
    .build

  implicit val headerCarrier = HeaderCarrier()

  object TestConnector extends ContactFrontendConnector {
    override val http: HttpGet = mock[HttpGet]
  }

  override def beforeEach(): Unit = {
    Mockito.reset(TestConnector.http)
  }

  private val OK = 200

  "ContactFrontendConnector" must {

    val dummyResponseHtml = "<div id=\"contact-partial\"></div>"
    lazy val serviceBase = s"${baseUrl("contact-frontend")}/contact"
    lazy val serviceUrl = s"$serviceBase/problem_reports"

    "contact the front end service to download the 'get help' partial" in {

      val response = HttpResponse(OK, responseString = Some(dummyResponseHtml))

      Mockito.when(TestConnector.http.GET[HttpResponse](mockitoEq(serviceUrl))(mockitoAny(), mockitoAny[HeaderCarrier], mockitoAny())) thenReturn Future.successful(response)

      await(TestConnector.getHelpPartial)

      Mockito.verify(TestConnector.http).GET(mockitoEq(serviceUrl))(mockitoAny(), mockitoAny[HeaderCarrier], mockitoAny())
    }

    "return an empty string if a BadGatewayException is encountered" in {

      Mockito.when(TestConnector.http.GET[HttpResponse](mockitoEq(serviceUrl))(mockitoAny(), mockitoAny[HeaderCarrier], mockitoAny())) thenReturn Future.failed(new BadGatewayException("Phony exception"))

      val result = await(TestConnector.getHelpPartial)

      result mustBe ""
    }
  }
}
