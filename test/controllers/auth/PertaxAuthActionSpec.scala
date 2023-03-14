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

package uk.gov.hmrc.nisp.controllers.auth

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Play.materializer
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.LOCATION
import play.api.test.{FakeRequest, Helpers}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{LoginTimes, Name}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nisp.connectors.PertaxAuthConnector
import uk.gov.hmrc.nisp.models.UserName
import uk.gov.hmrc.nisp.models.admin.{FeatureFlag, PertaxBackendToggle}
import uk.gov.hmrc.nisp.models.pertaxAuth.{PertaxAuthResponseModel, PertaxErrorView}
import uk.gov.hmrc.nisp.services.admin.FeatureFlagService
import uk.gov.hmrc.nisp.utils.{Constants, UnitSpec}
import uk.gov.hmrc.nisp.views.html.iv.failurepages.technical_issue
import uk.gov.hmrc.play.partials.HtmlPartial

import java.time.{Instant, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class PertaxAuthActionSpec extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach {

  lazy val connector: PertaxAuthConnector = mock[PertaxAuthConnector]

  lazy val featureFlagService = mock[FeatureFlagService]

  lazy val authAction = new PertaxAuthActionImpl(
    connector,
    app.injector.instanceOf[technical_issue],
    featureFlagService
  )(ExecutionContext.Implicits.global, Helpers.stubMessagesControllerComponents())

  lazy val date = LocalDate.now()
  lazy val instant = Instant.now()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset()
  }

  def authenticatedRequest(requestMethod: String = "GET", requestUrl: String = "/"): AuthenticatedRequest[AnyContent] = new AuthenticatedRequest[AnyContent](
    FakeRequest(requestMethod, requestUrl),
    NispAuthedUser(
      Nino("AA000000A"),
      date,
      UserName(Name(Some("John"), Some("Doe"))),
      None, None, isSa = true
    ),
    AuthDetails(ConfidenceLevel.L200, LoginTimes(
      instant, None
    ))
  )

  def mockAuth(pertaxAuthResponseModel: PertaxAuthResponseModel): OngoingStubbing[Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]] = {
    when(connector.authorise(any())(any())).thenReturn(Future.successful(Right(pertaxAuthResponseModel)))
  }

  def mockFailedAuth(error: UpstreamErrorResponse): OngoingStubbing[Future[Either[UpstreamErrorResponse, PertaxAuthResponseModel]]] = {
    when(connector.authorise(any())(any())).thenReturn(Future.successful(Left(error)))
  }

  def block: AuthenticatedRequest[_] => Future[Result] = _ => Future.successful(Ok("Successful"))

  "PertaxAuthAction.refine" when {

    "the pertax auth feature switch is on" should {

      "return Successful" when {

        "the response from pertax auth connector indicates ACCESS_GRANTED" in {
          val result = {
            mockAuth(PertaxAuthResponseModel(
              Constants.ACCESS_GRANTED,
              "This message doesn't matter.",
              None, None
            ))

            when(featureFlagService.get(PertaxBackendToggle)).thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))

            authAction.invokeBlock(authenticatedRequest(), block)
          }

          await(bodyOf(result)) shouldBe "Successful"
        }
      }

      "redirect the user" when {

        "the response from pertax auth connector indicates NO_HMRC_PT_ENROLMENT and has a redirect URL" which {
          lazy val request = {
            mockAuth(PertaxAuthResponseModel(
              Constants.NO_HMRC_PT_ENROLMENT,
              "Still doesn't matter.",
              Some("/some-redirect"),
              None
            ))

            when(featureFlagService.get(PertaxBackendToggle)).thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))

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

            when(connector.loadPartial(any())(any())).thenReturn(Future.successful(
              HtmlPartial.Success(Some("Test Title"), Html("Hello"))
            ))

            when(featureFlagService.get(PertaxBackendToggle)).thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))

            authAction.invokeBlock(authenticatedRequest(), block)
          })

          "has a status of IM_A_TEAPOT(418)" in {
            result.header.status shouldBe IM_A_TEAPOT
          }

          "has a body of 'Hello'" in {
            bodyOf(result) shouldBe "Hello"
          }
        }

      }

      "return an internal server error" when {

        "the pertax auth service fails to return a partial" which {

          "has a status of INTERNAL_SERVER_ERROR(500)" in {
            lazy val result = await({
              mockAuth(PertaxAuthResponseModel(
                "NOT_A_VALID_CODE",
                "Doesn't matter, even now.",
                None,
                Some(PertaxErrorView(IM_A_TEAPOT, "/partial-url"))
              ))

              when(featureFlagService.get(PertaxBackendToggle)).thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))

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

            when(featureFlagService.get(PertaxBackendToggle)).thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = true)))

            result.header.status shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    "the pertax feature switch is off" should {

      "return Successful in the request body" in {
        lazy val result = await {
          when(featureFlagService.get(PertaxBackendToggle)).thenReturn(Future.successful(FeatureFlag(PertaxBackendToggle, isEnabled = false)))
          authAction.invokeBlock(authenticatedRequest(), block)
        }

        bodyOf(result) shouldBe "Successful"
      }

    }
  }

}
