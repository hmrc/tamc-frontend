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

package controllers

import controllers.actions.AuthenticatedActionRefiner
import errors._
import forms.coc.{CheckClaimOrCancelDecisionForm, MakeChangesDecisionForm}
import models._
import models.auth.{AuthenticatedUserRequest, PermanentlyAuthenticated}
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{TimeService, _}
import test_utils.TestData.Ninos
import test_utils._
import test_utils.data.{ConfirmationModelData, RelationshipRecordData}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time

import scala.concurrent.Future
import scala.language.postfixOps

class UpdateRelationshipControllerTest extends ControllerBaseSpec {

  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockListRelationshipService: ListRelationshipService = mock[ListRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  private val failedFuture: Future[Nothing] = Future.failed(new RuntimeException("test"))

  def controller(auth: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner],
                 timeService: TimeService = mockTimeService): UpdateRelationshipController =
    new UpdateRelationshipController(
      messagesApi,
      auth,
      mockUpdateRelationshipService,
      mockRegistrationService,
      mockCachingService,
      timeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])


  class UpdateRelationshipActionTest(endReason: String) {
    lazy val document: Document = Jsoup.parse(contentAsString(result))
    when(mockCachingService.saveRoleRecord(any())(any(), any())).thenReturn(Future.successful("OK"))
    when(mockUpdateRelationshipService.saveEndRelationshipReason(any())(any(), any()))
      .thenReturn(Future.successful(EndRelationshipReason("")))
    val request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody(
      "role" -> "some role",
      "endReason" -> endReason,
      "historicActiveRecord" -> "true",
      "creationTimestamp" -> "timestamp",
      "dateOfDivorce" -> new LocalDate(time.TaxYear.current.startYear, 6, 12).toString()
    )
    val result: Future[Result] = controller().updateRelationshipAction()(request)
  }

  //TODO remove?!
  "history" should {
    "redirect to transfer" when {
      "has no active record, no active historic and temporary authentication" in {
        val relationship = RelationshipRecords(None, None, None)

        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.successful(relationship))

        val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.transfer().url)
      }
    }

    "redirect to how-it-works" when {
      "has no active record, no active historic and permanent authentication" in {
        val relationship = RelationshipRecords(None, None, None)

        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.successful(relationship))

        val result: Future[Result] = controller().history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.howItWorks().url)
      }
    }

    "load change of circumstances page" when {
      "active record" in {
        val cid = 1122L
        val timeStamp = new LocalDate().toString
        val hasAllowance = None
        val citizenName = CitizenName(Some("Test"), Some("User"))
        val loggedInUserInfo = Some(LoggedInUserInfo(cid, timeStamp, hasAllowance, Some(citizenName)))

        val relationship = RelationshipRecords(
          Some(activeRecipientRelationshipRecord),
          Some(Seq(inactiveRecipientRelationshipRecord1)),
          loggedInUserInfo)

        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.successful(relationship))
        when(mockUpdateRelationshipService.saveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.successful(relationship))

        val result: Future[Result] = controller().history()(request)
        status(result) shouldBe OK
      }

    }
  }

  "historyWithCy" should {
    "redirect to history, with a welsh language setting" in {
      val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).historyWithCy()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.history().url)
      val resolved = awaitResult(result)
      resolved.header.headers.keys should contain("Set-Cookie")
      resolved.header.headers("Set-Cookie") should include("PLAY_LANG=cy")
    }
  }

  "historyWithEn" should {
    "redirect to history, with an english language setting" in {
      val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).historyWithEn()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.history().url)
      val resolved = awaitResult(result)
      resolved.header.headers.keys should contain("Set-Cookie")
      resolved.header.headers("Set-Cookie") should include("PLAY_LANG=en")
    }
  }

  "makeChange" should {
    "there are no data in cache" in {
      val request = FakeRequest()
      when(mockUpdateRelationshipService.getMakeChangesDecision(any(), any()))
        .thenReturn(Future.successful(None))

      val result = controller().makeChange()(request)
      status(result) shouldBe OK
    }

    "there are data in cache but is wrong one" in {
      val request = FakeRequest()
      when(mockUpdateRelationshipService.getMakeChangesDecision(any(), any()))
        .thenReturn(Future.successful(Some("")))

      val result = controller().makeChange()(request)
      status(result) shouldBe OK
    }

    "there are data in cache and is valid" in {
      val request = FakeRequest()
      when(mockUpdateRelationshipService.getMakeChangesDecision(any(), any()))
        .thenReturn(Future.successful(Some(MakeChangesDecisionForm.Divorce)))

      val result = controller().makeChange()(request)
      status(result) shouldBe OK
    }
  }

  "submitDecision" should {

    "bad request" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "tralalal" -> Transferor.asString()
      )

      val result = controller().submitDecision(request)
      status(result) shouldBe BAD_REQUEST
    }

    "checkMarriageAllowanceClaim" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        CheckClaimOrCancelDecisionForm.DecisionChoice -> CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim
      )

      when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(any())(any(), any()))
        .thenReturn(Future.successful(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim))

      val result = controller().submitDecision(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.claims().url)
    }

    "stopMarriageAllowance" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        CheckClaimOrCancelDecisionForm.DecisionChoice -> CheckClaimOrCancelDecisionForm.StopMarriageAllowance
      )

      when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(any())(any(), any()))
        .thenReturn(Future.successful(CheckClaimOrCancelDecisionForm.StopMarriageAllowance))

      val result = controller().submitDecision(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.makeChange().url)
    }
  }

  "claims" should {
    "return success" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Transferor.asString(), "", "20130101", None, None, "", "")), None, None)
          )
        )

      val result: Future[Result] = controller().claims(request)
      status(result) shouldBe OK
    }
  }

  "submitMakeChange" should {

    "bad request" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "tralalal" -> Transferor.asString()
      )

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe BAD_REQUEST
    }

    "divorce" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.Divorce
      )
      when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(any())(any(), any()))
        .thenReturn(Future.successful(MakeChangesDecisionForm.Divorce))

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.divorceEnterYear().url)
    }

    "income change(recipient)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.IncomeChanges
      )

      when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(any())(any(), any()))
        .thenReturn(Future.successful(MakeChangesDecisionForm.IncomeChanges))
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Recipient.asString(), "", "", None, None, "", "")), None, None)
          )
        )

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.stopAllowance().url)
    }

    "income change(transferor)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.IncomeChanges
      )

      when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(any())(any(), any()))
        .thenReturn(Future.successful(MakeChangesDecisionForm.IncomeChanges))
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Transferor.asString(), "", "", None, None, "", "")), None, None)
          )
        )

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.changeOfIncome().url)
    }

    "no longer required(recipient)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.NoLongerRequired
      )

      when(mockUpdateRelationshipService.saveMakeChangeDecision(any())(any(), any()))
        .thenReturn(Future.successful(MakeChangesDecisionForm.NoLongerRequired))
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Recipient.asString(), "", "", None, None, "", "")), None, None)
          )
        )

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.stopAllowance().url)
    }

    "no longer required(transferor)" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.NoLongerRequired
      )

      when(mockUpdateRelationshipService.saveMakeChangeDecision(any())(any(), any()))
        .thenReturn(Future.successful(MakeChangesDecisionForm.NoLongerRequired))
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Transferor.asString(), "", "", None, None, "", "")), None, None)
          )
        )

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.cancel().url)
    }

    "bereavement" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.Bereavement
      )

      when(mockUpdateRelationshipService.saveMakeChangeDecision(any())(any(), any()))
        .thenReturn(Future.successful(MakeChangesDecisionForm.Bereavement))

      val result = controller().submitMakeChange()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.bereavement().url)
    }
  }

  "stopAllowance" should {
    "return success" in {
      val result: Future[Result] = controller().stopAllowance(request)
      status(result) shouldBe OK
    }
  }

  "cancel" should {
    "return success" in {
      val result: Future[Result] = controller().cancel(request)
      status(result) shouldBe OK
    }
  }

  "changeOfIncome" should {
    "return success" in {
      val result: Future[Result] = controller().changeOfIncome(request)
      status(result) shouldBe OK
    }
  }

  "bereavement" should {
    "return success is data in cache" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(Some(RelationshipRecord(Recipient.asString(), "", "", None, None, "", "")), None, None)
          )
        )
      status(controller().bereavement(request)) shouldBe OK
    }

    //TODO finish this test case as per implmenetation from models.RelationshipRecords.role case not found
    "return success is no data in cache" in {
      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
        .thenReturn(
          Future.successful(
            RelationshipRecords(None, None, None)
          )
        )

      status(controller().bereavement(request)) shouldBe OK
    }
  }


  "divorceEnterYear" should {
    "return success is data in cache" in {
      when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
        .thenReturn(Future.successful(Some(LocalDate.now().minusDays(1))))

      status(controller().divorceEnterYear(request)) shouldBe OK
    }

    "return success is no data in cache" in {
      when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
        .thenReturn(Future.successful(None))

      status(controller().divorceEnterYear(request)) shouldBe OK
    }

    "return success fail to get from cache" in {
      when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
        .thenReturn(failedFuture)

      status(controller().divorceEnterYear(request)) shouldBe OK
    }
  }

  "submitDivorceEnterYear" should {
    "an invalid form is submitted" in {
      val request = FakeRequest().withFormUrlEncodedBody("role" -> "ROLE", "historicActiveRecord" -> "string")
      val result: Future[Result] = controller().submitDivorceEnterYear(request)
      status(result) shouldBe BAD_REQUEST
    }

    "return success and save data in cache" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> "1",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2000"
      )

      when(mockUpdateRelationshipService.saveDivorceDate(any())(any(), any()))
        .thenReturn(Future.successful(LocalDate.now().minusDays(1)))

      val result = controller().submitDivorceEnterYear()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.divorceEndExplanation().url)
    }
  }

  "divorceEndExplanation" should {
    "return success there is data in cache" in {
      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> "1",
        "dateOfDivorce.month" -> "1",
        "dateOfDivorce.year" -> "2000"
      )

      when(mockUpdateRelationshipService.getDivorceExplanationData(any(), any()))
        .thenReturn(Future.successful((Transferor, LocalDate.now().minusDays(1))))

      val result = controller().divorceEndExplanation()(request)
      status(result) shouldBe OK
    }
  }

  "confirmYourEmailActionUpdate" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("transferor-email" -> "not a real email")
        val result = controller().confirmYourEmailActionUpdate()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect" when {
      "a successful form is submitted" in {
        val email = "example@example.com"
        val record = NotificationRecord(EmailAddress(email))
        when(mockRegistrationService.upsertTransferorNotification(ArgumentMatchers.eq(record))(any(), any()))
          .thenReturn(Future.successful(record))
        val request = FakeRequest().withFormUrlEncodedBody("transferor-email" -> email)
        val result = controller().confirmYourEmailActionUpdate()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmUpdate().url)
        verify(mockRegistrationService, times(1))
          .upsertTransferorNotification(ArgumentMatchers.eq(record))(any(), any())
      }
    }
  }

  "confirmEmail" should {
    "return a success" when {
      "fail to get data from cache" in {
        when(mockUpdateRelationshipService.getUpdateNotification(any(), any()))
          .thenReturn(failedFuture)
        val result = controller().confirmEmail(request)
        status(result) shouldBe OK
      }

      "an email is recovered from the cache" in {
        val email = "test@test.com"
        when(mockUpdateRelationshipService.getUpdateNotification(any(), any()))
          .thenReturn(Future.successful(Some(NotificationRecord(EmailAddress(email)))))
        val result = controller().confirmEmail(request)
        status(result) shouldBe OK
        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("transferor-email").attr("value") shouldBe email
      }

      "no email is recovered from the cache" in {
        when(mockUpdateRelationshipService.getUpdateNotification(any(), any()))
          .thenReturn(Future.successful(None))
        val result = controller().confirmEmail(request)
        status(result) shouldBe OK
      }
    }
  }

  "confirmReject" should {
    "return a success" when {
      "data is returned from the cache" in {
        val relationshipCacheData = Some(RelationshipRecordData.updateRelationshipCacheData)
        when(mockUpdateRelationshipService.getUpdateRelationshipCacheForReject(any(), any()))
          .thenReturn(Future.successful(relationshipCacheData))
        when(mockUpdateRelationshipService.getRelationship(ArgumentMatchers.eq(relationshipCacheData.get)))
          .thenReturn(RelationshipRecordData.activeRecord)
        when(mockUpdateRelationshipService.getEndDate(ArgumentMatchers.eq(relationshipCacheData.get.relationshipEndReasonRecord.get),
          ArgumentMatchers.eq(RelationshipRecordData.activeRecord)))
          .thenReturn(LocalDate.now())
        val result = controller().confirmReject()(request)
        status(result) shouldBe OK
      }
    }
  }

  "confirmCancel" should {
    "return a success" when {
      "end reason is successfully cached" in {
        val endReason = EndRelationshipReason(EndReasonCode.CANCEL)
        when(mockUpdateRelationshipService.saveEndRelationshipReason(ArgumentMatchers.eq(endReason))(any(), any()))
          .thenReturn(Future.successful(endReason))

        val result = controller(timeService = TimeService).confirmCancel
        result shouldBe OK
      }
    }
  }

  "getConfirmationInfoFromReason" should {
    "return a relationship end date, a relevant date and isEnded true" when {
      "the end reason code is Reject, the end date from relationship service is not blank" in {
        val relationshipEndDate = LocalDate.now()
        val endDate = LocalDate.now.minusDays(1)
        val endRelationshipReason = EndRelationshipReason(EndReasonCode.REJECT)

        when(mockUpdateRelationshipService.getRelationship(ArgumentMatchers.eq(RelationshipRecordData.updateRelationshipCacheData)))
          .thenReturn(RelationshipRecordData.activeRecord)
        when(mockUpdateRelationshipService.getRelationEndDate(ArgumentMatchers.eq(RelationshipRecordData.activeRecord)))
          .thenReturn(relationshipEndDate)
        when(mockUpdateRelationshipService.getEndDate(ArgumentMatchers.eq(endRelationshipReason), ArgumentMatchers.eq(RelationshipRecordData.activeRecord)))
          .thenReturn(endDate)

        val result = controller().getConfirmationInfoFromReason(endRelationshipReason, RelationshipRecordData.updateRelationshipCacheData)
        result shouldBe(true, Some(relationshipEndDate), Some(endDate))
      }
    }

    "return no relationship end date, a relevant date and isEnded false" when {
      "the end reason code is Reject, the end date from relationship service is blank" in {
        val endRelationshipReason = EndRelationshipReason(EndReasonCode.REJECT)
        val endDate = LocalDate.now.minusDays(1)

        when(mockUpdateRelationshipService.getRelationship(ArgumentMatchers.eq(RelationshipRecordData.updateRelationshipCacheData)))
          .thenReturn(RelationshipRecordData.activeRecordWithNoEndDate)
        when(mockUpdateRelationshipService.getEndDate(ArgumentMatchers.eq(endRelationshipReason), ArgumentMatchers.eq(RelationshipRecordData.activeRecordWithNoEndDate)))
          .thenReturn(endDate)

        val result = controller().getConfirmationInfoFromReason(endRelationshipReason, RelationshipRecordData.updateRelationshipCacheData)
        result shouldBe(false, None, Some(endDate))
      }

      "the end reason code is DIVORCE_PY" in {
        val endRelationshipReason = EndRelationshipReason(EndReasonCode.DIVORCE_PY)
        val date = LocalDate.now()
        when(mockTimeService.getEffectiveDate(ArgumentMatchers.eq(endRelationshipReason)))
          .thenReturn(date)

        val result = controller().getConfirmationInfoFromReason(endRelationshipReason, RelationshipRecordData.updateRelationshipCacheData)
        result shouldBe(false, None, Some(date))
      }

      "the end reason code is DIVORCE_CY" in {
        val endRelationshipReason = EndRelationshipReason(EndReasonCode.DIVORCE_CY)
        val date = LocalDate.now()
        when(mockTimeService.getEffectiveUntilDate(ArgumentMatchers.eq(endRelationshipReason)))
          .thenReturn(Some(date))

        val result = controller().getConfirmationInfoFromReason(endRelationshipReason, RelationshipRecordData.updateRelationshipCacheData)
        result shouldBe(false, None, Some(date))
      }

      "the end reason code is CANCEL" in {
        val endRelationshipReason = EndRelationshipReason(EndReasonCode.CANCEL)
        val date = LocalDate.now()
        when(mockTimeService.getEffectiveUntilDate(ArgumentMatchers.eq(endRelationshipReason)))
          .thenReturn(Some(date))

        val result = controller().getConfirmationInfoFromReason(endRelationshipReason, RelationshipRecordData.updateRelationshipCacheData)
        result shouldBe(false, None, Some(date))
      }
    }

  }

  "confirmUpdateAction" should {
    "redirect the user" when {
      "update relationship returns a future successful" in {
        when(mockUpdateRelationshipService.updateRelationship(any())(any(), any(), any()))
          .thenReturn(Future.successful(RelationshipRecordData.notificationRecord))

        val result = controller().confirmUpdateAction(request)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.finishUpdate().url)
      }
    }
  }

  "finishUpdate" should {
    "return a success" when {
      "an email is available" in {
        when(mockUpdateRelationshipService.getupdateRelationshipFinishedData(any())(any(), any()))
          .thenReturn(Future.successful((RelationshipRecordData.notificationRecord, EndRelationshipReason(EndReasonCode.REJECT))))

        val result = controller().finishUpdate()(request)
        status(result) shouldBe OK
      }
    }
  }

  "handleError" should {
    val auhtRequest: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(
      request,
      PermanentlyAuthenticated,
      Some(ConfidenceLevel.L200),
      isSA = false,
      Some("GovernmentGateway"),
      Nino(TestData.Ninos.nino1)
    )

    "return internal server error" when {
      val errors = List(
        (new CacheMissingUpdateRecord, "technical.issue.heading"),
        (new CacheUpdateRequestNotSent, "technical.issue.heading"),
        (new CannotUpdateRelationship, "technical.issue.heading"),
        (new CitizenNotFound, "technical.cannot-find-details.para1"),
        (new BadFetchRequest, "technical.technical-error.para1"),
        (new Exception, "technical.issue.heading")
      )

      for ((error, message) <- errors) {
        s"a $error has been thrown" in {
          val result = Future.successful(controller().handleError(HeaderCarrier(), auhtRequest)(error))
          status(result) shouldBe INTERNAL_SERVER_ERROR
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("error").text() shouldBe messagesApi(message)
        }
      }
    }

    "return OK" when {
      val errors = List(
        (new TransferorNotFound, "transferor.not.found"),
        (new RecipientNotFound, "recipient.not.found.para1")
      )

      for ((error, message) <- errors) {
        s"a $error has been thrown" in {
          val result = Future.successful(controller().handleError(HeaderCarrier(), auhtRequest)(error))
          status(result) shouldBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("error").text() shouldBe messagesApi(message)
        }
      }
    }

    "redirect" when {
      "a errors.CacheRelationshipAlreadyUpdated exception has been thrown" in {
        val result = Future.successful(controller().handleError(HeaderCarrier(), auhtRequest)(new CacheRelationshipAlreadyUpdated))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.finishUpdate().url)
      }
    }
  }

  "decision" should {
    "not found data in cache" when {

      "failed to get data from cache should return INTERNAL_SERVER_ERROR" in {
        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any()))
          .thenReturn(failedFuture)
        val result = controller().decision(request)
        status(result) shouldBe OK
      }

      "get None from cache should return OK" in {
        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(None))
        val result = controller().decision(request)
        status(result) shouldBe OK
      }

    }

    "found data in cache" when {

      "get data from cache should return OK" in {
        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(Some("wooby dooby")))
        val result = controller().decision(request)
        status(result) shouldBe OK
      }

      "get invalid data from cache should return OK" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "gender" -> "M",
          "nino" -> Ninos.nino1,
          "transferor-email" -> "example@example.com"
        )
        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(Some("wooby dooby")))
        val result = controller().decision(request)
        status(result) shouldBe OK
      }

    }
  }

  //TODO fix/rewrite after John implementation
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
        val request = FakeRequest().withFormUrlEncodedBody("role" -> "ROLE", "historicActiveRecord" -> "string")
        val result: Future[Result] = controller().updateRelationshipAction()(request)
        status(result) shouldBe BAD_REQUEST
      }

      "an unrecognised end reason is submitted" in new UpdateRelationshipActionTest("DIVORCE_PY") {
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "divorceYear" should {
    "return a success" when {
      "there is cache data returned" in {
        when(mockUpdateRelationshipService.getUpdateRelationshipCacheDataForDateOfDivorce(any(), any())).thenReturn(
          Future.successful(Some(UpdateRelationshipCacheData(None, Some(""), relationshipEndReasonRecord = Some(EndRelationshipReason("")), notification = None)))
        )
        val result = controller().divorceYear(request)

        status(result) shouldBe OK
      }
    }

    "return InternalServerError" when {
      "there is no cache data" in {
        when(mockUpdateRelationshipService.getUpdateRelationshipCacheDataForDateOfDivorce(any(), any()))
          .thenReturn(Future.successful(None))
        status(controller().divorceYear(request)) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "divorceAction" should {
    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "role" -> "some role",
          "endReason" -> "invalid end reason",
          "historicActiveRecord" -> "true",
          "creationTimeStamp" -> "timestamp",
          "dateOfDivorce.day" -> "1",
          "dateOfDivorce.month" -> "1",
          "dateOfDivorce.year" -> "2000"
        )
        val result = controller().divorceAction()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "redirect the user" when {
      "EndRelationshipReason is pulled from the cache" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "role" -> "some role",
          "endReason" -> "DIVORCE_CY",
          "historicActiveRecord" -> "true",
          "creationTimeStamp" -> "timestamp",
          "dateOfDivorce.day" -> "1",
          "dateOfDivorce.month" -> "1",
          "dateOfDivorce.year" -> "2000"
        )
        when(mockUpdateRelationshipService.saveEndRelationshipReason(any())(any(), any()))
          .thenReturn(Future.successful(EndRelationshipReason("DIVORCE_CY")))
        val result = controller().divorceAction()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmEmail().url)
      }
    }
  }

  "confirmUpdate" should {
    "return a success" when {
      "the relationship service successfully returns cache data and end relationship reason" in {
        when(mockUpdateRelationshipService.getConfirmationUpdateDataTemp(any(), any()))
          .thenReturn(Future.successful((ConfirmationModelData.updateRelationshipConfirmationModel, Some(RelationshipRecordData.updateRelationshipCacheData))))
        when(mockTimeService.getEffectiveDate(any()))
          .thenReturn(LocalDate.now())
        val result = controller().confirmUpdate()(request)
        status(result) shouldBe OK
      }
    }

    "return InternalServerError" when {
      "there is no cache data returned" in {
        when(mockUpdateRelationshipService.getConfirmationUpdateDataTemp(any(), any()))
          .thenReturn(Future.successful((ConfirmationModelData.updateRelationshipConfirmationModel, None)))
        val result = controller().confirmUpdate()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

}
