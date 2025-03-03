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

package utils

import controllers.ControllerViewTestHelper
import errors._
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.test.Helpers._
import play.api.test.Injecting
import test_utils._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class UpdateRelationshipErrorHandlerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  lazy val updateRelationshipErrorHandler: UpdateRelationshipErrorHandler = app.injector.instanceOf[UpdateRelationshipErrorHandler]

  "handleError" should {
    val authRequest: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(
      request,
      Some(ConfidenceLevel.L200),
      isSA = false,
      Some("GovernmentGateway"),
      Nino(TestData.Ninos.nino1)
    )

    "return internal server error" when {
      val errors = List(
        (new CacheMissingUpdateRecord, "Sorry, there is a problem with the service"),
        (new CacheUpdateRequestNotSent, "Sorry, there is a problem with the service"),
        (new CannotUpdateRelationship, "Sorry, there is a problem with the service"),
        (new CitizenNotFound, "Call us to make a change to your Marriage Allowance. Have your National Insurance number ready when you call"),
        (new BadFetchRequest, "Sorry, there is a problem with the service"),
        (new Exception, "Sorry, there is a problem with the service")
      )

      for ((error, message) <- errors) {
        s"a $error has been thrown" in {
          val result = Future.successful(updateRelationshipErrorHandler.handleError(authRequest)(error))
          status(result) shouldBe INTERNAL_SERVER_ERROR
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("error").text() shouldBe message
        }
      }
    }

    "return OK" when {
      val errors = List(
        (new TransferorNotFound, "We were unable to find a HMRC record for you."),
        (new RecipientNotFound, "We were unable to find a HMRC record of your partner.")
      )

      for ((error, message) <- errors) {
        s"a $error has been thrown" in {
          val result = Future.successful(updateRelationshipErrorHandler.handleError(authRequest)(error))
          status(result) shouldBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("error").text() shouldBe message
        }
      }
    }

    "redirect" when {
      "a errors.CacheRelationshipAlreadyUpdated exception has been thrown" in {
        val result = Future.successful(updateRelationshipErrorHandler.handleError(authRequest)(new CacheRelationshipAlreadyUpdated))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.UpdateRelationship.routes.FinishedChangeController.finishUpdate().url)
      }
    }
  }

}
