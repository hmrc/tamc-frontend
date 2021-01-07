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

import config.ApplicationConfig
import connectors.MarriageAllowanceConnector
import errors.ErrorResponseStatus._
import errors._
import forms.coc.CheckClaimOrCancelDecisionForm
import models._
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import test_utils.data.RelationshipRecordData._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.time.TaxYear
import utils.BaseTest

import scala.concurrent.Future

class UpdateRelationshipServiceTest extends BaseTest {

  val nino: Nino = new Generator().nextNino
  val instanceIdentifier = 1
  val timeStamp = "20130101"
  val firstName = "First"
  val surname = "Surname"
  val recordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), Some(createLoggedInUserInfo()))
  val date = LocalDate.now()
  val OK = 200
  val headers = Map("headers" -> Seq(""))
  val email = "email@email.com"

  def createCachedData(records: RelationshipRecordList = recordList): UpdateRelationshipCacheData = {
    UpdateRelationshipCacheData(Some(RelationshipRecords(records)), Some(email), Some("Divorce"), Some(date))
  }

  def createLoggedInUserInfo(name: Option[CitizenName] = Some(CitizenName(Some(firstName), Some(surname)))): LoggedInUserInfo = {
    LoggedInUserInfo(instanceIdentifier, timeStamp, None, name)
  }

  val service: UpdateRelationshipService = new UpdateRelationshipService {
      override val marriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
      override val customAuditConnector: AuditConnector = mock[AuditConnector]
      override val cachingService: CachingService = mock[CachingService]
  }

  class UpdateRelationshipSetup(cacheData: UpdateRelationshipCacheData = createCachedData()) {

    val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus("OK")))
    val httpResponse = HttpResponse(OK, Some(json), headers)

    when(service.cachingService.getUpdateRelationshipCachedData(any(), any()))
      .thenReturn(Future.successful(cacheData))

    when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
      .thenReturn(Future.successful(httpResponse))
  }

  "retrieveRelationshipRecords" should {
    "retrive RealtionshipRecord" in {
      when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(recordList))

     val result = await(service.retrieveRelationshipRecords(nino))

      result shouldBe RelationshipRecords(activeRecipientRelationshipRecord, Seq(), createLoggedInUserInfo())
    }

    "throw Runtime Exception" when {
      "a NoPrimaryRecordError is returned" in {

        val noActiveRecordList = RelationshipRecordList(Seq(inactiveRelationshipRecord1), Some(createLoggedInUserInfo()))

        when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.successful(noActiveRecordList))

        val result = intercept[NoPrimaryRecordError](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe NoPrimaryRecordError()
      }

      "a MultipleActiveRecordError is returned" in {
        val multipleActiveRecordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord,
          activeTransferorRelationshipRecord2), Some(createLoggedInUserInfo()))

        when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.successful(multipleActiveRecordList))

        val result = intercept[MultipleActiveRecordError](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe MultipleActiveRecordError()
      }

      "a CitizenNotFound error is returned" in {
        val noCitizenDetailsList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), None)

        when(service.marriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.successful(noCitizenDetailsList))

        val result = intercept[CitizenNotFound](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe CitizenNotFound()
      }

    }
  }

  "saveRelationshipRecords" should {

    val loggedInUserInfo = createLoggedInUserInfo()

    val relationshipRecords = RelationshipRecords(activeRecipientRelationshipRecord, Seq.empty[RelationshipRecord],
      loggedInUserInfo)

    val userRecord = UserRecord(Some(loggedInUserInfo))

    "return the saved RelationRecords" in {
      when(service.cachingService.unlockCreateRelationship()(any(), any())).thenReturn(Future.successful(false))
      when(service.cachingService.saveTransferorRecord(ArgumentMatchers.eq(userRecord))(any(), any())).
        thenReturn(Future.successful(userRecord))
      when(service.cachingService.cacheValue[RelationshipRecords](ArgumentMatchers.eq(ApplicationConfig.CACHE_RELATIONSHIP_RECORDS),
        ArgumentMatchers.eq(relationshipRecords))(any(), any(), any(), any())).thenReturn(Future.successful(relationshipRecords))

      val result = await(service.saveRelationshipRecords(relationshipRecords))

      result shouldBe relationshipRecords
    }

    "throw an exception if there is an issue saving to the cache" in {

      val exception = new RuntimeException("error")

      when(service.cachingService.unlockCreateRelationship()(any(), any())).thenReturn(Future.successful(false))
      when(service.cachingService.saveTransferorRecord(ArgumentMatchers.eq(userRecord))(any(), any())).
        thenReturn(Future.successful(userRecord))
      when(service.cachingService.cacheValue[RelationshipRecords](ArgumentMatchers.eq(ApplicationConfig.CACHE_RELATIONSHIP_RECORDS),
        ArgumentMatchers.eq(relationshipRecords))(any(), any(), any(), any())).thenReturn(Future.failed(exception))

      val result = intercept[RuntimeException](await(service.saveRelationshipRecords(relationshipRecords)))

      result shouldBe exception
    }

  }

  "getCheckClaimOrCancelDecision" should {
    "return a String when value is found in cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any())).thenReturn(Future.successful(Some("Check Claim")))

      val result = await(service.getCheckClaimOrCancelDecision)

      result shouldBe Some("Check Claim")
    }

    "return a None when value is not found in cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getCheckClaimOrCancelDecision)

      result shouldBe None
    }
  }

  "getMakeChangesDecision" should {
    "return a cache value" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some("Divorce")))

      val result = await(service.getMakeChangesDecision)

      result shouldBe Some("Divorce")
    }

    "return None when no value returned from cache" in {
      when(service.cachingService.fetchAndGetEntry[String](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getMakeChangesDecision)

      result shouldBe None
    }
  }

  "saveMakeChangeDecision" should {
    "return a value from the cache" in {
      val endReason = "Divorce"

      when(service.cachingService.cacheValue[String](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(endReason))

      val result = await(service.saveMakeChangeDecision(endReason))

      result shouldBe endReason
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

  "saveEmailAddress" should {
    "return a String" in {
      when(service.cachingService.cacheValue[EmailAddress](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(EmailAddress("email@email.com")))

      val result = await(service.saveEmailAddress(EmailAddress("email@email.com")))

      result shouldBe EmailAddress("email@email.com")
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

    "construct and return the generated UpdateRelationshipRequestHolder" when {

      "valid data is returned from the cache" in new UpdateRelationshipSetup {

          val expectedUpdateTimeStamp = "20150531235901"
          val expectedRecipientInformation = RecipientInformation(instanceIdentifier.toString, timeStamp)
          val expectedTransferorInformation = TransferorInformation(expectedUpdateTimeStamp)
          val expectedRelationshipInformation = RelationshipInformation(expectedUpdateTimeStamp, "Divorce/Separation", date.toString("yyyyMMdd"))
          val expectedName = s"$firstName $surname"

          val expectedUpdateRelationshipRequest = UpdateRelationshipRequest(expectedRecipientInformation, expectedTransferorInformation,
                                                  expectedRelationshipInformation)
          val expectedUpdateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest(expectedName, EmailAddress(email),
                                                  "Recipient", welsh = false, isRetrospective = false)

          val expectedResult = UpdateRelationshipRequestHolder(expectedUpdateRelationshipRequest, expectedUpdateRelationshipNotificationRequest)

          val result = await(service.updateRelationship(nino))

          result shouldBe expectedResult

      }

      "different marriage allowance end reasons are provided" when {

        val endReasonsWithEnumerations = Seq(("Divorce", "Divorce/Separation"), ("Cancel", "Cancelled by Transferor"))

        endReasonsWithEnumerations foreach { reasonAndEnumeration =>

          val cacheData = UpdateRelationshipCacheData(Some(RelationshipRecords(recordList)), Some(email), Some(reasonAndEnumeration._1), Some(date))

          s"the reason is ${reasonAndEnumeration._1}" in new UpdateRelationshipSetup(cacheData) {

            val result = await(service.updateRelationship(nino))
            val desEndReason = result.request.relationship.relationshipEndReason

            desEndReason shouldBe reasonAndEnumeration._2
          }
        }
      }

      val recordsWithUnknownUser = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), Some(createLoggedInUserInfo(None)))

      "a users name is not known" in new UpdateRelationshipSetup(createCachedData(recordsWithUnknownUser)){

        val result = await(service.updateRelationship(nino))
        val userName = result.notification.full_name

        userName shouldBe "Unknown"
      }
    }

    "throw relevant error" when {

      val cacheData = UpdateRelationshipCacheData(Some(RelationshipRecords(recordList)), Some(email), Some("Earnings"), Some(date))

      "if an unsupported marriage allowance end reason is provided" in new UpdateRelationshipSetup(cacheData){
        a[DesEnumerationNotFound] shouldBe thrownBy(await(service.updateRelationship(nino)))
      }


      "CannotUpdateRelationship error is returned from MarriageAllowanceConnector" in {
        val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus(CANNOT_UPDATE_RELATIONSHIP)))
        val httpResponse = HttpResponse(OK, Some(json), headers)

        when(service.cachingService.getUpdateRelationshipCachedData(any(), any()))
          .thenReturn(Future.successful(createCachedData()))

        when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = intercept[CannotUpdateRelationship](await(service.updateRelationship(nino)))

        result shouldBe CannotUpdateRelationship()
      }

      "RecipientNotFound error is returned from MarriageAllowanceConnector" in {
        val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST)))
        val httpResponse = HttpResponse(OK, Some(json), headers)

        when(service.cachingService.getUpdateRelationshipCachedData(any(), any()))
          .thenReturn(Future.successful(createCachedData()))

        when(service.marriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(httpResponse))

        val result = intercept[RecipientNotFound](await(service.updateRelationship(nino)))

        result shouldBe RecipientNotFound()
      }

      "RuntimeException is returned from the caching service" in {
        val json: JsValue = Json.toJson(UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST)))
        val httpResponse = HttpResponse(OK, Some(json), headers)

        when(service.cachingService.getUpdateRelationshipCachedData(any(), any()))
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

        result shouldBe ConfirmationUpdateAnswers(createLoggedInUserInfo(), Some(LocalDate.now()), "email@email.com", endDates)
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

        result shouldBe RelationshipRecords(activeRecipientRelationshipRecord, Seq(), createLoggedInUserInfo())
      }

      "when RelationshipRecords are not present return CacheMissingRelationshipRecords Error" in {
        when(service.cachingService.getRelationshipRecords(any(), any()))
          .thenReturn(Future.successful(None))

        val result = intercept[CacheMissingRelationshipRecords](await(service.getRelationshipRecords))

        result shouldBe CacheMissingRelationshipRecords()
      }
    }

    "removeCache" should {
      "return the HTTPResponse when the cache has been dropped" in {

        val httpResponse = HttpResponse(200, None, Map("" -> Seq("")))
        when(service.cachingService.remove()(any(), any())).thenReturn(Future.successful(httpResponse))

        val result = await(service.removeCache)

        result shouldBe httpResponse
      }
    }


  }
}
