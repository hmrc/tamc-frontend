/*
 * Copyright 2026 HM Revenue & Customs
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

package utils

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, ok, post, urlEqualTo}
import models.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Injecting
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import services.CacheService.*
import services.CacheService.CacheKey.{CacheReadKey, CacheReadWriteKey}
import services.{ApplicationService, CachingService}
import uk.gov.hmrc.domain.{AtedUtr, Generator, Nino}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

trait IntegrationSpec
    extends PlaySpec
    with GuiceOneAppPerSuite
    with Matchers
    with WireMockHelper
    with ScalaFutures
    with IntegrationPatience
    with Injecting {

  val generatedNino: Nino                        = new Generator().nextNino
  val generatedSaUtr: AtedUtr                    = new Generator().nextAtedUtr
  lazy val messagesApi: MessagesApi              = inject[MessagesApi]
  val mockCachingService: CachingService         = mock[CachingService]
  val mockApplicationService: ApplicationService = mock[ApplicationService]
  implicit lazy val messages: Messages           = MessagesImpl(Lang("en"), messagesApi)
  implicit lazy val ec: ExecutionContext         = app.injector.instanceOf[ExecutionContext]

  val instanceIdentifier = 1
  val timeStamp          = "20130101"
  val firstName          = "First"
  val surname            = "Surname"
  val date: LocalDate    = LocalDate.now()
  val email              = "email@email.com"

  protected val baseUrl = "/marriage-allowance-application"
  val sessionId: String = UUID.randomUUID().toString

  protected def request(
    url: String,
    method: String = GET
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(method, s"$baseUrl$url")
      .withSession(
        SessionKeys.sessionId -> sessionId,
        SessionKeys.authToken -> "Bearer 123"
      )

  val activeRecipientRelationshipRecord: RelationshipRecord = RelationshipRecord(
    Recipient.value,
    creationTimestamp = "20150531",
    participant1StartDate = "20150531",
    relationshipEndReason = Some(DesRelationshipEndReason.Default),
    participant1EndDate = None,
    otherParticipantInstanceIdentifier = "123456789123",
    otherParticipantUpdateTimestamp = "20150531"
  )

  def createLoggedInUserInfo(
    name: Option[CitizenName] = Some(CitizenName(Some(firstName), Some(surname)))
  ): LoggedInUserInfo = LoggedInUserInfo(instanceIdentifier, timeStamp, None, name)

  lazy val relationshipRecordList = RelationshipRecordList(
    Seq(activeRecipientRelationshipRecord),
    Some(createLoggedInUserInfo())
  )

  lazy val maConnectorResponse = RelationshipRecordStatusWrapper(
    relationship_record = relationshipRecordList,
    status = ResponseStatus("OK")
  )

  val recordList          = RelationshipRecordList(Seq(activeRecipientRelationshipRecord), Some(createLoggedInUserInfo()))
  val relationshipRecords = RelationshipRecords(recordList, date)

  val maEndingDates                      = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
  val dateOfMarriageInput                = DateOfMarriageFormInput(LocalDate.now().minusYears(2))
  val confirmationUpdateAnswersCacheData = ConfirmationUpdateAnswersCacheData(
    Some(relationshipRecords),
    Some(date),
    Some("email@email.com"),
    Some(maEndingDates)
  )

  val recipientDetails         = RecipientDetailsFormInput("Firstname", "Lastname", Gender("M"), Nino("AB123456A"))
  val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
  val userRecord: UserRecord   = UserRecord(123456789L, "2015", None, Some(citizenName))
  val cancellationConfirmation = CancellationConfirmation(marriageAllowanceEndDate = LocalDate.of(2027, 4, 5))

  val authResponse: String =
    s"""
       |{
       |    "confidenceLevel": 200,
       |    "nino": "$generatedNino",
       |    "saUtr": "$generatedSaUtr",
       |    "name": {
       |        "name": "Test",
       |        "lastName": "User"
       |    },
       |    "loginTimes": {
       |        "currentLogin": "2021-06-07T10:52:02.594Z",
       |        "previousLogin": null
       |    },
       |    "optionalCredentials": {
       |        "providerId": "4911434741952698",
       |        "providerType": "GovernmentGateway"
       |    },
       |    "authProviderId": {
       |        "ggCredId": "xyz"
       |    },
       |    "externalId": "testExternalId"
       |}
       |""".stripMargin

  private def stubGet[T](key: CacheReadKey[T], value: T): Unit =
    when(mockCachingService.get[T](eqTo(key))(any())).thenReturn(Future.successful(Some(value)))

  private def stubPut[T](key: CacheReadWriteKey[T], value: T): Unit =
    when(mockCachingService.put[T](eqTo(key), any())(any(), any())).thenReturn(Future.successful(value))

  private def stubCacheReads(): Unit = {
    stubGet(CACHE_RELATIONSHIP_RECORDS, relationshipRecords)
    stubGet(CACHE_DIVORCE_DATE, date)
    stubGet(CACHE_MARRIAGE_DATE, dateOfMarriageInput)
    stubGet(CACHE_CHOOSE_YEARS, "currentTaxYear,previousTaxYears")
    stubGet(CACHE_CHECK_CLAIM_OR_CANCEL, "checkMarriageAllowanceClaim")
    stubGet(CACHE_MAKE_CHANGES_DECISION, "Divorce")
    stubGet(USER_ANSWERS_UPDATE_CONFIRMATION, confirmationUpdateAnswersCacheData)
    stubGet(CACHE_NOTIFICATION_RECORD, NotificationRecord(EmailAddress(email)))
    stubGet(CACHE_EMAIL_ADDRESS, EmailAddress(email))
    stubGet(CACHE_RECIPIENT_DETAILS, recipientDetails)
    stubGet(CACHE_CANCELLATION_CONFIRMATION, cancellationConfirmation)
  }

  private def stubCacheWrites(): Unit = {
    stubPut(CACHE_MA_ENDING_DATES, maEndingDates)
    stubPut(CACHE_LOCKED_CREATE, false)
    stubPut(CACHE_TRANSFEROR_RECORD, userRecord)
    stubPut(CACHE_RELATIONSHIP_RECORDS, relationshipRecords)
    stubPut(CACHE_RECIPIENT_DETAILS, recipientDetails)

    when(mockCachingService.clear()(any())).thenReturn(Future.successful(()))
  }

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[CachingService].toInstance(mockCachingService)
      )
      .configure(
        "microservice.services.auth.port"               -> server.port(),
        "microservice.services.pertax-auth.port"        -> server.port(),
        "microservice.services.marriage-allowance.port" -> server.port()
      )
      .build()

  override def beforeEach(): Unit = {
    super.beforeEach()

    server.stubFor(
      post(urlEqualTo("/auth/authorise"))
        .willReturn(aResponse().withBody(authResponse))
    )

    server.stubFor(
      post(urlEqualTo("/pertax/authorise"))
        .willReturn(aResponse().withBody("{\"code\": \"ACCESS_GRANTED\", \"message\": \"Access granted\"}"))
    )

    server.stubFor(
      get(urlEqualTo(s"/paye/$generatedNino/list-relationship"))
        .willReturn(
          ok.withBody(Json.toJson(maConnectorResponse).toString())
        )
    )

    stubCacheReads()
    stubCacheWrites()
  }
}
