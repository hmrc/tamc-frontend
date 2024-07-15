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

package services

import config.ApplicationConfig
import connectors.MarriageAllowanceConnector
import errors._
import models._
import org.junit.Assert.{assertEquals, assertTrue}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import test_utils.TestData.{Cids, Ninos}
import test_utils.data.RecipientRecordData
import test_utils.data.RecipientRecordData.citizenName
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import utils.BaseTest
import services.CacheService._

import java.time.LocalDate
import scala.concurrent.Future

class TransferServiceTest extends BaseTest with BeforeAndAfterEach {

  val mockCachingService: CachingServiceOld = mock[CachingServiceOld]
  val mockApplicationService: ApplicationService = mock[ApplicationService]
  val mockMarriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
  val mockTimeService: TimeService = mock[TimeService]
  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]

  val dateOfMarriage: LocalDate = LocalDate.now()
  val nino: Nino = Nino(Ninos.nino1)
  val recipientData: RegistrationFormInput = RegistrationFormInput("First", "Last", Gender("F"), nino, dateOfMarriage)
  val relationshipRecord: RelationshipRecord = RelationshipRecord("Recipient", "20150531235901", "19960327", None, None, "123456789123", "20150531235901")

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("SessionId")))
  implicit val request = FakeRequest()

  val appConf: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

  appConf.currentTaxYear() -> {}

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[CachingServiceOld].toInstance(mockCachingService),
      bind[ApplicationService].toInstance(mockApplicationService),
      bind[MarriageAllowanceConnector].toInstance(mockMarriageAllowanceConnector),
      bind[TimeService].toInstance(mockTimeService),
      bind[AuditConnector].toInstance(mockAuditConnector)
    ).build()


  val service: TransferService = app.injector.instanceOf[TransferService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCachingService)
    reset(mockApplicationService)
    reset(mockMarriageAllowanceConnector)
    reset(mockTimeService)
    reset(mockAuditConnector)
  }

  "isRecipientEligible" should {
    "return true" when {
      "checkRecipientEligible is true" in {

        val response = GetRelationshipResponse(Some(RecipientRecordData.userRecord), None, ResponseStatus("OK"))
        val recipientRecord = RecipientRecord(RecipientRecordData.userRecord, recipientData, Nil)

        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(mockApplicationService.canApplyForMarriageAllowance(any(), any(), any()))
          .thenReturn(true)
        when(mockTimeService.getTaxYearForDate(recipientData.dateOfMarriage))
          .thenReturn(2020)
        when(mockMarriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(Right(response))
        when(mockTimeService.getValidYearsApplyMAPreviousYears(any()))
          .thenReturn(Nil)

        when(mockCachingService.put(CACHE_RECIPIENT_RECORD, recipientRecord))
          .thenReturn(recipientRecord)

        val result = service.isRecipientEligible(nino, recipientData)
        await(result) shouldBe true
      }
    }

    "throw an error" when {
      "recipient is not returned" in {
        val response = MarriageAllowanceError(ResponseStatus("TAMC:ERROR:RECIPIENT-NOT-FOUND"))
        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(mockApplicationService.canApplyForMarriageAllowance(any(), any(), any()))
          .thenReturn(true)
        when(mockMarriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(Left(response))

        intercept[RecipientNotFound](await(service.isRecipientEligible(nino, recipientData)))
      }

      "transferor deceased" in {
        val response = MarriageAllowanceError(ResponseStatus("TAMC:ERROR:TRANSFERER-DECEASED"))
        when(mockCachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(mockApplicationService.canApplyForMarriageAllowance(any(), any(), any()))
          .thenReturn(true)
        when(mockMarriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(Left(response))

        intercept[TransferorDeceased](await(service.isRecipientEligible(nino, recipientData)))
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

      when(mockCachingService.get[RecipientRecord](CACHE_RECIPIENT_RECORD)).thenReturn(Future.successful(Some(recipientRecord)))
      when(mockTimeService.getCurrentTaxYear).thenReturn(currentYear)

      val result = await(service.getCurrentAndPreviousYearsEligibility)
      result shouldBe a[CurrentAndPreviousYearsEligibility]
    }

    "throw an error" when {

      "no CurrentAndPreviousYearsEligibility is returned" in {
        when(mockCachingService.get[RecipientRecord](CACHE_RECIPIENT_RECORD)).thenReturn(Future.successful(None))
        intercept[CacheMissingRecipient](await(service.getCurrentAndPreviousYearsEligibility))
      }
    }
  }

  "createRelationship" should {
    "return a notificationRecord" in {
      val userRecord = UserRecord(11111111L,"timestamp")

      when(mockCachingService.getUserAnswersCachedData(any()))
        .thenReturn(Future.successful(Some(UserAnswersCacheData(
            Some(userRecord),
            Some(RecipientRecord(userRecord, RegistrationFormInput("firstName", "surname", Gender("M"),nino,LocalDate.now))),
            Some(NotificationRecord(EmailAddress("email@email.com"))), selectedYears = Some(List(2020, 2021))))))

      when(mockCachingService.put[Boolean](ArgumentMatchers.eq(CACHE_LOCKED_CREATE), ArgumentMatchers.eq(true))(any(),any())).thenReturn(Future.successful(true))

      when(mockAuditConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))

      when(mockMarriageAllowanceConnector.createRelationship(any(), any())(any(), any())).thenReturn(
        Future.successful(
          Right(Some(CreateRelationshipResponse(ResponseStatus("OK"))))
        )
      )

      val result = await(service.createRelationship(nino))

      result shouldBe NotificationRecord(EmailAddress("email@email.com"))

    }
  }

  "getFinishedData" should {
    "return NotificationRecord when notification is present in cacheData" in {
      val notificationRecord = NotificationRecord(EmailAddress("email@email.com"))
      val cacheData = UserAnswersCacheData(None, None, Some(notificationRecord), Some(true))

      when(mockCachingService.getUserAnswersCachedData(any())).thenReturn(Future.successful(Some(cacheData)))

      val result = await(service.getFinishedData(nino))

      result shouldBe notificationRecord
    }

    "return CacheCreateRequestNotSent Error" when {
      "cacheData is returned and no notificationRecord is present" in {
        val cacheData = UserAnswersCacheData(None, None, None, Some(true))

        when(mockCachingService.getUserAnswersCachedData(any())).thenReturn(Future.successful(Some(cacheData)))

        intercept[CacheCreateRequestNotSent]{
          await(service.getFinishedData(nino))
        }
      }

      "cacheData is returned and  relationshipCreated is false" in {
        val cacheData = UserAnswersCacheData(None, None, Some(NotificationRecord(EmailAddress("email@email.com"))), Some(false))

        when(mockCachingService.getUserAnswersCachedData(any())).thenReturn(Future.successful(Some(cacheData)))

        intercept[CacheCreateRequestNotSent]{
          await(service.getFinishedData(nino))
        }
      }

      "cacheDate is returned and relationshipCreated is None" in {
        val cacheData = UserAnswersCacheData(None, None, Some(NotificationRecord(EmailAddress("email@email.com"))))

        when(mockCachingService.getUserAnswersCachedData(any())).thenReturn(Future.successful(Some(cacheData)))

        intercept[CacheCreateRequestNotSent]{
          await(service.getFinishedData(nino))
        }
      }

      "no cacheData is returned" in {
        when(mockCachingService.getUserAnswersCachedData(any())).thenReturn(Future.successful(None))

        intercept[CacheCreateRequestNotSent]{
          await(service.getFinishedData(nino))
        }
      }
    }
  }

  "getCachedData test " should {
    "return a value " in {
      val rdfi = RecipientDetailsFormInput("Jain", "Doe", Gender("F"), nino)
      val recipientRecord = RecipientRecord(mock[UserRecord], mock[RegistrationFormInput], List(TaxYear(2019)))
      val cacheData = UserAnswersCacheData(None, Some(recipientRecord), Some(NotificationRecord(EmailAddress("email@email.com"))), None, None, Option(rdfi) )
      when(mockCachingService.getUserAnswersCachedData(any())).thenReturn(Future.successful(Some(cacheData)))

      val result = service.getRecipientDetailsFormData()
      await(result)
      assertTrue(result.nino == nino)
    }
  }

  "getConfirmationData test " should {
    "return a value " in {
      val rdfi = RecipientDetailsFormInput("Test", "User", Gender("F"), nino)
      val recipientRecord = RecipientRecord(mock[UserRecord], mock[RegistrationFormInput], List(TaxYear(2022)))
      val cacheData = UserAnswersCacheData(
        transferor = Some(RecipientRecordData.userRecord),
        recipient = Some(recipientRecord),
        notification = Some(NotificationRecord(EmailAddress("email@email.com"))),
        selectedYears = Some(List(2021, 2022)),
        recipientDetailsFormData = Option(rdfi),
        dateOfMarriage = Some( DateOfMarriageFormInput(LocalDate.of(2019, 6, 6))))
      when(mockCachingService.getUserAnswersCachedData).thenReturn(Future.successful(Some(cacheData)))

      val result = service.getConfirmationData(nino)
      await(result)
      assertEquals(CitizenName(Option("Test"), Option("User")).fullName, result.transferorFullName.get.fullName)
    }

    "update cache" in {
      val rdfi = RecipientDetailsFormInput("Test", "User", Gender("F"), nino)
      val recipientRecord = RecipientRecord(mock[UserRecord], mock[RegistrationFormInput], List(TaxYear(2022)))
      val cacheData = UserAnswersCacheData(
        transferor = None,
        recipient = Some(recipientRecord),
        notification = Some(NotificationRecord(EmailAddress("email@email.com"))),
        selectedYears = Some(List(2021, 2022)),
        recipientDetailsFormData = Option(rdfi),
        dateOfMarriage = Some( DateOfMarriageFormInput(LocalDate.of(2019, 6, 6))))
      val recordList = RelationshipRecordList(Seq.empty, Some(LoggedInUserInfo(Cids.cid1, "2015", Some(true), Some(citizenName))))
      val userRecord: UserRecord = UserRecord(Cids.cid1, "2015", None, Some(citizenName))

      when(mockCachingService.getUserAnswersCachedData).thenReturn(
        Future.successful(Some(cacheData)),
        Future.successful(Some(cacheData.copy(transferor = Some(userRecord))))
      )
      when(mockMarriageAllowanceConnector.listRelationship(nino)).thenReturn(recordList)
      when(mockCachingService.put[UserRecord](
        ArgumentMatchers.eq(CACHE_TRANSFEROR_RECORD),
        ArgumentMatchers.eq(userRecord))(any(), any())).thenReturn(Future.successful(userRecord))

      val result = service.getConfirmationData(nino)
      await(result)
      assertEquals(CitizenName(Option("Test"), Option("User")).fullName, result.transferorFullName.get.fullName)
      verify(mockMarriageAllowanceConnector, times(1)).listRelationship(ArgumentMatchers.eq(nino))(any(),any())
      verify(mockCachingService, times(1)).put[UserRecord](any(),any())(any(),any())
      verify(mockCachingService, times(2)).getUserAnswersCachedData(any())
    }
  }
}
