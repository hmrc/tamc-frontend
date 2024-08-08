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
import errors.ErrorResponseStatus._
import errors._
import forms.coc.CheckClaimOrCancelDecisionForm
import models._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import test_utils.data.RelationshipRecordData._
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, SystemLocalDate}
import services.CacheService._
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class UpdateRelationshipServiceTest extends BaseTest with BeforeAndAfterEach {

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
  lazy val localDate: LocalDate = instanceOf[SystemLocalDate].now()

  implicit val request: Request[AnyContent] = FakeRequest()

  def createCachedData(records: RelationshipRecordList = recordList): UpdateRelationshipCacheData = {
    UpdateRelationshipCacheData(Some(RelationshipRecords(records, localDate)), Some(email), Some("Divorce"), Some(date))
  }

  def createLoggedInUserInfo(name: Option[CitizenName] = Some(CitizenName(Some(firstName), Some(surname)))): LoggedInUserInfo = {
    LoggedInUserInfo(instanceIdentifier, timeStamp, None, name)
  }

  val mockMarriageAllowanceConnector: MarriageAllowanceConnector = mock[MarriageAllowanceConnector]
  val mockCachingService: CachingService = mock[CachingService]
  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[MarriageAllowanceConnector].toInstance(mockMarriageAllowanceConnector),
      bind[CachingService].toInstance(mockCachingService)
    ).build()

  val service: UpdateRelationshipService = instanceOf[UpdateRelationshipService]
  val applicationConfig: ApplicationConfig = instanceOf[ApplicationConfig]

  val updateRelationshipResponse = UpdateRelationshipResponse(ResponseStatus("OK"))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCachingService)
    reset(mockMarriageAllowanceConnector)
  }

  "retrieveRelationshipRecords" should {
    "retrive RealtionshipRecord" in {
      when(mockMarriageAllowanceConnector.listRelationship(any())(any(), any()))
        .thenReturn(Future.successful(recordList))

     val result = await(service.retrieveRelationshipRecords(nino))

      result shouldBe RelationshipRecords(activeRecipientRelationshipRecord, Seq(), createLoggedInUserInfo())
    }

    "throw Runtime Exception" when {
      "a NoPrimaryRecordError is returned" in {

        val noActiveRecordList = RelationshipRecordList(Seq(inactiveRelationshipRecord1), Some(createLoggedInUserInfo()))

        when(mockMarriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.successful(noActiveRecordList))

        val result = intercept[NoPrimaryRecordError](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe NoPrimaryRecordError()
      }

      "a MultipleActiveRecordError is returned" in {
        val multipleActiveRecordList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord,
          activeTransferorRelationshipRecord2), Some(createLoggedInUserInfo()))

        when(mockMarriageAllowanceConnector.listRelationship(any())(any(), any()))
          .thenReturn(Future.successful(multipleActiveRecordList))

        val result = intercept[MultipleActiveRecordError](await(service.retrieveRelationshipRecords(nino)))

        result shouldBe MultipleActiveRecordError()
      }

      "a CitizenNotFound error is returned" in {
        val noCitizenDetailsList = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), None)

        when(mockMarriageAllowanceConnector.listRelationship(any())(any(), any()))
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
      when(mockCachingService.put[Boolean](ArgumentMatchers.eq(CACHE_LOCKED_CREATE),
        ArgumentMatchers.eq(false))(any(), any())).thenReturn(Future.successful(false))
      when(mockCachingService.put[UserRecord](ArgumentMatchers.eq(CACHE_TRANSFEROR_RECORD),
        ArgumentMatchers.eq(userRecord))(any(), any())).thenReturn(Future.successful(userRecord))
      when(mockCachingService.put[RelationshipRecords](ArgumentMatchers.eq(CACHE_RELATIONSHIP_RECORDS),
        ArgumentMatchers.eq(relationshipRecords))(any(), any())).thenReturn(Future.successful(relationshipRecords))

      val result = await(service.saveRelationshipRecords(relationshipRecords))

      result shouldBe relationshipRecords
    }

    "throw an exception if there is an issue saving to the cache" in {

      val exception = new RuntimeException("error")

      when(mockCachingService.put[Boolean](ArgumentMatchers.eq(CACHE_LOCKED_CREATE),
        ArgumentMatchers.eq(false))(any(), any())).thenReturn(Future.successful(false))
      when(mockCachingService.put[UserRecord](ArgumentMatchers.eq(CACHE_TRANSFEROR_RECORD),
        ArgumentMatchers.eq(userRecord))(any(), any())).thenReturn(Future.successful(userRecord))
      when(mockCachingService.put[RelationshipRecords](ArgumentMatchers.eq(CACHE_RELATIONSHIP_RECORDS),
        ArgumentMatchers.eq(relationshipRecords))(any(), any())).thenReturn(Future.failed(exception))

      val result = intercept[RuntimeException](await(service.saveRelationshipRecords(relationshipRecords)))

      result shouldBe exception
    }

  }

  "getCheckClaimOrCancelDecision" should {
    "return a String when value is found in cache" in {
      when(mockCachingService.get[String](any())(any())).thenReturn(Future.successful(Some("Check Claim")))

      val result = await(service.getCheckClaimOrCancelDecision)

      result shouldBe Some("Check Claim")
    }

    "return a None when value is not found in cache" in {
      when(mockCachingService.get[String](any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getCheckClaimOrCancelDecision)

      result shouldBe None
    }
  }

  "getMakeChangesDecision" should {
    "return a cache value" in {
      when(mockCachingService.get[String](any())(any()))
        .thenReturn(Future.successful(Some("Divorce")))

      val result = await(service.getMakeChangesDecision)

      result shouldBe Some("Divorce")
    }

    "return None when no value returned from cache" in {
      when(mockCachingService.get[String](any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getMakeChangesDecision)

      result shouldBe None
    }
  }

  "saveMakeChangeDecision" should {
    "return a value from the cache" in {
      val endReason = "Divorce"

      when(mockCachingService.put[String](any(), any())(any(), any()))
        .thenReturn(Future.successful(endReason))

      val result = await(service.saveMakeChangeDecision(endReason))

      result shouldBe endReason
    }
  }

  "getDivorceDate" should {
    "return LocalDate when value returned from cache" in {
      when(mockCachingService.get[LocalDate](any())(any()))
        .thenReturn(Future.successful(Some(date)))

      val result = await(service.getDivorceDate)

      result shouldBe Some(date)
    }

    "return None when no value returned from cache" in {
      when(mockCachingService.get[LocalDate](any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getDivorceDate)

      result shouldBe None
    }
  }

  "getEmailAddress" should {
    "return String when value returned from cache" in {
      when(mockCachingService.get[String](any())(any()))
        .thenReturn(Future.successful(Some("email@email.com")))

      val result = await(service.getEmailAddress)

      result shouldBe Some("email@email.com")
    }

    "return None when no value returned from cache" in {
      when(mockCachingService.get[String](any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getEmailAddress)

      result shouldBe None
    }
  }

  "saveEmailAddress" should {
    "return a String" in {
      when(mockCachingService.put[EmailAddress](any(), any())(any(), any()))
        .thenReturn(Future.successful(EmailAddress("email@email.com")))

      val result = await(service.saveEmailAddress(EmailAddress("email@email.com")))

      result shouldBe EmailAddress("email@email.com")
    }
  }

  "getDataForDivorceExplanation" should {
    "return Role and LocalDate" in {
      val relationshipRecords = RelationshipRecords(recordList, localDate)

      when(mockCachingService.get[RelationshipRecords](CACHE_RELATIONSHIP_RECORDS))
        .thenReturn(Future.successful(Some(relationshipRecords)))

      when(mockCachingService.get[LocalDate](ArgumentMatchers.eq(CACHE_DIVORCE_DATE))(any()))
        .thenReturn(Future.successful(Some(date)))

      val result = await(service.getDataForDivorceExplanation)

      result._1 shouldBe Recipient
      result._2 shouldBe date
    }

    "CacheMissingDivorceDate Error when no value returned from cache" in {
      val relationshipRecords = RelationshipRecords(recordList, localDate)

      when(mockCachingService.get[RelationshipRecords](CACHE_RELATIONSHIP_RECORDS))
        .thenReturn(Future.successful(Some(relationshipRecords)))

      when(mockCachingService.get[LocalDate](ArgumentMatchers.eq(CACHE_DIVORCE_DATE))(any()))
        .thenReturn(Future.successful(None))

      val result = intercept[CacheMissingDivorceDate](await(service.getDataForDivorceExplanation))

      result shouldBe CacheMissingDivorceDate()
    }
  }

  "saveDivorceDate" should {
    "return a LocalDate" in {
      when(mockCachingService.put[LocalDate](any(), any())(any(), any()))
        .thenReturn(Future.successful(date))

      val result = await(service.saveDivorceDate(date))

      result shouldBe date
    }
  }

  "saveCheckClaimOrCancelDecision" should {
    "return a String" in {
      when(mockCachingService.put[String](any(), any())(any(), any()))
        .thenReturn(Future.successful("stopMarriageAllowance"))

      val result = await(service.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.StopMarriageAllowance))

      result shouldBe "stopMarriageAllowance"
    }
  }

  "UpdateRelationship" should {

    "construct and return the generated UpdateRelationshipRequestHolder" when {

      "valid data is returned from the cache" in {

          val expectedUpdateTimeStamp = "20150531235901"
          val expectedRecipientInformation = RecipientInformation(instanceIdentifier.toString, timeStamp)
          val expectedTransferorInformation = TransferorInformation(expectedUpdateTimeStamp)
          val expectedRelationshipInformation = RelationshipInformation(expectedUpdateTimeStamp, "Divorce/Separation", date.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
          val expectedName = s"$firstName $surname"

          val expectedUpdateRelationshipRequest = UpdateRelationshipRequest(expectedRecipientInformation, expectedTransferorInformation,
                                                  expectedRelationshipInformation)
          val expectedUpdateRelationshipNotificationRequest = UpdateRelationshipNotificationRequest(expectedName, EmailAddress(email),
                                                  "Recipient", welsh = false, isRetrospective = false)

          val expectedResult = UpdateRelationshipRequestHolder(expectedUpdateRelationshipRequest, expectedUpdateRelationshipNotificationRequest)

            when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
              .thenReturn(Future.successful(Some(createCachedData())))

            when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
              .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

          val result = await(service.updateRelationship(nino))

          result shouldBe expectedResult

      }

      "different marriage allowance end reasons are provided" when {

        val endReasonsWithEnumerations = Seq(("Divorce", "Divorce/Separation"), ("Cancel", "Cancelled by Transferor"))

        endReasonsWithEnumerations foreach { case(reason, enumeration) =>

          val cacheData = UpdateRelationshipCacheData(Some(RelationshipRecords(recordList, localDate)), Some(email), Some(reason), Some(date))

          s"the reason is $reason" in  {

            when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
              .thenReturn(Future.successful(Some(cacheData)))

            when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
              .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

            val result = await(service.updateRelationship(nino))
            val desEndReason = result.request.relationship.relationshipEndReason

            desEndReason shouldBe enumeration
          }
        }
      }

      val recordsWithUnknownUser = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), Some(createLoggedInUserInfo(None)))

      "a users name is not known" in {

        val cacheData = createCachedData(recordsWithUnknownUser)

        when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
          .thenReturn(Future.successful(Some(cacheData)))

        when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

        val result = await(service.updateRelationship(nino))
        val userName = result.notification.full_name

        userName shouldBe "Unknown"
      }
    }

    "throw relevant error" when {

      val cacheData = UpdateRelationshipCacheData(Some(RelationshipRecords(recordList, localDate)), Some(email), Some("Earnings"), Some(date))

      "if an unsupported marriage allowance end reason is provided" in {
        when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
          .thenReturn(Future.successful(Some(cacheData)))

        when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

        a[DesEnumerationNotFound] shouldBe thrownBy(await(service.updateRelationship(nino)))
      }


      "CannotUpdateRelationship error is returned from MarriageAllowanceConnector" in {
        val updateRelationshipResponse = 
          UpdateRelationshipResponse(ResponseStatus(CANNOT_UPDATE_RELATIONSHIP))

        when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
          .thenReturn(Future.successful(Some(createCachedData())))

        when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

        val result = intercept[CannotUpdateRelationship](await(service.updateRelationship(nino)))

        result shouldBe CannotUpdateRelationship()
      }

      "RecipientNotFound error is returned from MarriageAllowanceConnector" in {
        val updateRelationshipResponse = UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST))

        when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
          .thenReturn(Future.successful(Some(createCachedData())))

        when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

        val result = intercept[RecipientNotFound](await(service.updateRelationship(nino)))

        result shouldBe RecipientNotFound()
      }

      "RuntimeException is returned from the caching service" in {
        val updateRelationshipResponse = UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST))

        when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
          .thenReturn(Future.failed(new RuntimeException("Failed to retrieve cacheMap")))

        when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

        val result = intercept[RuntimeException](await(service.updateRelationship(nino)))

        result.getMessage shouldBe "Failed to retrieve cacheMap"
      }

      "CacheMapNoFound is returned from the caching service" in {
        val updateRelationshipResponse = UpdateRelationshipResponse(ResponseStatus(BAD_REQUEST))

        when(mockCachingService.get[UpdateRelationshipCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_RELATIONSHIP))(any()))
          .thenReturn(Future.successful(None))

        when(mockMarriageAllowanceConnector.updateRelationship(any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(Some(updateRelationshipResponse))))

        intercept[CacheMapNoFound](await(service.updateRelationship(nino)))
      }

    }

    "getConfirmationUpdateAnswers" should {
      "return Confirmation update answers" in {
        val relationshipRecords = Some(RelationshipRecords(recordList, localDate))
        val endDates = MarriageAllowanceEndingDates(TaxYear.current.starts, TaxYear.current.finishes)
        val cacheData = ConfirmationUpdateAnswersCacheData(relationshipRecords, Some(date), Some("email@email.com"), Some(endDates))

        when(mockCachingService.get[ConfirmationUpdateAnswersCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_CONFIRMATION))(any()))
          .thenReturn(Future.successful(Some(cacheData)))

        val result = await(service.getConfirmationUpdateAnswers)

        result shouldBe ConfirmationUpdateAnswers(createLoggedInUserInfo(), Some(LocalDate.now()), "email@email.com", endDates)
      }

      "return RuntimeException when cacheMap not found and returns RuntimeException" in {
        when(mockCachingService.get[ConfirmationUpdateAnswersCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_CONFIRMATION))(any()))
          .thenReturn(Future.failed(new RuntimeException("Failed to retrieve cacheMap")))

        val result = intercept[RuntimeException](await(service.getConfirmationUpdateAnswers))

        result.getMessage shouldBe "Failed to retrieve cacheMap"
      }

      "return CacheMapNoFound when cacheMap not found" in {
        when(mockCachingService.get[ConfirmationUpdateAnswersCacheData](ArgumentMatchers.eq(USER_ANSWERS_UPDATE_CONFIRMATION))(any()))
          .thenReturn(Future.successful(None))

        intercept[CacheMapNoFound](await(service.getConfirmationUpdateAnswers))
      }

    }

    "getMAEndingDatesForCancellation" should {
      "return dates" in {
        val result = await(service.getMAEndingDatesForCancellation)

        result shouldBe MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
      }
    }

    "getMAEndingDatesForDivorce" should {
      "return the end of the previous tax year as endDate" when {
        "role is Recipient and endDate is not in current tax year" in {
          val role = Recipient
          val divorceDate = LocalDate.of(TaxYear.current.previous.startYear, 7, 7)
          val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)

          val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

          result shouldBe expectedDates
        }

        "role is Transferor and endDate is in current tax year" in {
          val role = Transferor
          val divorceDate = LocalDate.of(TaxYear.current.finishYear, 1, 1)
          val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)

          val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

          result shouldBe expectedDates
        }
      }

      "return the end of the current tax year as endDate" in {
        val role = Recipient
        val divorceDate = LocalDate.of(TaxYear.current.startYear, 12, 31)
        val expectedDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)

        val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

        result shouldBe expectedDates
      }

      "return the endDate of the taxYear the divorceDate falls in" in {
        val role = Transferor
        val divorceDate = LocalDate.of(TaxYear.current.previous.startYear, 1, 1)

        val expectedDates = MarriageAllowanceEndingDates(
          LocalDate.of(TaxYear.current.previous.startYear, 4, 5),
          TaxYear.current.previous.starts)

        val result = await(service.getMAEndingDatesForDivorce(role, divorceDate))

        result shouldBe expectedDates

      }
    }

    "saveMarriageAllowanceEndingDates" should {
      "return MarriageAllowanceEndDate" in {
        val endingDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)

        when(mockCachingService.put[MarriageAllowanceEndingDates](any(), any())(any(), any()))
          .thenReturn(Future.successful(endingDates))

        val result = await(service.saveMarriageAllowanceEndingDates(endingDates))

        result shouldBe MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
      }
    }

    "getRelationshipRecords" should {
      "when RelationshipRecords are present return RelationshipRecords" in {
        when(mockCachingService.get[RelationshipRecords](CACHE_RELATIONSHIP_RECORDS))
          .thenReturn(Some(RelationshipRecords(recordList, localDate)))

        val result = await(service.getRelationshipRecords)

        result shouldBe RelationshipRecords(activeRecipientRelationshipRecord, Seq(), createLoggedInUserInfo())
      }

      "when RelationshipRecords are not present return CacheMissingRelationshipRecords Error" in {
        when(mockCachingService.get[RelationshipRecords](CACHE_RELATIONSHIP_RECORDS))
          .thenReturn(Future.successful(None))

        val result = intercept[CacheMissingRelationshipRecords](await(service.getRelationshipRecords))

        result shouldBe CacheMissingRelationshipRecords()
      }
    }

    "removeCache" should {
      "return the HTTPResponse when the cache has been dropped" in {
        when(mockCachingService.clear()(any())).thenReturn(Future.successful(()))
        val result = await(service.removeCache)
        result shouldBe ()
      }
    }
  }
}
