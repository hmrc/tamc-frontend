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
import controllers.ControllerBaseSpec
import errors.ErrorResponseStatus._
import errors.{BadFetchRequest, CacheMissingDivorceDate, CacheMissingRelationshipRecords, CannotUpdateRelationship, CitizenNotFound, RecipientNotFound, TransferorNotFound}
import forms.coc.CheckClaimOrCancelDecisionForm
import models.{ConfirmationUpdateAnswers, ConfirmationUpdateAnswersCacheData, Divorce, EndMarriageAllowanceReason, MarriageAllowanceEndingDates, Recipient, RelationshipRecordList, RelationshipRecords, ResponseStatus, Transferor, UpdateRelationshipCacheDataTemp, UpdateRelationshipResponse, UserRecord}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.time.TaxYear
import viewModels.FinishedUpdateViewModel

import scala.concurrent.Future

class UpdateRelationshipServiceTest extends ControllerBaseSpec {

  val nino: Nino = new Generator().nextNino
  val recordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), Some(loggedInUser))
  val date = LocalDate.now()
  val OK = 200
  val headers = Map("headers" -> Seq(""))


  val service: UpdateRelationshipService = new UpdateRelationshipService {
      override val marriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
      override val customAuditConnector: AuditConnector = mock[AuditConnector]
      override val cachingService: CachingService = mock[CachingService]
  }

  "retrieveRelationshipRecords" should {
    "retrive RealtionshipRecord" in {
      when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(recordList))

     val result = await(service.retrieveRelationshipRecords(nino))

      result shouldBe RelationshipRecords(activeRecipientRelationshipRecord, Seq(), loggedInUser)
    }

    "throw Runtime Exception" when {
      "a TransferorNotFound Error is returned" in {
        when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.failed(TransferorNotFound()))

        val result = intercept[TransferorNotFound](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe TransferorNotFound()
      }

      "a CitizenNotFoundError is returned" in {
        when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.failed(CitizenNotFound()))

        val result = intercept[CitizenNotFound](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe CitizenNotFound()
      }

      "a BadFetchRequest is returned" in {
        when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.failed(BadFetchRequest()))

        val result = intercept[BadFetchRequest](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe BadFetchRequest()
      }
    }
  }

  //TODO Figure out what the actual point of this is... Then write the rest of the tests!
  "saveRelationshipStatus" ignore {
    "return RelationshipRecord" in {
      ???
    }
  }

  "getCheckClaimOrCancelDecision" should {
    "return a String when value is found in cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any())).thenReturn(Future.successful(Some("You found a poke ball")))

      val result = await(service.getCheckClaimOrCancelDecision)

      result shouldBe Some("You found a poke ball")
    }

    "return a None when value is not found in cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getCheckClaimOrCancelDecision)

      result shouldBe None
    }
  }

  "getMakeChangesDecision" should {
    "return EndMarriageAllowanceReason when value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[EndMarriageAllowanceReason](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Divorce)))

      val result = await(service.getMakeChangesDecision)

      result shouldBe Some(Divorce)
    }

    "return None when no value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[EndMarriageAllowanceReason](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getMakeChangesDecision)

      result shouldBe None
    }
  }

  "saveMakeChangeDecision" should {
    "return EndMarriageAllowaneReason" in {
      when(service.cachingService.cacheValue[EndMarriageAllowanceReason](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Divorce))

      val endReason = "Divorce"

      val result = await(service.saveMakeChangeDecision(endReason))

      result shouldBe Divorce
    }
  }

  "getDivorceDate" should {
    "return LocalDate when value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[LocalDate](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(date)))

      val result = await(service.getDivorceDate)

      result shouldBe Some(date)
    }

    "return None when no value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[LocalDate](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getDivorceDate)

      result shouldBe None
    }
  }

  "getEmailAddress" should {
    "return String when value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some("email@email.com")))

      val result = await(service.getEmailAddress)

      result shouldBe Some("email@email.com")
    }

    "return None when no value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getEmailAddress)

      result shouldBe None
    }
  }

  "getInformationForConfirmation" should {
    "return String when value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some("email@email.com")))

      when(service.cachingService.getRelationshipRecords(any(), any()))
        .thenReturn(Future.successful(Some(RelationshipRecords(recordList))))

      val result = await(service.getInformationForConfirmation)

      result shouldBe FinishedUpdateViewModel(EmailAddress("email@email.com"), Recipient)
    }

    "return RuntimeException when no email returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = intercept[RuntimeException](await(service.getInformationForConfirmation))

      result.getLocalizedMessage shouldBe "Email not found in cache"
    }

    "return CacheMissingRelationshipRecords when no RelationshipRecords returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some("email@email.com")))

      when(service.cachingService.getRelationshipRecords(any(), any()))
        .thenReturn(Future.successful(None))

      val result = intercept[CacheMissingRelationshipRecords](await(service.getInformationForConfirmation))

      result shouldBe CacheMissingRelationshipRecords()
    }
  }

  "saveEmailAddress" should {
    "return a String" in {
      when(service.cachingService.cacheValue[String](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful("email@email.com"))

      val result = await(service.saveEmailAddress("email@email.com"))

      result shouldBe "email@email.com"
    }
  }

  "getDataForDivorceExplanation" should {
    "return Role and LocalDate" in {
      val relationshipRecords = RelationshipRecords(recordList)

      when(service.cachingService.getRelationshipRecords(any(), any()))
        .thenReturn(Future.successful(Some(relationshipRecords)))

      when(service.cachingService.fetchAndGetEntry[LocalDate](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(date)))

      val result = await(service.getDataForDivorceExplanation)

      result._1 shouldBe Recipient
      result._2 shouldBe date
    }

    "CacheMissingDivorceDate Error when no value returned from cache" in {
      val relationshipRecords = RelationshipRecords(recordList)

      when(service.cachingService.getRelationshipRecords(any(), any()))
        .thenReturn(Future.successful(Some(relationshipRecords)))

      when(service.cachingService.fetchAndGetEntry[LocalDate](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = intercept[CacheMissingDivorceDate](await(service.getDataForDivorceExplanation))

      result shouldBe CacheMissingDivorceDate()
    }
  }

  "saveDivorceDate" should {
    "return a LocalDate" in {
      when(service.cachingService.cacheValue[LocalDate](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(date))

      val result = await(service.saveDivorceDate(date))

      result shouldBe date
    }
  }

  "saveCheckClaimOrCancelDecision" should {
    "return a String" in {
      when(service.cachingService.cacheValue[String](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful("stopMarriageAllowance"))

      val result = await(service.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.StopMarriageAllowance))

      result shouldBe "stopMarriageAllowance"
    }
  }

  "updateRelationship" should {
    "return a unit" in {
      val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus("OK")))
      val httpResponse = HttpResponse(OK, Some(json), headers)


      when(service.cachingService.getUpdateRelationshipCachedDataTemp(any(), any()))
        .thenReturn(Future.successful(UpdateRelationshipCacheDataTemp(RelationshipRecords(recordList), "email@email.com", "Divorce", date)))

      when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
        .thenReturn(Future.successful(httpResponse))

      val result = await(service.updateRelationship(nino))

      result shouldBe ()
    }

    "throw relevant error" when {
      "CannotUpdateRelationship error is returned from MarriageAllowanceConnector" in {
        val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus(CANNOT_UPDATE_RELATIONSHIP)))
        val httpResponse = HttpResponse(OK, Some(json), headers)

        when(service.cachingService.getUpdateRelationshipCachedDataTemp(any(), any()))
          .thenReturn(Future.successful(UpdateRelationshipCacheDataTemp(RelationshipRecords(recordList), "email@email.com", "Divorce", date)))

        when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = intercept[CannotUpdateRelationship](await(service.updateRelationship(nino)))

        result shouldBe CannotUpdateRelationship()
      }

      "RecipientNotFound error is returned from MarriageAllowanceConnector" in {
        val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST)))
        val httpResponse = HttpResponse(OK, Some(json), headers)

        when(service.cachingService.getUpdateRelationshipCachedDataTemp(any(), any()))
          .thenReturn(Future.successful(UpdateRelationshipCacheDataTemp(RelationshipRecords(recordList), "email@email.com", "Divorce", date)))

        when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = intercept[RecipientNotFound](await(service.updateRelationship(nino)))

        result shouldBe RecipientNotFound()
      }

      "RuntimeException is returned from the caching service" in {
        val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST)))
        val httpResponse = HttpResponse(OK, Some(json), headers)

        when(service.cachingService.getUpdateRelationshipCachedDataTemp(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Failed to retrieve cacheMap")))

        when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = intercept[RuntimeException](await(service.updateRelationship(nino)))

        result.getMessage shouldBe "Failed to retrieve cacheMap"
      }
    }

    "getConfirmationUpdateAnswers" should {
      "return Confirmation update answers" in {
        val relationshipRecords = Some(RelationshipRecords(recordList))
        val endDates = MarriageAllowanceEndingDates(TaxYear.current.starts, TaxYear.current.finishes)
        val cacheData = ConfirmationUpdateAnswersCacheData(relationshipRecords, Some(date), Some("email@email.com"), Some(endDates))

        when(service.cachingService.getConfirmationAnswers(any(), any()))
          .thenReturn(Future.successful(cacheData))

        val result = await(service.getConfirmationUpdateAnswers)

        result shouldBe ConfirmationUpdateAnswers("Test User", Some(LocalDate.now()), "email@email.com", endDates)
      }

      "return RuntimeException when cacheMap not found and returns RuntimeException" in {
        when(service.cachingService.getConfirmationAnswers(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Failed to retrieve cacheMap")))

        val result = intercept[RuntimeException](await(service.getConfirmationUpdateAnswers))

        result.getMessage shouldBe "Failed to retrieve cacheMap"
      }
    }

    "getMAEndingDatesForCancelation" should {
      "return dates" in {
        val result = await(service.getMAEndingDatesForCancelation)

        result shouldBe MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
      }
    }

    "getMAEndingDatesForDivorce" should {
      "return the end of the previous tax year as endDate" when {
        "role is Recipient and endDate is not in current tax year" in {
          val role = Recipient
          val divorceDate = new LocalDate(TaxYear.current.previous.startYear, 7, 7)
          val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)

          val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

          result shouldBe expectedDates
        }

        "role is Transferor and endDate is in current tax year" in {
          val role = Transferor
          val divorceDate = new LocalDate(TaxYear.current.finishYear, 1, 1)
          val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)

          val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

          result shouldBe expectedDates
        }
      }

      "return the end of the current tax year as endDate" in {
        val role = Recipient
        val divorceDate = new LocalDate(TaxYear.current.startYear, 12, 31)
        val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)

        val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

        result shouldBe expectedDates
      }

      "return the endDate of the taxYear the divorceDate falls in" in {
        val role = Transferor
        val divorceDate = new LocalDate(TaxYear.current.previous.startYear, 1, 1)

        val expectedDates = MarriageAllowanceEndingDates(
          new LocalDate(TaxYear.current.previous.startYear, 4, 5),
          TaxYear.current.previous.starts)

        val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

        result shouldBe expectedDates

      }
    }

    "saveMarriageAllowanceEndingDates" should {
      "return MarriageAllowanceEndDate" in {
        val endingDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)

        when(service.cachingService.cacheValue[MarriageAllowanceEndingDates](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(endingDates))

        val result = await(service.saveMarriageAllowanceEndingDates(endingDates))

        result shouldBe MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
      }
    }

    "getRelationshipRecords" should {
      "when RelationshipRecords are present return RelationshipRecords" in {
        when(service.cachingService.getRelationshipRecords(any(), any()))
          .thenReturn(Some(RelationshipRecords(recordList)))

        val result = await(service.getRelationshipRecords)

        result shouldBe RelationshipRecords(activeRecipientRelationshipRecord, Seq(), loggedInUser)
      }

      "when RelationshipRecords are not present return CacheMissingRelationshipRecords Error" in {
        when(service.cachingService.getRelationshipRecords(any(), any()))
          .thenReturn(Future.successful(None))

        val result = intercept[CacheMissingRelationshipRecords](await(service.getRelationshipRecords))

        result shouldBe CacheMissingRelationshipRecords()
      }
    }

    "removeCache" should {
      "return Unit when cache has been dropped" in {
        when(service.cachingService.remove()(any(), any()))
          .thenReturn(Future.successful(HttpResponse(200, None, Map("" -> Seq("")))))

        val result = await(service.removeCache)

        result shouldBe ()
      }
    }


  }
}
