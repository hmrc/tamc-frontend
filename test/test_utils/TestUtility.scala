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
import controllers._
import details._
import errors.ErrorResponseStatus.RELATION_MIGHT_BE_CREATED
import models._
import org.joda.time.DateTime
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import services.{CachingService, TimeService, TransferService}
import test_utils.TestData.{Cids, Ninos}
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

class DummyHttpResponse(override val body: String, override val status: Int, override val allHeaders: Map[String, Seq[String]] = Map.empty)
      extends HttpResponse {
  override def json: JsValue = Json.parse(body)
}

trait TestUtility extends UnitSpec {

  def marriageAllowanceUrl(pageUrl: String): String = "/marriage-allowance-application" + pageUrl

  lazy val fakeApplication: Application = new GuiceApplicationBuilder().build()

  def eventsShouldMatch(event: DataEvent, auditType: String, details: Map[String, String], tags: Map[String, String] = Map.empty): Unit = {
    event match {
      case DataEvent("tamc-frontend", `auditType`, _, eventTags, `details`, _) if (tags.toSet subsetOf eventTags.toSet) =>
      case _ => fail(s"${event} did not match auditType:${auditType} details:${details} tags:${tags}")
    }
  }

  def makeFakeHomeController(): AuthorisationController = {
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
      override val auditConnector: AuditConnector = fakeCustomAuditConnector
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      def auditEventsToTest: List[DataEvent] = fakeCustomAuditConnector.auditEventsToTest
    }
  }

  def makeEligibilityController(): GdsEligibilityController = {
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

    new GdsEligibilityController {
      override val auditConnector = fakeCustomAuditConnector
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      def auditEventsToTest = fakeCustomAuditConnector.auditEventsToTest
    }
  }

  case class PtaElibilityTestComponent(request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type], controller: PtaEligibilityController)

  def makePtaEligibilityTestComponent(
                                       dataId: String,
                                       pd: PersonDetailsSuccessResponse = PersonDetailsSuccessResponse(PersonDetails(Person(Some("test_name"))))): PtaElibilityTestComponent = {
    Map(
      ("user_happy_path" -> PtaElibilityTestComponent(makeFakeRequest("ID-" + Ninos.ninoHappyPath), makePtaEligibilityController(Some(Ninos.ninoHappyPath), pd))),
      ("user_returning" -> PtaElibilityTestComponent(makeFakeRequest("ID-" + Ninos.ninoWithCL100), makePtaEligibilityController(Some(Ninos.ninoWithCL100), pd))),
      ("not_logged_in" -> PtaElibilityTestComponent(FakeRequest(), makePtaEligibilityController(None, pd))))
      .get(dataId).get
  }

  private def makePtaEligibilityController(
                                            nino: Option[String],
                                            pd: PersonDetailsSuccessResponse) = {
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

    val fakeIDACustomAuditConnector: AuditConnector = new AuditConnector {
      override lazy val auditingConfig = ???
      var auditEventsToTest: List[DataEvent] = List()

      override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
        auditEventsToTest = auditEventsToTest :+ event
        Future {
          AuditResult.Success
        }
      }
    }

    val fakeHttpGet: HttpGet = new HttpGet with WSGet {
      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        val nino: String = url.split("/")(2)
        val response = TestConstants.dummyHttpGetResponseJsonMap.get(nino)
        response.getOrElse(throw new IllegalArgumentException("transferor not supported for :" + url))
      }

      def appName: String = ???

      val hooks = NoneRequired

    }

    val fakeCitizenDetailsConnector: CitizenDetailsConnector = new CitizenDetailsConnector {
      override def httpGet: HttpGet = fakeHttpGet

      override def citizenDetailsUrl: String = "foo"
    }


    val fakeIdaAuthenticationProvider = new IdaAuthentificationProvider {
      override val login = "bar"
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      override implicit val formPartialRetriever = TamcFormPartialRetriever
      override def redirectToLogin(implicit request: Request[_]): Future[Result] = {
        nino match {
          case Some(validNino) => throw new IllegalArgumentException
          case None => super.redirectToLogin
        }
      }

      override val customAuditConnector = fakeIDACustomAuditConnector
    }

    val fakeMarriageAllowanceRegime = new MarriageAllowanceRegime {
      override val authenticationType = fakeIdaAuthenticationProvider
    }

    def createFakePayeAuthority(nino: String) =
      nino match {
        case Ninos.ninoWithLOA1 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, confidenceLevel = ConfidenceLevel.L50, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case Ninos.ninoWithLOA1_5 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L100, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case Ninos.ninoWithCL100 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = Some(new DateTime(2015, 11, 13, 9, 0)), credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L100, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case ninoLoa2 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L500, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
      }

    val fakeAuthConnector = new ApplicationAuthConnector {
      override val serviceUrl: String = null
      override lazy val http = null

      override def currentAuthority(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Authority]] = {
        nino match {
          case Some("NINO_NOT_AUTHORISED") => Future.successful(Some(Authority("ID-NOT_AUTHORISED", accounts = Accounts(), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L0, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")))
          case Some(validNino) => Future.successful(Some(createFakePayeAuthority(validNino)))
          case None => throw new IllegalArgumentException
        }
      }
    }

    val fakeCitizenDetailsService = new CitizenDetailsService {
      def citizenDetailsUrl = ???

      override def cachingService = ???

      override def citizenDetailsConnector: CitizenDetailsConnector = fakeCitizenDetailsConnector

      override def getPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] = {
        return Future.successful(pd)
      }
    }

    new PtaEligibilityController {
      override val auditConnector = fakeCustomAuditConnector
      override val maAuthRegime = fakeMarriageAllowanceRegime
      override val authConnector = fakeAuthConnector
      override val citizenDetailsService = fakeCitizenDetailsService
      override val ivUpliftUrl = "jazz"
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      def auditEventsToTest = fakeCustomAuditConnector.auditEventsToTest
    }
  }

  def makeMultiYearGdsEligibilityController(): MultiYearGdsEligibilityController = {
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

    new MultiYearGdsEligibilityController {
      override val auditConnector: AuditConnector = fakeCustomAuditConnector
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      def auditEventsToTest: List[DataEvent] = fakeCustomAuditConnector.auditEventsToTest
    }
  }

  case class MultiYearPtaElibilityTestComponent(request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type], controller: MultiYearPtaEligibilityController)

  def makeMultiYearPtaEligibilityTestComponent(
                                                dataId: String,
                                                pd: PersonDetailsSuccessResponse = PersonDetailsSuccessResponse(PersonDetails(Person(Some("test_name"))))): MultiYearPtaElibilityTestComponent = {
    Map(
      ("user_happy_path" -> MultiYearPtaElibilityTestComponent(makeFakeRequest("ID-" + Ninos.ninoHappyPath), makeMultiYearPtaEligibilityController(Some(Ninos.ninoHappyPath), pd))),
      ("user_returning" -> MultiYearPtaElibilityTestComponent(makeFakeRequest("ID-" + Ninos.ninoWithCL100), makeMultiYearPtaEligibilityController(Some(Ninos.ninoWithCL100), pd))),
      ("not_logged_in" -> MultiYearPtaElibilityTestComponent(FakeRequest(), makeMultiYearPtaEligibilityController(None, pd))))
      .get(dataId).get
  }

  private def makeMultiYearPtaEligibilityController(
                                                     nino: Option[String],
                                                     pd: PersonDetailsSuccessResponse) = {

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

    val fakeHttpGet = new HttpGet with WSGet{
      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        val nino: String = url.split("/")(2)
        val response = TestConstants.dummyHttpGetResponseJsonMap.get(nino)
        response.getOrElse(throw new IllegalArgumentException("transferor not supported for :" + url))
      }

      def appName: String = ???

      val hooks = NoneRequired
    }

    val fakeCitizenDetailsConnector = new CitizenDetailsConnector {
      override def httpGet: HttpGet = fakeHttpGet

      override def citizenDetailsUrl: String = "foo"
    }

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
          case None => super.redirectToLogin
        }
      }

      override val customAuditConnector = fakeIDACustomAuditConnector
    }

    val fakeMarriageAllowanceRegime = new MarriageAllowanceRegime {
      override val authenticationType = fakeIdaAuthenticationProvider
    }

    def createFakePayeAuthority(nino: String) =
      nino match {
        case Ninos.ninoWithLOA1 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, confidenceLevel = ConfidenceLevel.L50, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case Ninos.ninoWithLOA1_5 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L100, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case Ninos.ninoWithCL100 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = Some(new DateTime(2015, 11, 13, 9, 0)), credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L100, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
        case ninoLoa2 => Authority("ID-" + nino, accounts = Accounts(paye = Some(PayeAccount(s"/ZZZ/${nino}", Nino(nino)))), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L500, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")
      }

    val fakeAuthConnector = new ApplicationAuthConnector {
      override val serviceUrl: String = null
      override lazy val http = null

      override def currentAuthority(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Authority]] = {
        nino match {
          case Some("NINO_NOT_AUTHORISED") => Future.successful(Some(Authority("ID-NOT_AUTHORISED", accounts = Accounts(), loggedInAt = None, previouslyLoggedInAt = None, credentialStrength = CredentialStrength.Strong, ConfidenceLevel.L0, userDetailsLink = None, enrolments = None, ids = None, legacyOid = "")))
          case Some(validNino) => Future.successful(Some(createFakePayeAuthority(validNino)))
          case None => throw new IllegalArgumentException
        }
      }
    }

    val fakeCitizenDetailsService = new CitizenDetailsService {
      def citizenDetailsUrl = ???

      override def cachingService = ???

      override def citizenDetailsConnector: CitizenDetailsConnector = fakeCitizenDetailsConnector

      override def getPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] = {
        return Future.successful(pd)
      }
    }

    new MultiYearPtaEligibilityController {
      override val auditConnector = fakeCustomAuditConnector
      override val maAuthRegime = fakeMarriageAllowanceRegime
      override val authConnector = fakeAuthConnector
      override val citizenDetailsService = fakeCitizenDetailsService
      override val ivUpliftUrl = "jazz"
      override implicit val templateRenderer: TemplateRenderer = MockTemplateRenderer
      def auditEventsToTest = fakeCustomAuditConnector.auditEventsToTest
    }
  }

  trait DebugData {
    def cachingTransferorRecordToTest(): Option[UserRecord] = ???

    def cachingTransferorRecordToTestCount(): Int = ???

    def cachingRecipientRecordToTest(): Option[UserRecord] = ???

    def cachingRecipientRecordToTestCount(): Int = ???

    def cachingRecipientDataToTest(): Option[RegistrationFormInput] = ???

    def cachingRecipientFormDataToTest(): Option[RecipientDetailsFormInput] = ???

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

    def citizenDetailsCallsCount: Int = ???

    def saveSelectedYears: Option[List[Int]] = ???
  }

  trait MarriageAllowanceControllerWithDebug extends TransferController with DebugData

  case class TestComponent(request: play.api.test.FakeRequest[play.api.mvc.AnyContentAsEmpty.type], controller: MarriageAllowanceControllerWithDebug)

  def makeTestComponent(
                         dataId: String,
                         recipientData: Option[RegistrationFormInput] = None,
                         transferorRecipientData: Option[CacheData] = None,
                         recipientDetailsFormData: Option[RecipientDetailsFormInput] = None,
                         riskTriageRouteBiasPercentageParam: Int = 0,
                         cocEnabledTestInput: Boolean = false,
                         pd: PersonDetailsSuccessResponse = PersonDetailsSuccessResponse(PersonDetails(Person(Some("test_name")))),
                         testingTime: DateTime = TestConstants.TEST_CURRENT_DATE,
                         testCacheData: Option[UpdateRelationshipCacheData] = None): TestComponent = {
    Map(
      ("user_happy_path" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoHappyPath), makeController(Some(Ninos.ninoHappyPath), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("user_LOA_1" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithLOA1), makeController(Some(Ninos.ninoWithLOA1), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("user_LOA_1_5" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithLOA1_5), makeController(Some(Ninos.ninoWithLOA1_5), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("user_has_relationship" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithRelationship), makeController(Some(Ninos.ninoWithRelationship), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("transferor_cid_not_found" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoTransferorNotFound), makeController(Some(Ninos.ninoTransferorNotFound), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("transferor_deceased" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoTransferorDeceased), makeController(Some(Ninos.ninoTransferorDeceased), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("conflict_409" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithConflict), makeController(Some(Ninos.ninoWithConflict), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("ltm000503" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoWithLTM000503), makeController(Some(Ninos.ninoWithLTM000503), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("throw_error" -> TestComponent(makeFakeRequest("ID-" + Ninos.ninoError), makeController(Some(Ninos.ninoError), recipientData, transferorRecipientData, recipientDetailsFormData, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("not_logged_in" -> TestComponent(FakeRequest(), makeController(None, None, None, None, cocEnabledTestInput, pd, testingTime, testCacheData))),
      ("not_authorised" -> TestComponent(makeFakeRequest("ID-NOT_AUTHORISED"), makeController(Some("NINO_NOT_AUTHORISED"), None, None, None, cocEnabledTestInput, pd, testingTime, testCacheData))))
      .get(dataId).get
  }

  private def makeFakeRequest(userId: String) = FakeRequest().withSession(
    SessionKeys.sessionId -> s"session-$userId",
    SessionKeys.lastRequestTimestamp -> now.getMillis.toString,
    SessionKeys.userId -> userId)

  private def makeController(
                              nino: Option[String],
                              recipientData: Option[RegistrationFormInput],
                              transferorRecipientData: Option[CacheData],
                              recipientDetailsFormData: Option[RecipientDetailsFormInput],
                              cocEnabledTestInput: Boolean,
                              pd: PersonDetailsSuccessResponse,
                              testingTime: DateTime,
                              testingCacheData: Option[UpdateRelationshipCacheData]) = {

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
          case None => super.redirectToLogin
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

    val fakeHttpGet = new HttpGet with WSGet{
      override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
        val nino: String = url.split("/")(2)
        val response = TestConstants.dummyHttpGetResponseJsonMap.get(nino)
        response.getOrElse(throw new IllegalArgumentException("transferor not supported for :" + url))
      }

      def appName: String = ???

      val hooks = NoneRequired

    }

    val fakeHttpPost = new HttpPost with WSPost{
      override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        body match {
          case RegistrationFormInput(_, _, _, Nino(Ninos.ninoWithLOA1), _) =>
            Future.successful(new DummyHttpResponse(s"""{"user_record":{"cid": ${Cids.cid2}, "timestamp": "2015", "has_allowance": false}, "status": {"status_code":"OK"}}""", 200))
          case RegistrationFormInput(_, _, _, Nino(Ninos.nino4), _) =>
            Future.successful(new DummyHttpResponse(s"""{"user_record":{"cid": 2000002, "timestamp": "2015", "has_allowance": true}, "status": {"status_code":"OK"}}""", 200))
          case RegistrationFormInput(_, _, _, Nino(Ninos.ninoTransferorNotFound), _) =>
            Future.successful(new DummyHttpResponse(s"""{"status": {"status_code":"TAMC:ERROR:RECIPIENT-NOT-FOUND"}}""", OK))
          case RegistrationFormInput(_, _, _, Nino(Ninos.ninoTransferorDeceased), _) =>
            Future.successful(new DummyHttpResponse(s"""{"status": {"status_code":"TAMC:ERROR:RECIPIENT-DECEASED"}}""", OK))
          case RegistrationFormInput(_, _, _, Nino(Ninos.ninoError), _) =>
            Future.failed(new BadGatewayException("TESTING-POST-ERROR"))
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

    val fakeHttpPut = new HttpPut with WSPut{
      var createRelationshipCallCountToTest = 0
      var createRelationshipUrl: Option[String] = None

      override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
        createRelationshipCallCountToTest = createRelationshipCallCountToTest + 1
        createRelationshipUrl = Some(url)

        val response = TestConstants.dummyHttpPutResponseJsonMap.get(body.toString)
        response.getOrElse(throw new IllegalArgumentException("action not supported for :" + url))
      }

      def appName: String = ???

      val hooks = NoneRequired
    }

    val fakeMiddleConnector = new MarriageAllowanceConnector {
      override def httpGet: HttpGet = fakeHttpGet

      override def httpPost: HttpPost = fakeHttpPost

      override def httpPut: HttpPut = fakeHttpPut

      override val marriageAllowanceUrl = "foo"

      override def createRelationship(transferorNino: Nino, data: CreateRelationshipRequestHolder, journey: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
        transferorNino.nino match {
          case TestData.Ninos.ninoWithConflict => Future.successful(HttpResponse(200, Some(Json.toJson(CreateRelationshipResponse(
            status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED))))))
          case TestData.Ninos.ninoWithLTM000503 =>  Future.successful(HttpResponse(200, Some(Json.toJson(CreateRelationshipResponse(
            status = ResponseStatus(status_code = RELATION_MIGHT_BE_CREATED))))))
          case _ => super.createRelationship(transferorNino: Nino, data: CreateRelationshipRequestHolder, journey: String)
        }

      }
    }

    val fakeCitizenDetailsConnector = new CitizenDetailsConnector {
      override def httpGet: HttpGet = fakeHttpGet

      override def citizenDetailsUrl: String = "foo"
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

      var recipientDetailsFormInputDataToTest: Option[RecipientDetailsFormInput] = None

      var notificationToTest: Option[NotificationRecord] = None

      var saveYearsToTest: Option[List[Int]] = None

      var transferorRecordRetrieval = 0
      var recipientRecordRetrieval = 0
      var saveNotificationCount = 0
      var retrieveAllCount = 0

      var lockCreateRelationshipCount = 0
      var lockCreateRelationshipVal: Option[Boolean] = None

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

      override def saveRecipientDetails(recipientDetails: RecipientDetailsFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RecipientDetailsFormInput] = {
        recipientDetailsFormInputDataToTest = Some(recipientDetails)
        Future.successful(recipientDetails)
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

      override def getCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheData]] = {
        retrieveAllCount = retrieveAllCount + 1
        Future.successful(transferorRecipientData)
      }

      override def saveLoggedInUserInfo(loggedInUserInfo: LoggedInUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LoggedInUserInfo] =
        Future.successful(loggedInUserInfo)

      override def saveActiveRelationshipRecord(activeRelationshipRecord: RelationshipRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecord] =
        Future.successful(activeRelationshipRecord)

      override def saveSelectedYears(selectedYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] = {
        saveYearsToTest = Some(selectedYears)
        Future.successful(selectedYears)
      }

      override def getUpdateRelationshipCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] = {
        Future.successful(testingCacheData)
      }

      override def getRecipientDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RecipientDetailsFormInput]] = {
        Future.successful(recipientDetailsFormInputDataToTest)
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

    val fakeCitizenDetailsService = new CitizenDetailsService {
      def citizenDetailsUrl = ???

      override def cachingService = fakeCachingService

      override def citizenDetailsConnector: CitizenDetailsConnector = fakeCitizenDetailsConnector

      var citizenDetailsCallsCount = 0

      override def getPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] = {
        citizenDetailsCallsCount = citizenDetailsCallsCount + 1
        return Future.successful(pd)
      }
    }

    new MarriageAllowanceControllerWithDebug {
      override val maAuthRegime = fakeMarriageAllowanceRegime
      override val registrationService = fakeRegistrationService
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

      override def cachingRecipientFormDataToTest = fakeCachingService.recipientDetailsFormInputDataToTest

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

      override def citizenDetailsCallsCount = fakeCitizenDetailsService.citizenDetailsCallsCount

      override def saveSelectedYears = fakeCachingService.saveYearsToTest
    }
  }
}
