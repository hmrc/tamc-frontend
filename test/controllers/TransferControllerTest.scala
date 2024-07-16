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

package controllers

import config.ApplicationConfig
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import errors._
import helpers.FakePertaxAuthAction
import models._
import models.auth.AuthenticatedUserRequest
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData
import test_utils.TestData.Ninos
import test_utils.data.{ConfirmationModelData, RecipientRecordData}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.time
import utils.{ControllerBaseTest, MockAuthenticatedAction}

import java.time.LocalDate

class TransferControllerTest extends ControllerBaseTest {

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

  def controller: TransferController =
    app.injector.instanceOf[TransferController]

  when(mockTimeService.getCurrentDate) thenReturn LocalDate.now()
  when(mockTimeService.getCurrentTaxYear) thenReturn currentTaxYear

//  "transfer" should {
//    "return success" in {
//      val result = controller.transfer()(request)
//      status(result) shouldBe OK
//    }
//  }

//  "transferAction" should {
//    "return bad request" when {
//      "an invalid form is submitted" in {
//        val recipientDetails: RecipientDetailsFormInput =
//          RecipientDetailsFormInput("Test", "User", Gender("M"), Nino(Ninos.nino2))
//        when(mockCachingService.put[RecipientDetailsFormInput](ArgumentMatchers.eq(applicationConfig.CACHE_RECIPIENT_DETAILS), ArgumentMatchers.eq(recipientDetails))(any(), any(), any(), any()))
//          .thenReturn(recipientDetails)
//        val result = controller.transferAction()(request)
//        status(result) shouldBe BAD_REQUEST
//      }
//    }

//    "redirect the user" when {
//      "a valid form is submitted" in {
//        val recipientDetails: RecipientDetailsFormInput =
//          RecipientDetailsFormInput("Test", "User", Gender("M"), Nino(Ninos.nino2))
//        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
//          "name"      -> "Test",
//          "last-name" -> "User",
//          "gender"    -> "M",
//          "nino"      -> Ninos.nino2
//        )
//        when(mockCachingService.put[RecipientDetailsFormInput](ArgumentMatchers.eq(applicationConfig.CACHE_RECIPIENT_DETAILS), ArgumentMatchers.eq(recipientDetails))(any(), any(), any(), any()))
//          .thenReturn(recipientDetails)
//        val result = controller.transferAction()(request)
//        status(result)           shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.dateOfMarriage().url)
//      }
//    }
//  }

  "dateOfMarriage" should {
    "return success" in {
      val result = controller.dateOfMarriage()(request)
      status(result) shouldBe OK
    }
  }

  "dateOfMarriageWithCy" should {
    "redirect to dateOfMarriage, with a welsh language setting" in {
      val result = await(controller.dateOfMarriageWithCy()(request))
      status(result)               shouldBe SEE_OTHER
      redirectLocation(result)     shouldBe Some(controllers.routes.TransferController.dateOfMarriage().url)
      result.newCookies.head.name  shouldBe "PLAY_LANG"
      result.newCookies.head.value shouldBe "cy"
    }
  }

  "dateOfMarriageWithEn" should {
    "redirect to dateOfMarriage, with an english language setting" in {
      val result = await(controller.dateOfMarriageWithEn()(request))
      status(result)               shouldBe SEE_OTHER
      redirectLocation(result)     shouldBe Some(controllers.routes.TransferController.dateOfMarriage().url)
      result.newCookies.head.name  shouldBe "PLAY_LANG"
      result.newCookies.head.value shouldBe "en"
    }
  }

