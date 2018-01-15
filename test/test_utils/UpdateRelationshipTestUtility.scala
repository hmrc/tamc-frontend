/*
 * Copyright 2018 HM Revenue & Customs
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

package test_utils

import actions.{IdaAuthentificationProvider, MarriageAllowanceRegime}
import config.TamcFormPartialRetriever
import connectors.{ApplicationAuthConnector, CitizenDetailsConnector, MarriageAllowanceConnector}
import controllers.{AuthorisationController, UpdateRelationshipController}
import details.{CitizenDetailsService, Person, PersonDetails, PersonDetailsSuccessResponse}
import models._
import org.joda.time.DateTime
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Writes
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CachingService, TimeService, TransferService, UpdateRelationshipService}
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost, WSPut}
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.DateTimeUtils.now
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.{ExecutionContext, Future}

trait UpdateRelationshipTestUtility extends UnitSpec {

  def marriageAllowanceUrl(pageUrl: String): String = "/marriage-allowance-application" + pageUrl

  lazy val fakeApplication: Application =
    new GuiceApplicationBuilder()
      .configure(Map("Test.microservice.assets.url" -> "test-url", "Test.microservice.assets.version" -> "test-version"))
      .build

  def eventsShouldMatch(event: DataEvent, auditType: String, details: Map[String, String], tags: Map[String, String] = Map.empty) = event match {
    case DataEvent("tamc-frontend", `auditType`, _, eventTags, `details`, _) if (tags.toSet subsetOf eventTags.toSet) =>
    case _ => fail(s"${event} did not match auditType:${auditType} details:${details} tags:${tags}")
  }

  def makeFakeHomeController(ptaEnabledTestInput: Boolean = false) = {
    val fakeCustomAuditConnector = new AuditConnector {
      override lazy val auditingConfig = ???
      var auditEventsToTest: List[DataEvent] = List()

      override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
        auditEventsToTest = auditEventsToTest :+ event
        Future {
          AuditResult.Success
        }
      }
    }

    new AuthorisationController {
      override val logoutUrl = "/ida/signout"
      override val logoutCallbackUrl = "/feedback-survey/?origin=TAMC"
      override val auditConnector = fakeCustomAuditConnector

      def auditEventsToTest = fakeCustomAuditConnector.auditEventsToTest
    }
  }

  trait DebugData {
    def cachingTransferorRecordToTest(): Option[UserRecord] = ???

    def cachingTransferorRecordToTestCount(): Int = ???

    def cachingRecipientRecordToTest(): Option[UserRecord] = ???

    def cachingRecipientRecordToTestCount(): Int = ???

    def cachingRecipientDataToTest(): Option[RegistrationFormInput] = ???

    def notificationToTest: Option[NotificationRecord] = ???

    def saveNotificationCount: Int = ???

    def cachingRecipientRecordRetrievalCount(): Int = ???

    def cachingRetrievalCount(): Int = ???

    def cachingLockCreateRelationship(): Int = ???

    def cachingLockCreateValue: Option[Boolean] = ???

    def createRelationshipCallCountToTest: Int = ???

    def createRelationshipUrl: Option[String] = ???

    def auditEventsToTest: List[DataEvent] = ???

    def idaAuditEventsToTest: List[DataEvent] = ???

    def loggedInUserInfoCount: Int = ???

    def loggedInUserInfoVal: Option[LoggedInUserInfo] = ???

    def relationshipEndReasonCount: Int = ???

    def relationshipEndReasonRecord: Option[EndRelationshipReason] = ???

  }

  trait UpdateRelationshipControllerWithDebug extends UpdateRelationshipController with DebugData

  case class TestComponent(request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type], controller: UpdateRelationshipControllerWithDebug)

  def makeUpdateRelationshipTestComponent(
                                           dataId: String,
                                           loggedInUserInfo: Option[LoggedInUserInfo] = None,
                                           transferorRecipientData: Option[UpdateRelationshipCacheData] = None,
                                           cocEnabledTestInput: Boolean = true,
                                           cachePd: Option[PersonDetails] = None,
                                           cidPd: PersonDetailsSuccessResponse = PersonDetailsSuccessResponse(PersonDetails(Person(Some("test_name")))),
                                           testingTime: DateTime = TestConstants.TEST_CURRENT_DATE): TestComponent = {
    Map(
      ("coc_no_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithNoRelationship), makeController(Some(Ninos.ninoWithNoRelationship), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_active_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithActiveRelationship), makeController(Some(Ninos.ninoWithActiveRelationship), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_gap_in_years" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithGapInYears), makeController(Some(Ninos.ninoWithGapInYears), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_historic_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithHistoricRelationship), makeController(Some(Ninos.ninoWithHistoricRelationship), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_historic_rejectable_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithHistoricRejectableRelationship), makeController(Some(Ninos.ninoWithHistoricRejectableRelationship), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_historically_active_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithHistoricallyActiveRelationship), makeController(Some(Ninos.ninoWithHistoricallyActiveRelationship), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_active_historic_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithHistoricActiveRelationship), makeController(Some(Ninos.ninoWithHistoricActiveRelationship), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_citizen_not_found" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoCitizenNotFound), makeController(Some(Ninos.ninoCitizenNotFound), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))),
      ("coc_bad_request" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoForBadRequest), makeController(Some(Ninos.ninoForBadRequest), loggedInUserInfo, transferorRecipientData, cocEnabledTestInput, cachePd, cidPd, testingTime))))
      .get(dataId).get
  }

  private def makeFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-$userId",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId)

  private def makeController(
                              nino: Option[String],
                              loggedInUserInfo: Option[LoggedInUserInfo],
                              transferorRecipientData: Option[UpdateRelationshipCacheData],
                              cocEnabledTestInput: Boolean,
                              cachePd: Option[PersonDetails],
                              cidPd: PersonDetailsSuccessResponse,
                              testingTime: DateTime) = {
    val fakeIDACustomAuditConnector = new AuditConnector {
      override lazy val auditingConfig = ???
      var auditEventsToTest: List[DataEvent] = List()

      override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
        auditEventsToTest = auditEventsToTest :+ event
        Future {
          AuditResult.Success
        }
      }
    }


    val fakeIdaAuthenticationProvider = new IdaAuthentificationProvider {
      override val login = "bar"
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      override implicit val formPartialRetriever = TamcFormPartialRetriever
      override def redirectToLogin(implicit request: Request[_]): Future[Result] = {
        nino match {
          case Some(validNino) => throw new IllegalArgumentException
          case None => super.redirectToLogin(request)
        }
      }

      override val customAuditConnector = fakeIDACustomAuditConnector
    }

    val fakeMarriageAllowanceRegime = new MarriageAllowanceRegime {
      override val authenticationType = fakeIdaAuthenticationProvider
    }

    def createFakePayeAuthority(nino: String) =
      nino match {
        case Ninos.ninoWithLOA1 => Authority("ID-" + nino, Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L50, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case Ninos.ninoWithLOA1_5 => Authority("ID-" + nino, Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L100, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case ninoLoa2 => Authority("ID-" + nino, Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), None, None, CredentialStrength.Strong, ConfidenceLevel.L500, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
      }

    val fakeAuthConnector = new ApplicationAuthConnector {
      override val serviceUrl: String = null
      override lazy val http = null

      override def currentAuthority(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Authority]] = {
        nino match {
          case Some("NINO_NOT_AUTHORISED") => Future.successful(Some(Authority("ID-NOT_AUTHORISED", Accounts(), None, None, CredentialStrength.Strong, ConfidenceLevel.L0, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")))
          case Some(validNino) => Future.successful(Some(createFakePayeAuthority(validNino)))
          case None => throw new IllegalArgumentException
        }
      }
    }

    val fakeCustomAuditConnector = new AuditConnector {
      override lazy val auditingConfig = ???
      var auditEventsToTest: List[DataEvent] = List()

      override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
        auditEventsToTest = auditEventsToTest :+ event
        Future {
          AuditResult.Success
        }
      }
    }

    val fakeHttpGet = new HttpGet with WSGet {
      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        val nino: String = url.split("/")(2)
        val response = TestConstants.dummyHttpGetResponseJsonMap.get(nino)
        response.getOrElse(throw new IllegalArgumentException("transferor not supported for :" + url))
      }

      def appName: String = ???

      val hooks = NoneRequired
    }

    val fakeHttpCititzenGet = new HttpGet with WSGet {

      import play.api.libs.json.Json

      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        new DummyHttpResponse(Json.toJson(cidPd.personDetails).toString, OK)
      }

      def appName: String = ???

      val hooks = NoneRequired
    }

    val fakeHttpPost = new HttpPost with WSPost {
      override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        body match {
          case _ =>
            throw new IllegalArgumentException("recepient not supported for :" + url)
        }
      }

      override def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      override def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

      def appName: String = ???

      val hooks = NoneRequired
    }

    val fakeHttpPut = new HttpPut with WSPut {
      var createRelationshipCallCountToTest = 0
      var createRelationshipUrl: Option[String] = None

      override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        createRelationshipCallCountToTest = createRelationshipCallCountToTest + 1
        createRelationshipUrl = Some(url)

        val response = TestConstants.dummyHttpPutResponseJsonMap.get(body.toString)
        response.getOrElse(throw new IllegalArgumentException(body.toString + "action not supported for : " + url))
      }

      def appName: String = ???

      val hooks = NoneRequired
    }

    val fakeMiddleConnector = new MarriageAllowanceConnector {
      override def httpGet: HttpGet = fakeHttpGet

      override def httpPost: HttpPost = fakeHttpPost

      override def httpPut: HttpPut = fakeHttpPut

      override val marriageAllowanceUrl = "foo"
    }

    val fakeCachingService = new CachingService {
      override def baseUri: String = ???

      override def defaultSource: String = ???

      override def domain: String = ???

      override def http = ???

      var transferorRecordToTest: Option[UserRecord] = None
      var transferorRecordToTestCount = 0

      var recipientRecordToTest: Option[UserRecord] = None
      var recipientRecordToTestCount = 0

      var recipientDataToTest: Option[RegistrationFormInput] = None

      var notificationToTest: Option[NotificationRecord] = None

      var loggedInUserInfoToTest: Option[LoggedInUserInfo] = None
      var relationshipEndReasonRecord: Option[EndRelationshipReason] = None

      var transferorRecordRetrieval = 0
      var recipientRecordRetrieval = 0
      var saveNotificationCount = 0
      var retrieveAllCount = 0

      var lockCreateRelationshipCount = 0
      var lockCreateRelationshipVal: Option[Boolean] = None
      var loggedInUserInfoCount = 0
      var endRelationshipReasonCount = 0

      override def saveTransferorRecord(transferorRecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] = {
        transferorRecordToTest = Some(transferorRecord)
        transferorRecordToTestCount = transferorRecordToTestCount + 1
        Future.successful(transferorRecord)
      }

      override def saveRecipientRecord(recipientRecord: UserRecord, recipientData: RegistrationFormInput, aivailableYears: List[TaxYear])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] = {
        recipientRecordToTest = Some(recipientRecord)
        recipientDataToTest = Some(recipientData)
        recipientRecordToTestCount = recipientRecordToTestCount + 1
        Future.successful(recipientRecord)
      }

      override def saveNotificationRecord(notificationRecord: NotificationRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] = {
        saveNotificationCount = saveNotificationCount + 1
        notificationToTest = Some(notificationRecord)
        Future.successful(notificationRecord)
      }

      override def lockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
        lockCreateRelationshipCount = lockCreateRelationshipCount + 1
        lockCreateRelationshipVal = Some(true)
        Future.successful(true)
      }

      override def unlockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
        lockCreateRelationshipCount = lockCreateRelationshipCount + 1
        lockCreateRelationshipVal = Some(false)
        Future.successful(false)
      }

      override def getUpdateRelationshipCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] = {
        retrieveAllCount = retrieveAllCount + 1
        Future.successful(transferorRecipientData)
      }

      override def saveLoggedInUserInfo(loggedInUserInfo: LoggedInUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LoggedInUserInfo] = {
        loggedInUserInfoCount = loggedInUserInfoCount + 1
        loggedInUserInfoToTest = Some(loggedInUserInfo)
        Future.successful(loggedInUserInfo)
      }

      override def saveActiveRelationshipRecord(activeRelationshipRecord: RelationshipRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecord] =
        Future.successful(activeRelationshipRecord)

      override def savRelationshipEndReasonRecord(relationshipEndReason: EndRelationshipReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndRelationshipReason] = {
        relationshipEndReasonRecord = Some(relationshipEndReason)
        endRelationshipReasonCount = endRelationshipReasonCount + 1
        Future.successful(relationshipEndReason)
      }

      override def saveHistoricRelationships(historic: Option[Seq[RelationshipRecord]])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[RelationshipRecord]]] =
        Future.successful(historic)

      override def getPersonDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PersonDetails]] = {
        Future {
          cachePd
        }
      }

      override def savePersonDetails(personDetails: PersonDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PersonDetails] = {
        Future {
          personDetails
        }
      }
    }

    val fakeTimeService = new TimeService {
      override val taxYearResolver = new TaxYearResolver {
        override lazy val now = () => testingTime
      }
    }

    val fakeRegistrationService = new TransferService {
      override val customAuditConnector = fakeCustomAuditConnector
      override val marriageAllowanceConnector = fakeMiddleConnector
      override val cachingService = fakeCachingService
      override val timeService = fakeTimeService
    }

    val fakeUpdateRelationshipService = new UpdateRelationshipService {
      override val customAuditConnector = fakeCustomAuditConnector
      override val marriageAllowanceConnector = fakeMiddleConnector
      override val cachingService = fakeCachingService
      override val timeService = fakeTimeService
    }

    val fakeCitizenDetailsConnector = new CitizenDetailsConnector {
      override def httpGet: HttpGet = fakeHttpCititzenGet

      override def citizenDetailsUrl: String = "foo"
    }

    val fakeCitizenDetailsService = new CitizenDetailsService {
      def citizenDetailsUrl = ???

      override def citizenDetailsConnector: CitizenDetailsConnector = fakeCitizenDetailsConnector

      override def cachingService = fakeCachingService
    }

    new UpdateRelationshipControllerWithDebug {

      override val registrationService = fakeRegistrationService
      override val updateRelationshipService = fakeUpdateRelationshipService
      override val maAuthRegime = fakeMarriageAllowanceRegime
      override val authConnector = fakeAuthConnector
      override val citizenDetailsService = fakeCitizenDetailsService
      override val ivUpliftUrl = "jazz"
      override val timeService = fakeTimeService

      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer

      override def cachingTransferorRecordToTest = fakeCachingService.transferorRecordToTest

      override def cachingTransferorRecordToTestCount = fakeCachingService.transferorRecordToTestCount

      override def cachingRecipientRecordToTest = fakeCachingService.recipientRecordToTest

      override def cachingRecipientRecordToTestCount = fakeCachingService.recipientRecordToTestCount

      override def cachingRecipientDataToTest = fakeCachingService.recipientDataToTest

      override def cachingRecipientRecordRetrievalCount = fakeCachingService.recipientRecordRetrieval

      override def cachingRetrievalCount = fakeCachingService.retrieveAllCount

      override def createRelationshipCallCountToTest = fakeHttpPut.createRelationshipCallCountToTest

      override def auditEventsToTest = fakeCustomAuditConnector.auditEventsToTest

      override def idaAuditEventsToTest = fakeIDACustomAuditConnector.auditEventsToTest

      override def notificationToTest = fakeCachingService.notificationToTest

      override def saveNotificationCount = fakeCachingService.saveNotificationCount

      override def cachingLockCreateRelationship = fakeCachingService.lockCreateRelationshipCount

      override def cachingLockCreateValue = fakeCachingService.lockCreateRelationshipVal

      override def createRelationshipUrl = fakeHttpPut.createRelationshipUrl

      override def loggedInUserInfoCount = fakeCachingService.loggedInUserInfoCount

      override def loggedInUserInfoVal = fakeCachingService.loggedInUserInfoToTest

      override def relationshipEndReasonCount = fakeCachingService.endRelationshipReasonCount

      override def relationshipEndReasonRecord = fakeCachingService.relationshipEndReasonRecord
    }
  }
}
