/*
 * Copyright 2020 HM Revenue & Customs
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
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers._
import test_utils.TestData.Ninos
import test_utils.data.RecipientRecordData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import utils.BaseTest

import scala.concurrent.Future

class TransferServiceTest extends BaseTest {

  val service: TransferService = new TransferService {
    override val cachingService: CachingService = mock[CachingService]
    override val timeService: TimeService = mock[TimeService]
    override val marriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
    override val customAuditConnector: AuditConnector = mock[AuditConnector]
    override val applicationService: ApplicationService = mock[ApplicationService]
  }

  val nino = Nino(Ninos.nino1)
  val recipientData = RegistrationFormInput("", "", Gender("F"), nino, LocalDate.now())
  val relationshipRecord = RelationshipRecord("Recipient", "", "19960327", None, None, "", "")

  "isRecipientEligible" should {
    "return true" when {
      "checkRecipientEligible is true" in {
        val response = GetRelationshipResponse(Some(RecipientRecordData.userRecord), None, ResponseStatus("OK"))

        when(service.cachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(service.applicationService.canApplyForMarriageAllowance(Some(List(relationshipRecord)), Some(relationshipRecord)))
          .thenReturn(true)
        when(service.marriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(HttpResponse(OK, responseJson = Some(Json.toJson(response))))
        when(service.timeService.getValidYearsApplyMAPreviousYears(any()))
          .thenReturn(Nil)
        when(service.cachingService.saveRecipientRecord(RecipientRecordData.userRecord, recipientData, Nil))
          .thenReturn(RecipientRecordData.userRecord)

        val result = service.isRecipientEligible(nino, recipientData)
        await(result) shouldBe true
      }
    }

    "throw an error" when {
      "recipient is not returned" in {
        val response = GetRelationshipResponse(None, None, ResponseStatus("OK"))
        when(service.cachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(service.applicationService.canApplyForMarriageAllowance(Some(List(relationshipRecord)), Some(relationshipRecord)))
          .thenReturn(true)
        when(service.marriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(HttpResponse(OK, responseJson = Some(Json.toJson(response))))

        intercept[RecipientNotFound](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the cache returns no data" in {
        when(service.cachingService.getCachedDataForEligibilityCheck)
          .thenReturn(None)

        intercept[CacheMissingTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the active relationship record is not returned" in {
        when(service.cachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, None, Some(List(relationshipRecord)), None)))

        intercept[NoTaxYearsForTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the historic relationship record is not returned" in {
        when(service.cachingService.getCachedDataForEligibilityCheck)
          .thenReturn(Some(EligibilityCheckCacheData(None, None, Some(relationshipRecord), None, None)))

        intercept[NoTaxYearsForTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

    }
  }

  "getCurrentAndPreviousYearsEligibility" should {

    "return a CurrentAndPreviousYearsEligibility" in {
      val currentYear = 2019
      val recipientRecord = RecipientRecord(mock[UserRecord], mock[RegistrationFormInput], List(TaxYear(currentYear)))
      when(service.cachingService.getRecipientRecord).thenReturn(Future.successful(Some(recipientRecord)))
      when(service.timeService.getCurrentTaxYear).thenReturn(currentYear)

      val result = await(service.getCurrentAndPreviousYearsEligibility)
      result shouldBe a[CurrentAndPreviousYearsEligibility]


    }

    "throw an error" when {

      "no CurrentAndPreviousYearsEligibility is returned" in {
        when(service.cachingService.getRecipientRecord).thenReturn(Future.successful(None))
        intercept[CacheMissingRecipient](await(service.getCurrentAndPreviousYearsEligibility))
      }

    }
  }

}