  "dateOfMarriageAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val result = controller.dateOfMarriageAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect the user" when {
      "a valid form is submitted" in {
        val dateOfMarriageInput = DateOfMarriageFormInput(LocalDate.now().minusDays(1))
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "dateOfMarriage.year"  -> dateOfMarriageInput.dateOfMarriage.getYear.toString,
          "dateOfMarriage.month" -> s"0${dateOfMarriageInput.dateOfMarriage.getMonthValue.toString}",
          "dateOfMarriage.day"   -> dateOfMarriageInput.dateOfMarriage.getDayOfMonth.toString
        )
        val registrationFormInput =
          RegistrationFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1), dateOfMarriageInput.dateOfMarriage)

        when(mockCachingService.put[DateOfMarriageFormInput](ArgumentMatchers.eq(applicationConfig.CACHE_MARRIAGE_DATE), ArgumentMatchers.eq(dateOfMarriageInput))(any(), any(), any(), any()))
          .thenReturn(dateOfMarriageInput)

        when(mockTransferService.getRecipientDetailsFormData()(any(), any()))
          .thenReturn(RecipientDetailsFormInput("Test", "User", Gender("F"), Nino(Ninos.nino1)))
        when(
          mockTransferService.isRecipientEligible(
            ArgumentMatchers.eq(Nino(Ninos.nino1)),
            ArgumentMatchers.eq(registrationFormInput)
          )(any(), any(), any())
        )
          .thenReturn(true)
        val result = controller.dateOfMarriageAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.eligibleYears().url)
      }
    }
  }

  "eligibleYears" should {
    "return a success" when {
      "there are available tax years not including current year" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        val result = controller.eligibleYears()(request)
        status(result) shouldBe OK
      }

      "there are available tax years including current year" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              true,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(mockTimeService.getStartDateForTaxYear(any())).thenReturn(time.TaxYear.current.starts)
        val result = controller.eligibleYears()(request)
        status(result) shouldBe OK
      }
    }

    "throw an exception and recover user to error page" when {
      "available tax years is empty" in {
        when(mockTransferService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility(any(), any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              Nil,
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        val result = controller.eligibleYears()(request)
        status(result) shouldBe OK
      }
    }
  }

  "eligibleYearsAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "abc")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return success" when {
      "extra years is not empty and applyForCurrentYear is true" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "true")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(currentTaxYear)))(any(), any(), any()))
          .thenReturn(List(currentTaxYear))
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe OK
        verify(mockTransferService, times(1)).saveSelectedYears(ArgumentMatchers.eq(List(currentTaxYear)))(any(), any(), any())
      }

      "extra years is not empty and applyForCurrentYear is false" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "false")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(Nil))(any(), any(), any())).thenReturn(Nil)
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe OK
        verify(mockTransferService, times(1)).saveSelectedYears(ArgumentMatchers.eq(Nil))(any(), any(), any())
      }
    }

    "redirect the user" when {
      "extra years is empty and current year is unavailable" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "false")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              Nil,
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(Nil))(any(), any(), any())).thenReturn(Nil)
        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.confirmYourEmail().url)
      }

      "extra years is empty, current year is available but applyForCurrentYear is true" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "true")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              true,
              Nil,
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(List(currentTaxYear)))(any(), any(), any()))
          .thenReturn(List(currentTaxYear))
        val result = controller.eligibleYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.confirmYourEmail().url)
      }
    }

    "throw an exception and show an error page" when {
      "extra years is empty, current year is available and applyForCurrentYear is false" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("applyForCurrentYear" -> "false")
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              true,
              Nil,
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(mockTransferService.saveSelectedYears(ArgumentMatchers.eq(Nil))(any(), any(), any())).thenReturn(Nil)
        val result = controller.eligibleYearsAction()(request)
        status(result) shouldBe OK
      }
    }
  }

  "previousYears" should {
    "return success" when {
      "a successful call to transfer service is made" in {
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        val result = controller.previousYears()(request)
        status(result) shouldBe OK
      }
    }
  }

  "extraYearsAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        val result = controller.extraYearsAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "return success" when {
      "furtherYears is not empty" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "selectedYear"              -> "2015",
          "furtherYears"              -> "2014,2013",
          "yearAvailableForSelection" -> "2014"
        )
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(
          mockTransferService.updateSelectedYears(
            ArgumentMatchers.eq(RecipientRecordData.recipientRecord.availableTaxYears),
            ArgumentMatchers.eq(2015),
            ArgumentMatchers.eq(Some(2014))
          )(any(), any(), any())
        )
          .thenReturn(Nil)
        val result = controller.extraYearsAction()(request)
        status(result) shouldBe OK
      }
    }

    "redirect" when {
      "further years is empty" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody(
          "selectedYear"              -> "2015",
          "furtherYears"              -> "",
          "yearAvailableForSelection" -> "2014"
        )
        when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
          .thenReturn(
            CurrentAndPreviousYearsEligibility(
              false,
              List(TaxYear(2015)),
              RecipientRecordData.recipientRecord.data,
              RecipientRecordData.recipientRecord.availableTaxYears
            )
          )
        when(
          mockTransferService.updateSelectedYears(
            ArgumentMatchers.eq(RecipientRecordData.recipientRecord.availableTaxYears),
            ArgumentMatchers.eq(2015),
            ArgumentMatchers.eq(Some(2014))
          )(any(), any(), any())
        )
          .thenReturn(Nil)
        val result = controller.extraYearsAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.confirmYourEmail().url)
      }
    }
  }

  "confirmYourEmail" should {
    "return a success" when {
      "an email is recovered from the cache" in {
        val email = "test@test.com"
        when(mockCachingService.get[NotificationRecord](any())(any(), any(), any()))
          .thenReturn(Some(NotificationRecord(EmailAddress(email))))
        val result = controller.confirmYourEmail()(request)
        status(result) shouldBe OK
      }

      "no email is recovered from the cache" in {
        when(mockCachingService.get[NotificationRecord](any())(any(), any(), any()))
          .thenReturn(None)
        val result = controller.confirmYourEmail()(request)
        status(result) shouldBe OK
      }
    }
  }

  "confirmYourEmailAction" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("transferor-email" -> "not an email")
        val result = controller.confirmYourEmailAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withMethod("POST").withFormUrlEncodedBody("transferor-email" -> "test@test.com")
        when(mockTransferService.upsertTransferorNotification(ArgumentMatchers.eq(notificationRecord))(any(), any(), any()))
          .thenReturn(notificationRecord)
        val result = controller.confirmYourEmailAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.confirm().url)
      }
    }
  }

  "confirm" should {
    "return success" when {
      "successful future is returned from transfer service" in {
        when(mockTransferService.getConfirmationData(any())(any(), any(), any()))
          .thenReturn(ConfirmationModelData.confirmationModelData)
        val result = controller.confirm()(request)
        status(result) shouldBe OK
      }
    }
  }

  "confirmAction" should {
    "redirect" when {
      "a user is permanently authenticated" in {
        when(mockTransferService.createRelationship(any())(any(), any(), any(), any()))
          .thenReturn(notificationRecord)
        val result = controller.confirmAction()(request)
        status(result)           shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.finished().url)
        verify(mockTransferService, times(1)).createRelationship(any())(any(), any(), any(), any())
      }
    }
  }

  "finished" should {
    "return success" when {
      "A notification record is returned and cache is called" in {
        reset(mockCachingService)
        verify(mockCachingService, times(0)).clear()(any(), any(), any())

        when(mockTransferService.getFinishedData(any())(any(), any()))
          .thenReturn(notificationRecord)

        val result = controller.finished()(request)
        status(result) shouldBe OK

        verify(mockCachingService, times(1)).clear()(any(), any(), any())
      }
    }

    "return error" when {
      "error is thrown" in {
        reset(mockCachingService)
        verify(mockCachingService, times(0)).clear()(any(), any(), any())

        when(mockTransferService.getFinishedData(any())(any(), any()))
          .thenThrow(new IllegalArgumentException("123"))

        controller.finished()(request)

        verify(mockCachingService, times(0)).clear()(any(), any(), any())
      }
    }
  }

  "cannotUseService" should {
    "return success when call cannotUseService" in {
      val result = controller.cannotUseService(request)
      status(result) shouldBe OK
    }
  }

  "handleError" should {
    val authRequest: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(
      request,
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
      for ((error, redirectUrl) <- data)
        s"a $error has been thrown" in {
          val result = controller.handleError(authRequest)(error)
          status(result)           shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(redirectUrl)
        }
    }

    "handle an error" when {
      val data = List(
        (new TransferorNotFound, OK, "transferor.not.found"),
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
          val result = controller.handleError(authRequest)(error)
          status(result) shouldBe responseStatus
        }
    }
  }

}
