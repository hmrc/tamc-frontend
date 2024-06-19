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

package controllers.auth

import cats.data.EitherT
import config.ApplicationConfig
import connectors.PertaxAuthConnector
import models.auth.AuthenticatedUserRequest
import models.pertaxAuth.{PertaxAuthResponseModel, PertaxErrorView}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR, SEE_OTHER, UNAUTHORIZED}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.Helpers.LOCATION
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.partials.HtmlPartial
import utils.Constants.{ACCESS_GRANTED, CONFIDENCE_LEVEL_UPLIFT_REQUIRED, CREDENTIAL_STRENGTH_UPLIFT_REQUIRED, NO_HMRC_PT_ENROLMENT}
import utils.UnitSpec
import views.Main
import views.html.errors.try_later

import java.time.{Instant, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrier()

  lazy val connector: PertaxAuthConnector =
    mock[PertaxAuthConnector]

  lazy val authAction = new PertaxAuthActionImpl(
    pertaxAuthConnector = connector,
    technicalIssue      = app.injector.instanceOf[try_later],
    main                = app.injector.instanceOf[Main],
    appConfig           = app.injector.instanceOf[ApplicationConfig]
  )(ExecutionContext.Implicits.global, Helpers.stubMessagesControllerComponents())

  lazy val date = LocalDate.now()
  lazy val instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset()
  }

  def authenticatedRequest(requestMethod: String = "GET", requestUrl: String = "/"): AuthenticatedUserRequest[AnyContent] =
    new AuthenticatedUserRequest[AnyContent](
      FakeRequest(requestMethod, requestUrl),
      Some(ConfidenceLevel.L200),
      true,
      None,
      Nino("AA000000A")
    )

  def mockAuth(pertaxAuthResponseModel: PertaxAuthResponseModel): OngoingStubbing[Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]] = {
    when(connector.authorise(any())(any())).thenReturn(Future.successful(Right(pertaxAuthResponseModel)))
  }

  def mockFailedAuth(error: UpstreamErrorResponse): OngoingStubbing[Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]] = {
    when(connector.authorise(any())(any())).thenReturn(Future.successful(Left(error)))
  }

  def block: Request[_] => Future[Result] = _ => Future.successful(Ok("Successful"))

  "PertaxAuthAction.filter" when {

    "the pertax auth feature switch is on" should {

      "return Successful" when {

        "the response from pertax auth connector indicates ACCESS_GRANTED" in {
          val result = {
            mockAuth(PertaxAuthResponseModel(
              ACCESS_GRANTED,
              "This message doesn't matter.",
              None, None
            ))

            when(connector.pertaxPostAuthorise(any(), any()))
              .thenReturn(
                EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                Future.successful(Right(PertaxAuthResponseModel("ACCESS_GRANTED", "", None, None)))
              ))

            authAction.invokeBlock(authenticatedRequest(), block)
          }

          await(bodyOf(result)) shouldBe "Successful"
        }
      }

      "redirect the user" when {

        "the response from pertax auth connector has an UNAUTHORIZED status" in {
          lazy val result = await({
            mockFailedAuth(UpstreamErrorResponse("Error", UNAUTHORIZED))
            authAction.invokeBlock(authenticatedRequest(), block)
          })

          when(connector.pertaxPostAuthorise(any(), any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                Future.successful(Left(UpstreamErrorResponse("Error", UNAUTHORIZED))
                )))

          result.header.status shouldBe SEE_OTHER
        }

        "the response from pertax auth connector indicates NO_HMRC_PT_ENROLMENT and has a redirect URL" which {
          lazy val request = {
            mockAuth(PertaxAuthResponseModel(
              NO_HMRC_PT_ENROLMENT,
              "Still doesn't matter.",
              Some("/some-redirect"),
              None
            ))

            when(connector.pertaxPostAuthorise(any(), any()))
              .thenReturn(
                EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                  Future.successful(Right(PertaxAuthResponseModel("NO_HMRC_PT_ENROLMENT", "", Some("/some-redirect/"), None)
                ))))

            authAction.invokeBlock(authenticatedRequest(requestUrl = "/some-base-url"), block)
          }
          lazy val result = await(request)

          "has a status of SEE_OTHER(303)" in {
            status(result) shouldBe SEE_OTHER
          }

          "has a redirect url of '/some-redirect'" in {
            val expectedRedirectUrl = "/some-redirect/?redirectUrl=%2Fsome-base-url"

            result.header.headers(LOCATION) shouldBe expectedRedirectUrl
          }

          "the response from pertax auth connector indicates CREDENTIAL_STRENGTH_UPLIFT_REQUIRED" which {
            lazy val request = {
              mockAuth(PertaxAuthResponseModel(
                CREDENTIAL_STRENGTH_UPLIFT_REQUIRED,
                "Still doesn't matter.",
                Some("/some-redirect"),
                None
              ))

              when(connector.pertaxPostAuthorise(any(), any()))
                .thenReturn(
                  EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                    Future.successful(Right(PertaxAuthResponseModel("CREDENTIAL_STRENGTH_UPLIFT_REQUIRED", "", Some("/some-redirect/"), None)
                    ))))

              authAction.invokeBlock(authenticatedRequest(requestUrl = "/some-base-url"), block)
            }
            lazy val result = await(request)

            "has a status of INTERNAL_SERVER_ERROR(500)" in {
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }

          "the response from pertax auth connector indicates CONFIDENCE_LEVEL_UPLIFT_REQUIRED and has a redirect URL" which {
            lazy val request = {
              mockAuth(PertaxAuthResponseModel(
                CONFIDENCE_LEVEL_UPLIFT_REQUIRED,
                "Still doesn't matter.",
                Some("/some-redirect"),
                None
              ))

              when(connector.pertaxPostAuthorise(any(), any()))
                .thenReturn(
                  EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                    Future.successful(Right(PertaxAuthResponseModel("CONFIDENCE_LEVEL_UPLIFT_REQUIRED", "", Some("/some-redirect/"), None)
                    ))))

              authAction.invokeBlock(authenticatedRequest(requestUrl = "/some-base-url"), block)
            }
            lazy val result = await(request)

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }
          }
        }
      }

      "return an error and a partial returned from the pertax auth service" when {

        "the pertax auth service returns a partial" which {

          lazy val result = await({
            mockAuth(PertaxAuthResponseModel(
              "NOT_A_VALID_CODE",
              "Doesn't matter, even now.",
              None,
              Some(PertaxErrorView(IM_A_TEAPOT, "/partial-url"))
            ))

            when(connector.pertaxPostAuthorise(any(), any()))
              .thenReturn(
                EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                  Future.successful(Right(PertaxAuthResponseModel(
                    "NOT_A_VALID_CODE",
                    "Doesn't matter, even now.",
                    None,
                    Some(PertaxErrorView(IM_A_TEAPOT, "/partial-url"))
                  )))))

            when(connector.loadPartial(any())(any())).thenReturn(Future.successful(
              HtmlPartial.Success(Some("Test Title"), Html("<div id=\"partial\">Hello</div>"))
            ))

            authAction.invokeBlock(authenticatedRequest(), block)
          })

          "has a status of IM_A_TEAPOT(418)" in {
            result.header.status shouldBe IM_A_TEAPOT
          }

          "has a title of 'Test Title' and partial of 'Hello'" in {
            lazy val doc = Jsoup.parse(bodyOf(result))
            doc.getElementsByTag("title").text() shouldBe "Test Title - title.pattern GOV.UK"
            doc.getElementById("partial").text() shouldBe "Hello"
          }
        }
      }

      "return an internal server error" when {

        "pertax auth connector returns a left that isn't unauthorised" in {
          lazy val result = await({
            mockFailedAuth(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR))
            authAction.invokeBlock(authenticatedRequest(), block)
          })

          when(connector.pertaxPostAuthorise(any(), any()))
            .thenReturn(
              EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                Future.successful(Left(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR))
                )))

          result.header.status shouldBe INTERNAL_SERVER_ERROR
        }

        "the pertax auth service fails to return a partial" which {

          "has a status of INTERNAL_SERVER_ERROR(500)" in {
            lazy val result = await({
              mockAuth(PertaxAuthResponseModel(
                "NOT_A_VALID_CODE",
                "Doesn't matter, even now.",
                None,
                Some(PertaxErrorView(IM_A_TEAPOT, "/partial-url"))
              ))

              when(connector.pertaxPostAuthorise(any(), any()))
                .thenReturn(
                  EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                    Future.successful(Right(PertaxAuthResponseModel(
                      "NOT_A_VALID_CODE",
                      "Doesn't matter, even now.",
                      None,
                      Some(PertaxErrorView(IM_A_TEAPOT, "/partial-url"))
                    )))))

              when(connector.loadPartial(any())(any())).thenReturn(Future.successful(
                HtmlPartial.Failure(None, "ERROR")
              ))

              authAction.invokeBlock(authenticatedRequest(), block)
            })

            result.header.status shouldBe INTERNAL_SERVER_ERROR
          }
        }

        "the pertax authentication fails" which {

          "has a status of INTERNAL_SERVER_ERROR(500)" in {
            lazy val result = await({
              mockFailedAuth(UpstreamErrorResponse("I'M AN ERROR", INTERNAL_SERVER_ERROR))
              authAction.invokeBlock(authenticatedRequest(), block)
            })

            when(connector.pertaxPostAuthorise(any(), any()))
              .thenReturn(
                EitherT[Future, UpstreamErrorResponse, PertaxAuthResponseModel](
                  Future.successful(Right(PertaxAuthResponseModel(
                    "NOT_A_VALID_CODE",
                    "Doesn't matter, even now.",
                    None,
                    None
                  )))))

            result.header.status shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}
