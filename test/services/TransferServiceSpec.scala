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

package services

import connectors.MarriageAllowanceConnector
import controllers.ControllerBaseSpec
import errors.{CacheMissingTransferor, NoTaxYearsForTransferor, RecipientNotFound}
import models._
import org.joda.time.LocalDate
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.test.Helpers._
import test_utils.TestData.Ninos
import test_utils.data.RecipientRecordData
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.audit.DefaultAuditConnector

import scala.concurrent.ExecutionContext.Implicits.global

class TransferServiceSpec extends ControllerBaseSpec {
     val cachingService: CachingService = mock[CachingService]
     val timeService: TimeService = mock[TimeService]
     val marriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
     val updateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
     val auditConnector = mock[DefaultAuditConnector]
  val service: TransferService = new TransferService(auditConnector,marriageAllowanceConnector,cachingService,timeService,updateRelationshipService)

  val nino = Nino(Ninos.nino1)
  val recipientData = RegistrationFormInput("", "", Gender("F"), nino, LocalDate.now())
  val relationshipRecord = RelationshipRecord("", "", "19960327", None, None, "", "")

  "isRecipientEligible" should {
    "return true" when {
      "checkRecipientEligible is true" in {
        val response = GetRelationshipResponse(Some(RecipientRecordData.userRecord), None, ResponseStatus("OK"))

        when(cachingService.getUpdateRelationshipCachedData)
          .thenReturn(Some(UpdateRelationshipCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(updateRelationshipService.canApplyForMarriageAllowance(Some(List(relationshipRecord)), Some(relationshipRecord)))
          .thenReturn(true)
        when(marriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(HttpResponse(OK, responseJson = Some(Json.toJson(response))))
        when(cachingService.saveRecipientRecord(RecipientRecordData.userRecord, recipientData, Nil))
          .thenReturn(RecipientRecordData.userRecord)

        val result = service.isRecipientEligible(nino, recipientData)
        await(result) shouldBe true
      }
    }

    "throw an error" when {
      "recipient is not returned" in {
        val response = GetRelationshipResponse(None, None, ResponseStatus("OK"))
        when(cachingService.getUpdateRelationshipCachedData)
          .thenReturn(Some(UpdateRelationshipCacheData(None, None, Some(relationshipRecord), Some(List(relationshipRecord)), None)))
        when(updateRelationshipService.canApplyForMarriageAllowance(Some(List(relationshipRecord)), Some(relationshipRecord)))
          .thenReturn(true)
        when(marriageAllowanceConnector.getRecipientRelationship(nino, recipientData))
          .thenReturn(HttpResponse(OK, responseJson = Some(Json.toJson(response))))

        intercept[RecipientNotFound](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the cache returns no data" in {
        when(cachingService.getUpdateRelationshipCachedData)
          .thenReturn(None)

        intercept[CacheMissingTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the active relationship record is not returned" in {
        when(cachingService.getUpdateRelationshipCachedData)
          .thenReturn(Some(UpdateRelationshipCacheData(None, None, None, Some(List(relationshipRecord)), None)))

        intercept[NoTaxYearsForTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }

      "the historic relationship record is not returned" in {
        when(cachingService.getUpdateRelationshipCachedData)
          .thenReturn(Some(UpdateRelationshipCacheData(None, None, Some(relationshipRecord), None, None)))

        intercept[NoTaxYearsForTransferor](await(service.isRecipientEligible(nino, recipientData)))
      }
    }
  }

}
