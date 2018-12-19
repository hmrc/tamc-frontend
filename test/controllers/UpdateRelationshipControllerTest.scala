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

package controllers

import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.{AnyContent, Cookie, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CachingService, TimeService, TransferService, UpdateRelationshipService}
import test_utils._
import test_utils.data.RelationshipRecordData
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time.TaxYearResolver

import scala.concurrent.Future

class UpdateRelationshipControllerTest extends UpdateRelationshipTestUtility {

  //NOTE: These vals will ruin the mock counts
  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]

  def controller(auth: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner]): UpdateRelationshipController =
    new UpdateRelationshipController(
      messagesApi,
      auth,
      mockUpdateRelationshipService,
      mockRegistrationService,
      mockCachingService,
      instanceOf[TimeService]
  ) {
    override val templateRenderer: TemplateRenderer = MockTemplateRenderer
    override val formPartialRetriver: FormPartialRetriever = MockFormPartialRetriever
  }

  "History" should {
    "redirect to transfer" when {
      "has no active record, no historic and temporary authentication" in{
        when(mockUpdateRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful(
              (RelationshipRecordList(None, None, None, activeRecord = false, historicRecord = false, historicActiveRecord = false), true)
            )
          )
        val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.transfer().url)
      }
    }

    "redirect to how-it-works" when {
      "has no active record, no historic and permanent authentication" in {
        when(mockUpdateRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful(
              (RelationshipRecordList(None, None, None, activeRecord = false, historicRecord = false, historicActiveRecord = false),true)
            )
          )
        val result: Future[Result] = controller().history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.howItWorks().url)
      }
    }

    "load change of circumstances page" when {
      "has some active record" in {
        when(mockUpdateRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful((RelationshipRecordData.activeRelationshipRecordList, false))
          )


        val result: Future[Result] = controller().history()(request)
        status(result) shouldBe OK
      }

      "has some historic record" in {
        when(mockUpdateRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful((RelationshipRecordData.historicRelationshipRecordList,true))
          )
        val result: Future[Result] = controller().history()(request)
        status(result) shouldBe OK
      }
    }
  }

  "makeChange" should {
    "return successful response" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "role" → "some role",
          "endReason" → "some end reason",
          "historicActiveRecord" → "true"
        )
        val result = controller().makeChange()(request)
        status(result) shouldBe OK
      }
    }

    "redirect the user" when {
      "an invalid form with errors is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("historicActiveRecord" → "string")
        val result = controller().makeChange()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.UpdateRelationshipController.history().url)
      }
    }
  }

  class UpdateRelationshipActionTest(endReason: String) {
    val request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody(
      "role" → "some role",
      "endReason" → endReason,
      "historicActiveRecord" → "true",
      "creationTimestamp" → "timestamp",
      "dateOfDivorce" → new LocalDate(TaxYearResolver.currentTaxYear, 6, 12).toString()
    )
    when(mockCachingService.saveRoleRecord(any())(any(), any())).thenReturn("OK")
    when(mockUpdateRelationshipService.saveEndRelationshipReason(any())(any(), any()))
      .thenReturn(Future.successful(EndRelationshipReason("")))
    val result: Future[Result] = controller().updateRelationshipAction()(request)
    lazy val document: Document = Jsoup.parse(contentAsString(result))
  }

  "updateRelationshipAction" should {
    "return a success" when {
      "the end reason code is DIVORCE" in new UpdateRelationshipActionTest("DIVORCE") {
        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("title.divorce")
      }

      "the end reason code is EARNINGS" in new UpdateRelationshipActionTest("EARNINGS") {
        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("change.status.earnings.h1")
      }

      "the end reason code is BEREAVEMENT" in new UpdateRelationshipActionTest("BEREAVEMENT") {
        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("change.status.bereavement.sorry")
      }
    }

    "redirect the user" when {
      "the end reason code is CANCEL" in new UpdateRelationshipActionTest("CANCEL") {
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmCancel().url)
      }

      "the end reason code is REJECT" in new UpdateRelationshipActionTest("REJECT") {
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmReject().url)
      }
    }

    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("role" → "ROLE", "historicActiveRecord" -> "string")
        val result: Future[Result] = controller().updateRelationshipAction()(request)
        status(result) shouldBe BAD_REQUEST
      }

      "an unrecognised end reason is submitted" in new UpdateRelationshipActionTest("DIVORCE_PY") {
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "changeOfIncome" should {
    "return success" in {
      status(controller().changeOfIncome()(request)) shouldBe OK
    }
  }

  "bereavement" should {
    "return success" in {
      status(controller().bereavement()(request)) shouldBe OK
    }
  }

  "confirmYourEmailActionUpdate" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("transferor-email" → "not a real email")
        val result = controller().confirmYourEmailActionUpdate()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect" when {
      "a successful form is submitted" in {
        val email = "example@example.com"
        val record = NotificationRecord(EmailAddress(email))
        when(mockRegistrationService.upsertTransferorNotification(ArgumentMatchers.eq(record))(any(), any()))
          .thenReturn(record)
        val request = FakeRequest().withFormUrlEncodedBody("transferor-email" → email)
        val result = controller().confirmYourEmailActionUpdate()(request)
        status(result) shouldBe SEE_OTHER
      }
    }
  }

  "divorceYear" should {
    "return a success" when {
      "there is cache data returned" in {
        when(mockUpdateRelationshipService.getUpdateRelationshipCacheDataForDateOfDivorce(any(), any())).thenReturn(
          Some(UpdateRelationshipCacheData(None, Some(""), relationshipEndReasonRecord = Some(EndRelationshipReason("")), notification = None))
        )
        val result = controller().divorceYear()(request)

        status(result) shouldBe OK
      }
    }

    "return InternalServerError" when {
      "there is no cache data" in {
        when(mockUpdateRelationshipService.getUpdateRelationshipCacheDataForDateOfDivorce(any(), any())).thenReturn(None)
        status(controller().divorceYear()(request)) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "divorceSelectYear" should {
    "return bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("role"→ "")
        val result = controller().divorceSelectYear()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }
//TODO fix... always returning bad request. possibly due to divorce date on form - not sure
    "return a success" when {
      def request = FakeRequest().withFormUrlEncodedBody(
        "role"→ "some role",
        "endReason" → "DIVORCE",
        "historicActiveRecord" → "true",
        "creationTimeStamp" → "timestamp",
        "dateOfDivorce" -> LocalDate.now.minusDays(1).toString()
      )

      "divorce date is valid" in {
        when(mockUpdateRelationshipService.isValidDivorceDate(any())(any(), any())).thenReturn(true)
        val result = controller().divorceSelectYear()(request)
        lazy val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("title.make-a-change")
      }

      "divorce date is invalid" in {
        when(mockUpdateRelationshipService.isValidDivorceDate(any())(any(), any())).thenReturn(false)
        val result = controller().divorceSelectYear()(request)
        lazy val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("title.divorce")
      }
    }
  }

  "Update relationship email notification " should {

    "save a valid email and redirect to confirmation page" in {
      val email = "example@example.com"
      val record = NotificationRecord(EmailAddress(email))
      val request = FakeRequest().withFormUrlEncodedBody(data = "transferor-email" -> email)
      when(mockRegistrationService.upsertTransferorNotification(ArgumentMatchers.eq(record))(any(), any()))
        .thenReturn(Future.successful(record))
      val result = controller().confirmYourEmailActionUpdate(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/confirm-change")
      verify(mockRegistrationService, times(1)).upsertTransferorNotification(ArgumentMatchers.eq(record))(any(), any())
    }

    "read from keystore and display email field" in {
      val email = "example@example.com"
      val record = NotificationRecord(EmailAddress(email))
      when(mockUpdateRelationshipService.getUpdateNotification(any(), any())).thenReturn(Some(record))
      val result = controller().confirmEmail(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      verify(mockUpdateRelationshipService, times(1))
      document.getElementById("transferor-email").attr("value") shouldBe email
    }
  }

  "Update relationship caching data " should {

    "have divorce previous year in end relationship cache " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_PY)
      val result = controllerToTest.divorceAction()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("DIVORCE_PY"))
    }

    "have divorce previous year in end relationship cache (with divorce date)" in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(
        "endReason" -> EndReasonCode.DIVORCE_PY,
        "dateOfDivorce.day" -> "20",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2015")
      val result = controllerToTest.divorceAction()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("DIVORCE_PY", Some(new LocalDate(2015, 1, 20))))
    }

    "have divorce current year in end relationship cache " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody("endReason" -> EndReasonCode.DIVORCE_CY)
      val result = controllerToTest.divorceAction()(request)

      status(result) shouldBe SEE_OTHER
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason("DIVORCE_CY"))
    }

    "have cancel in end relationship cache " in {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmCancel()(request)

      status(result) shouldBe OK
      controllerToTest.relationshipEndReasonCount shouldBe 1
      controllerToTest.relationshipEndReasonRecord shouldBe Some(EndRelationshipReason(EndReasonCode.CANCEL))
    }

    "have update relationship details in cache with Cancel end reason" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord =  RelationshipRecord(Role.RECIPIENT, "", "20150101", Some(""), Some("20160101"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser), roleRecord = Some(""),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK

    }

    "have update relationship details in cache with rejection reason and end date from service call" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "", "20150101", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser), roleRecord = Some(""),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.REJECT)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK
    }

    "list correct tax year and have update relationship details in cache with rejection reason and history" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "", Some(""), Some(""), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20100406", Some(""), Some("20150405"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        roleRecord = Some(Role.RECIPIENT),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdate()(request)

      status(result) shouldBe OK

      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("confirm-page").text() shouldBe "Confirm removal of a previous Marriage Allowance claim"
      document.getElementById("confirm-note").text() shouldBe "You have asked us to remove your Marriage Allowance from tax year 2010 to 2015. This means:"
    }

    "have update relationship action details in cache " in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(EndReasonCode.CANCEL)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
    }

    "reject active relationship" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "20120101", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord), notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT)), relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
    }

    "reject historic relationship" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "", Some(""), Some(""), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20130101", Some(""), Some("1-01-2014"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmUpdateAction()(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/marriage-allowance-application/finished-change")
    }

    "confirm rejection of relationship" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val historic1Record = RelationshipRecord(Role.TRANSFEROR, "56789", "", Some(""), Some(""), "", "")
      val historic2Record = RelationshipRecord(Role.RECIPIENT, "98765", "20130101", Some(""), Some("1-01-2014"), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        historicRelationships = Some(Seq(historic1Record, historic2Record)),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.confirmReject()(request)

      status(result) shouldBe OK
    }

    "Finish update after change of circumstances journey for recipient rejection" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.RECIPIENT, "123456", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finishUpdate()(request)

      status(result) shouldBe OK
    }

    "Finish update after change of circumstances journey for transferor rejects" in {
      val loggedInUser = LoggedInUserInfo(999700100, "2015", None, TestConstants.GENERIC_CITIZEN_NAME)
      val relationshipRecord = RelationshipRecord(Role.TRANSFEROR, "123456", "", Some(""), Some(""), "", "")
      val updateRelationshipCacheData = UpdateRelationshipCacheData(
        loggedInUserInfo = Some(loggedInUser),
        activeRelationshipRecord = Some(relationshipRecord),
        notification = Some(NotificationRecord(EmailAddress("example@example.com"))),
        relationshipEndReasonRecord = Some(EndRelationshipReason(endReason = EndReasonCode.CANCEL, timestamp = Some("98765"))),
        relationshipUpdated = Some(false))

      val testComponent = makeUpdateRelationshipTestComponent("coc_active_relationship", transferorRecipientData = Some(updateRelationshipCacheData))
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.finishUpdate()(request)

      status(result) shouldBe OK
    }

    "select divorce year for update relationship" in {
      val transferorRecipientData = Some(UpdateRelationshipCacheData(None, None,
        Some(RelationshipRecord(Role.RECIPIENT, "1234567890", "20150101", None, None, Role.TRANSFEROR, "1234567890")),
        None, None, None, None))

      val testComponent = makeUpdateRelationshipTestComponent(dataId = "coc_active_relationship", transferorRecipientData = transferorRecipientData)
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withFormUrlEncodedBody(
        "role" -> Role.RECIPIENT,
        "endReason" -> EndReasonCode.DIVORCE_PY,
        "historicActiveRecord" -> "false",
        "creationTimestamp" -> "1234567890",
        "dateOfDivorce.day" -> "20",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2015")

      val result = controllerToTest.divorceSelectYear()(request)
      status(result) shouldBe OK
    }

  }

  "Calling history page" should {
    //CONTENT
    "show 'Cancel Marriage Allowance' button on ’History’ page with PTA journey" ignore {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "PTA"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }
    //CONTENT

    "show 'Cancel Marriage Allowance' button on ’History’ page with GDS journey" ignore {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request.withCookies(Cookie("TAMC_JOURNEY", "GDS"))
      val result = controllerToTest.history()(request)

      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById("cancel-marriage-allowance").text() shouldBe "Cancel Marriage Allowance"
    }
    //CONTENT

    "show sign-out on ’History’ page along with message" ignore {
      val testComponent = makeUpdateRelationshipTestComponent("coc_active_historic_relationship")
      val controllerToTest = testComponent.controller
      val request = testComponent.request
      val result = controllerToTest.history()(request)
      status(result) shouldBe OK
      val document = Jsoup.parse(contentAsString(result))
      val article = document.getElementsByTag("article")
      article.text().contains("Marriage Allowance Foo Bar") shouldBe true
    }
  }
}
