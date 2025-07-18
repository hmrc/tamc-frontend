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

import config.ApplicationConfig
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import errors._
import helpers.FakePertaxAuthAction
import models._
import models.auth.AuthenticatedUserRequest
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time

import java.time.LocalDate

class TransferErrorHandlerTest extends ControllerBaseTest {

  val currentTaxYear: Int = time.TaxYear.current.startYear
  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]
  val notificationRecord: NotificationRecord = NotificationRecord(EmailAddress("test@test.com"))
  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[CachingService].toInstance(mockCachingService),
      bind[TimeService].toInstance(mockTimeService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    )
    .build()

  def helper: TransferErrorHandler =
    app.injector.instanceOf[TransferErrorHandler]

  when(mockTimeService.getCurrentDate) `thenReturn` LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) `thenReturn` currentTaxYear

  "handleError" should {
    val authRequest: AuthenticatedUserRequest[?] = AuthenticatedUserRequest(
      request,
      Some(ConfidenceLevel.L200),
      isSA = false,
      Some("GovernmentGateway"),
      Nino(TestData.Ninos.nino1)
    )
    "redirect" when {
      val data = List(
        (new TransferorNotFound, "/marriage-allowance-application/marriage-allowance-details-missing"),
        (new CacheMissingTransferor, "/marriage-allowance-application/history"),
        (new CacheMissingRecipient, "/marriage-allowance-application/history"),
        (new CacheMissingEmail, "/marriage-allowance-application/confirm-your-email"),
        (new CacheRelationshipAlreadyCreated, "/marriage-allowance-application/history"),
        (new CacheCreateRequestNotSent, "/marriage-allowance-application/history"),
        (new RelationshipMightBeCreated, "/marriage-allowance-application/history"),
        (new TransferorDeceased, "/marriage-allowance-application/you-cannot-use-this-service"),
        (new RecipientDeceased, "/marriage-allowance-application/you-cannot-use-this-service")
      )
      for ((error, redirectUrl) <- data)
        s"an $error has been thrown" in {
          val result = helper.handleError(authRequest)(error)
          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
    }

    "handle an error" when {
      val data = List(
        (new TransferorNotFound, SEE_OTHER, "transferor.not.found"),
        (new RecipientNotFound, OK, "recipient.not.found.para1"),
        (new CacheRecipientInRelationship, INTERNAL_SERVER_ERROR, "recipient.has.relationship.para1"),
        (new CannotCreateRelationship, INTERNAL_SERVER_ERROR, "create.relationship.failure"),
        (new NoTaxYearsAvailable, OK, "transferor.no-eligible-years"),
        (new NoTaxYearsForTransferor, OK, ""),
        (new CacheTransferorInRelationship, OK, "transferor.has.relationship"),
        (new NoTaxYearsSelected, OK, "pages.noyears.h1"),
        (new Exception, INTERNAL_SERVER_ERROR, "technical.issue.heading")
      )
      for ((error, responseStatus, message) <- data)
        s"an $error has been thrown" in {
          val result = helper.handleError(authRequest)(error)
          status(result) shouldBe responseStatus
        }
    }
  }
}
