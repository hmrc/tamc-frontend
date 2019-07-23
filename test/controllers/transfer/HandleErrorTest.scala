/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.transfer

import java.util.Locale

import controllers.ControllerBaseSpec
import errors._
import models.auth.{AuthenticatedUserRequest, PermanentlyAuthenticated}
import org.jsoup.Jsoup
import play.api.i18n.Lang
import play.api.test.Helpers._
import test_utils.TestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.language.postfixOps

class HandleErrorTest extends ControllerBaseSpec {


  "handleError" should {
    val authRequest: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(
      request,
      PermanentlyAuthenticated,
      Some(ConfidenceLevel.L200),
      isSA = false,
      Some("GovernmentGateway"),
      Nino(TestData.Ninos.nino1)
    )
    "redirect" when {
      val data = List(
        (new CacheMissingTransferor, "/marriage-allowance-application/history"),
        (new CacheMissingRecipient, "/marriage-allowance-application/history"),
        (new CacheMissingEmail, "/marriage-allowance-application/confirm-your-email"),
        (new CacheRelationshipAlreadyCreated, "/marriage-allowance-application/history"),
        (new CacheCreateRequestNotSent, "/marriage-allowance-application/history"),
        (new RelationshipMightBeCreated, "/marriage-allowance-application/history"),
        (new TransferorDeceased, "/marriage-allowance-application/you-cannot-use-this-service"),
        (new RecipientDeceased, "/marriage-allowance-application/you-cannot-use-this-service")
      )
      for ((error, redirectUrl) <- data) {
        s"a $error has been thrown" in {
          val result = HandleError.apply(HeaderCarrier(), authRequest, messages, instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])(error)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
      }
    }

    "handle an error" when {
      val data = List(
        (new TransferorNotFound, INTERNAL_SERVER_ERROR, "transferor.not.found"),
        (new RecipientNotFound, INTERNAL_SERVER_ERROR, "recipient.not.found.para1"),
        (new CacheRecipientInRelationship, INTERNAL_SERVER_ERROR, "recipient.has.relationship.para1"),
        (new CannotCreateRelationship, INTERNAL_SERVER_ERROR, "create.relationship.failure"),
        (new NoTaxYearsAvailable, OK, "transferor.no-eligible-years"),
        (new NoTaxYearsForTransferor, INTERNAL_SERVER_ERROR, ""),
        (new CacheTransferorInRelationship, OK, "title.transfer-in-place"),
        (new NoTaxYearsSelected, OK, "title.other-ways"),
        (new Exception, INTERNAL_SERVER_ERROR, "technical.issue.heading")
      )
      for ((error, responseStatus, message) <- data) {
        s"an $error has been thrown" in {
          val result = HandleError.apply(HeaderCarrier(), authRequest, messages, instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])(error)
          status(result) shouldBe responseStatus
          val doc = Jsoup.parse(contentAsString(result))
          doc.text() should include(messagesApi(message))
        }
      }
    }
  }

  val messages = messagesApi.preferred(Seq(Lang(Locale.ENGLISH)))
}
