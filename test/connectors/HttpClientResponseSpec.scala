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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.RecoverMethods.recoverToExceptionIf
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpException, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor, Future}

class HttpClientResponseSpec extends AnyWordSpec with Matchers {
  implicit val ec: ExecutionContextExecutor = ExecutionContext.global
  val mockHttpResponse: HttpResponse = mock[HttpResponse]
  val httpClientResponse = new HttpClientResponse
  val mockLogger: Logger = mock[Logger]

  "HttpClientResponse" should {
    "return HttpResponse for successful response" in {

      val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.successful(Right(mockHttpResponse))

      val result: EitherT[Future, UpstreamErrorResponse, HttpResponse] =
        httpClientResponse.read(response)

      result.value.map { res =>
        res shouldBe Right(mockHttpResponse)
      }
    }

    List(
      BAD_REQUEST,
      NOT_FOUND,
      REQUEST_TIMEOUT,
      UNPROCESSABLE_ENTITY,
      INTERNAL_SERVER_ERROR,
      BAD_GATEWAY,
      SERVICE_UNAVAILABLE,
      LOCKED,
      UNAUTHORIZED
      ).foreach { error =>
        s"hand $error and log info" in {
        val returnedError = UpstreamErrorResponse("Error Message", error)

        val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
          Future.successful(Left(returnedError))

        val result: EitherT[Future, UpstreamErrorResponse, HttpResponse] =
          httpClientResponse.read(response)

        result.value.map { res =>
          res shouldBe Left(returnedError)
          verify(mockLogger, times(1)).info(any)
        }
      }
    }
    "return BAD_GATEWAY when the response is a HttpException" in {

      val exception = new HttpException("Some Error", BAD_GATEWAY)
      val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.failed(exception)

      val result = Await.result(httpClientResponse.read(response).value, 20.seconds)
      result shouldBe Left(UpstreamErrorResponse("Some Error", BAD_GATEWAY, BAD_GATEWAY))
    }

    "return exception when the response is a generic exception" in {

      val exception = new Exception("Some Error")
      val response: Future[Either[UpstreamErrorResponse, HttpResponse]] =
        Future.failed(exception)

      recoverToExceptionIf[Exception] {
        httpClientResponse.read(response).value
      }.map { ex =>
        ex.getMessage shouldBe "Some Error"
       }
    }
  }
}
