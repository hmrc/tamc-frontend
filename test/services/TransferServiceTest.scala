/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import connectors.MarriageAllowanceConnector
import errors.{CacheMissingRecipient, CacheMissingTransferor, NoTaxYearsForTransferor, RecipientNotFound}
import models._
import java.time.LocalDate

import org.mockito.Mockito.{reset, when}
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import test_utils.TestData.Ninos
import test_utils.data.RecipientRecordData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.BaseTest

import scala.concurrent.Future

class TransferServiceTest extends BaseTest with BeforeAndAfterEach {

  val mockCachingService: CachingService = mock[CachingService]
  val mockApplicationService: ApplicationService = mock[ApplicationService]
  val mockMarriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
  val mockTimeService: TimeService = mock[TimeService]

  override def fakeApplication: Application = GuiceApplicationBuilder()
    .overrides(
      bind[CachingService].toInstance(mockCachingService),
      bind[ApplicationService].toInstance(mockApplicationService),
      bind[MarriageAllowanceConnector].toInstance(mockMarriageAllowanceConnector),
      bind[TimeService].toInstance(mockTimeService)
    ).build()

  val nino: Nino = Nino(Ninos.nino1)
  val recipientData: RegistrationFormInput = RegistrationFormInput("First", "Last", Gender("F"), nino, LocalDate.now())
  val relationshipRecord: RelationshipRecord = RelationshipRecord("Recipient", "20150531235901", "19960327", None, None, "123456789123", "20150531235901")
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("SessionId")))

  val service: TransferService = app.injector.instanceOf[TransferService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCachingService, mockApplicationService, mockMarriageAllowanceConnector, mockTimeService)
  }

  "isRecipientEligible" should {
    "return true" when {
      "checkRecipientEligible is true" in {

        val response = GetRelationshipResponse(Some(RecipientRecordData.userRecord), None, ResponseStatus("OK"))

        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(mockApplicationService.canApplyForMarriageAllowance(any(), any(), any()))
          .thenReturn(true)
        when(mockTimeService.getTaxYearForDate(recipientData.dateOfMarriage))
          .thenReturn(2020)
        when(mockMarriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(HttpResponse(OK, responseJson = Some(Json.toJson(response))))
        when(mockTimeService.getValidYearsApplyMAPreviousYears(any()))
          .thenReturn(Nil)
        when(mockCachingService.saveRecipientRecord(RecipientRecordData.userRecord, recipientData, Nil))
          .thenReturn(RecipientRecordData.userRecord)

        val result = service.isRecipientEligible(nino, recipientData)
        await(result) shouldBe true
      }
    }

    "throw an error" when {
      "recipient is not returned" in {
        val response = GetRelationshipResponse(None, None, ResponseStatus("OK"))
        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(mockApplicationService.canApplyForMarriageAllowance(Some(List(relationshipRecord)), Some(relationshipRecord)))
          .thenReturn(true)
        when(mockMarriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(HttpResponse(OK, responseJson = Some(Json.toJson(response))))

        intercept[RecipientNotFound](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the cache returns no data" in {
        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(None)

        intercept[CacheMissingTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the active relationship record is not returned" in {
        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, None, Some(List(relationshipRecord)), None)))

        intercept[NoTaxYearsForTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the historic relationship record is not returned" in {
        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), None, None)))

        intercept[NoTaxYearsForTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }
    }
  }

  "getCurrentAndPreviousYearsEligibility" should {

    "return a CurrentAndPreviousYearsEligibility" in {
      val currentYear = 2019
      val recipientRecord = RecipientRecord(mock[UserRecord], mock[RegistrationFormInput], List(TaxYear(currentYear)))
      when(mockCachingService.getRecipientRecord).thenReturn(Future.successful(Some(recipientRecord)))
      when(mockTimeService.getCurrentTaxYear).thenReturn(currentYear)

      val result = await(service.getCurrentAndPreviousYearsEligibility)
      result shouldBe a[CurrentAndPreviousYearsEligibility]
    }

    "throw an error" when {

      "no CurrentAndPreviousYearsEligibility is returned" in {
        when(mockCachingService.getRecipientRecord).thenReturn(Future.successful(None))
        intercept[CacheMissingRecipient](await(service.getCurrentAndPreviousYearsEligibility))
      }
    }
  }

}
