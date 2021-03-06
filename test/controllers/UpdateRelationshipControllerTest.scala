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

package controllers

import errors._
import forms.EmailForm.emailForm
import forms.coc.{CheckClaimOrCancelDecisionForm, DivorceSelectYearForm, MakeChangesDecisionForm}
import models._
import models.auth.AuthenticatedUserRequest
import java.time.LocalDate

import controllers.actions.AuthenticatedActionRefiner
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services._
import test_utils._
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.domain.{Generator, Nino}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.RequestBuilder._
import utils.{ControllerBaseTest, MockAuthenticatedAction, MockTemplateRenderer}
import viewModels._
import play.api.inject.bind
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future

class UpdateRelationshipControllerTest extends ControllerBaseTest with ControllerViewTestHelper {

  val generatedNino: Nino = new Generator().nextNino
  val mockTransferService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  val failedFuture: Future[Nothing] = Future.failed(new RuntimeException("test"))


  def createRelationshipRecords(roleType: String = "Transferor", nonPrimaryRecords: Seq[RelationshipRecord] = Seq.empty[RelationshipRecord]) = {

    val citizenName = CitizenName(Some("Test"), Some("User"))
    val loggedInUserInfo = LoggedInUserInfo(cid = 123456789, timestamp = "20181212", has_allowance = None, name = Some(citizenName))
    val primaryRelationshipRecord = RelationshipRecord(participant = roleType, creationTimestamp = "20181212", participant1StartDate = "20181212",
      relationshipEndReason = None, participant1EndDate = None, otherParticipantInstanceIdentifier = "1234567", otherParticipantUpdateTimestamp = "20181212")

    RelationshipRecords(primaryRelationshipRecord, nonPrimaryRecords, loggedInUserInfo)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[CachingService].toInstance(mockCachingService),
      bind[TimeService].toInstance(mockTimeService),
      bind[TemplateRenderer].toInstance(MockTemplateRenderer),
      bind[AuthenticatedActionRefiner].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi())
    ).build()

  lazy val controller = app.injector.instanceOf[UpdateRelationshipController]

  "history" should {

    "display the history summary page with a status of OK" in {

      val relationshipRecords = createRelationshipRecords()
      val historySummaryViewModel = HistorySummaryViewModel(relationshipRecords.primaryRecord.role,
        relationshipRecords.hasMarriageAllowanceBeenCancelled,
        relationshipRecords.loggedInUserInfo)

      when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
        .thenReturn(Future.successful(relationshipRecords))

      when(mockUpdateRelationshipService.saveRelationshipRecords(ArgumentMatchers.eq(relationshipRecords))(any(), any()))
        .thenReturn(Future.successful(relationshipRecords))

      val result = controller.history()(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.history_summary(historySummaryViewModel)
    }
  }
  "History" should {
    "redirect to how-it-works" when {
      "there is no active (primary) record" in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(NoPrimaryRecordError()))

        val result = controller.history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.howItWorks().url)
      }
    }

    "display an error page" when {
      "a TransferorNotFound error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(TransferorNotFound()))

        val result = controller.history()(request)
        status(result) shouldBe OK
        result rendersTheSameViewAs views.html.errors.transferor_not_found()
      }

      "a BadFetchRequest error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(BadFetchRequest()))

        val result = controller.history()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs views.html.errors.try_later()
      }

      "a CitizenNotFound error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(CitizenNotFound()))

        val result = controller.history()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs views.html.errors.citizen_not_found()
      }

      "a MultipleActiveRecordError error is returned " in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(MultipleActiveRecordError()))

        val result = controller.history()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "decision" should {
    "display the decision page with cached data" in {
      val cacheData = Some("checkMarriageAllowanceClaim")
      val validFormWithData = CheckClaimOrCancelDecisionForm.form.fill(cacheData)
      when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(cacheData))

      val result = controller.decision(request)
      status(result) shouldBe OK
      result rendersTheSameViewAs views.html.coc.decision(validFormWithData)

    }

    "display a decision page without cached data" when {
      "there is no data return from the cache" in {

        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(None))
        val validForm = CheckClaimOrCancelDecisionForm.form
        val result = controller.decision(request)
        status(result) shouldBe OK

        result rendersTheSameViewAs views.html.coc.decision(validForm)
      }

      "a non fatal error has occurred when trying to get cached data" in {

        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any()))
          .thenReturn(failedFuture)
        val result = controller.decision(request)
        val validForm = CheckClaimOrCancelDecisionForm.form
        status(result) shouldBe OK

        result rendersTheSameViewAs views.html.coc.decision(validForm)
      }
    }
  }

  "submitDecision" should {
    "redirect to the claims page" when {
      "a user selects the checkMarriageAllowanceClaim option" in {

        val userAnswer = CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim
        val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> userAnswer)

        when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
          .thenReturn(Future.successful(userAnswer))

        val result = controller.submitDecision(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.claims().url)
      }

    }
  }

  "submitDecision" should {
    "redirect to the make change page" when {
      "a user selects the stopMarriageAllowance option" in {

        val userAnswer = CheckClaimOrCancelDecisionForm.StopMarriageAllowance

        val request = FakeRequest().withFormUrlEncodedBody(
          CheckClaimOrCancelDecisionForm.DecisionChoice -> userAnswer
        )

        when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
          .thenReturn(Future.successful(CheckClaimOrCancelDecisionForm.StopMarriageAllowance))

        val result = controller.submitDecision(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.makeChange().url)
      }
    }

    "return a bad request" when {
      "the form submission has a blank value" in {
        val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> "")
        val result = controller.submitDecision(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

  }

  "claims" should {
    "display the claims page" in {

      val relationshipRecords = createRelationshipRecords()

      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.successful(relationshipRecords))

      val claimsViewModel = ClaimsViewModel(relationshipRecords.primaryRecord, relationshipRecords.nonPrimaryRecords)
      val result = controller.claims(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.claims(claimsViewModel)
    }

    "display an error page" when {
      "there is no cached data found" in {
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.failed(CacheMissingRelationshipRecords()))

        val result = controller.claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }

  }

  "makeChange" should {
    "display the make change page" when {
      "there is valid data in the cache" in {
        val userAnswer = "Divorce"
        when(mockUpdateRelationshipService.getMakeChangesDecision(any(),any())).thenReturn(Future.successful(Some(userAnswer)))

        val result = controller.makeChange()(request)
        status(result) shouldBe OK
        result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form.fill(Some(userAnswer)))
      }

      "there is no data in the cache" in {
        when(mockUpdateRelationshipService.getMakeChangesDecision(any(),any())).thenReturn(Future.successful(None))

        val result = controller.makeChange()(request)
        status(result) shouldBe OK
        result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form)
      }

      "a non fatal error has occurred when trying to get cached data" in {
        when(mockUpdateRelationshipService.getMakeChangesDecision(any(),any())).thenReturn(failedFuture)

        val result = controller.makeChange()(request)
        status(result) shouldBe OK
        result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form)
      }
    }
  }

  "submitMakeChange" should {
    "redirect to the divorce enter year page" when {
      "a user selects the Divorce option" in {

        val userAnswer = MakeChangesDecisionForm.Divorce
        val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)

        when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
          .thenReturn(Future.successful("Divorce"))

        val result = controller.submitMakeChange()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.divorceEnterYear().url)

      }

      "redirect to the stop allowance page" when {

        val relationshipRecords = createRelationshipRecords("Recipient")
        val userAnswers = List("Earnings", "Cancel")

        userAnswers.foreach { userAnswer =>

          s"a recipient selects $userAnswer" in {

            val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)

            when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
              .thenReturn(Future.successful(userAnswer))

            when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
              .thenReturn(Future.successful(relationshipRecords))

            val result = controller.submitMakeChange()(request)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.stopAllowance().url)

          }

        }
      }

      "redirect to the change of income page" when {
        "a transferor selects Household Income changes" in {

          val userAnswer = "Earnings"
          val relationshipRecords = createRelationshipRecords()
          val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)

          when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
            .thenReturn(Future.successful(userAnswer))

          when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
            .thenReturn(Future.successful(relationshipRecords))

          val result = controller.submitMakeChange()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.changeOfIncome().url)

        }
      }


      "redirect to the cancel page" when {
        "a transferor selects Do not want Marriage Allowance anymore" in {

          val userAnswer = "Cancel"
          val relationshipRecords = createRelationshipRecords()
          val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)

          when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
            .thenReturn(Future.successful(userAnswer))

          when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
            .thenReturn(Future.successful(relationshipRecords))

          val result = controller.submitMakeChange()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.cancel().url)

        }
      }


      "redirect to the bereavement page" when {
        "a user selects the bereavement option" in {

          val userAnswer = "Bereavement"
          val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> userAnswer)

          when(mockUpdateRelationshipService.saveMakeChangeDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
            .thenReturn(Future.successful(userAnswer))

          val result = controller.submitMakeChange()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.bereavement().url)

        }
      }

      "return a bad request" when {
        "the form submission has a blank value" in {
          val request = buildFakePostRequest(MakeChangesDecisionForm.StopMAChoice -> "")
          val result = controller.submitDecision(request)
          status(result) shouldBe BAD_REQUEST
        }
      }

    }
  }

  "stopAllowance" should {
    "display the stop allowance page" in {
      val result = controller.stopAllowance(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.stopAllowance()
    }
  }

  "cancel" should {
    "display the cancel page" in {

      val nowDate = LocalDate.now
      val marriageAllowanceEndingDates = MarriageAllowanceEndingDates(nowDate, nowDate)

      when(mockUpdateRelationshipService.getMAEndingDatesForCancelation).thenReturn(Future.successful(marriageAllowanceEndingDates))
      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(ArgumentMatchers.eq(marriageAllowanceEndingDates))(any(), any())).
        thenReturn(Future.successful(marriageAllowanceEndingDates))

      val result = controller.cancel(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.cancel(marriageAllowanceEndingDates)
    }

    "display an error page" when {
      "there are issues saving data to the cache" in {
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(failedFuture)

        val result = controller.claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "changeOfIncome" should {
    "display the changeOfIncome page" in {
      val result  = controller.changeOfIncome(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.change_in_earnings()
    }
  }

  "bereavement" should {
    "display the bereavement page" when {
      "there is data returned from the cache" in {

        val relationshipRecords = createRelationshipRecords()
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.successful(relationshipRecords))
        val result = controller.bereavement(request)

        status(result) shouldBe OK
        result rendersTheSameViewAs views.html.coc.bereavement(relationshipRecords.primaryRecord.role)

      }

      "display an error page" when {
        "there is no cached data found" in {
          when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.failed(CacheMissingRelationshipRecords()))

          val result = controller.bereavement(request)
          status(result) shouldBe INTERNAL_SERVER_ERROR

          result rendersTheSameViewAs views.html.errors.try_later()
        }
      }
    }
  }


  "divorceEnterYear" should {
    "display the enter a divorce year page" when {
      "there is data in the cache" in {

        val divorceDateInThePast = LocalDate.now().minusDays(1)
        when(mockUpdateRelationshipService.getDivorceDate(any(), any())).thenReturn(Future.successful(Some(divorceDateInThePast)))

        val result = controller.divorceEnterYear(request)
        status(result) shouldBe OK

        result rendersTheSameViewAs views.html.coc.divorce_select_year(DivorceSelectYearForm.form.fill(divorceDateInThePast))
      }

      "there is no data in the cache" in {
        when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller.divorceEnterYear(request)
        status(result) shouldBe OK

        result rendersTheSameViewAs views.html.coc.divorce_select_year(DivorceSelectYearForm.form)
      }

    }

    "a non fatal error has occurred when trying to get cached data" in {
      when(mockUpdateRelationshipService.getDivorceDate(any(), any())).thenReturn(failedFuture)

      val result = controller.makeChange()(request)
      status(result) shouldBe OK
      result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form)
    }
  }

  "submitDivorceEnterYear" should {
    "redirect to the divorce end explanation page" when {
      "the user enters a valid divorce date in the past" in {

        val divorceDateInThePast = LocalDate.now().minusDays(1)
        val request = buildFakePostRequest("dateOfDivorce.year" -> divorceDateInThePast.getYear.toString,
                                           "dateOfDivorce.month" -> divorceDateInThePast.getMonthValue.toString,
                                           "dateOfDivorce.day" -> divorceDateInThePast.getDayOfMonth.toString)

        when(mockUpdateRelationshipService.saveDivorceDate(ArgumentMatchers.eq(divorceDateInThePast))(any(), any()))
          .thenReturn(Future.successful(divorceDateInThePast))

        val result = controller.submitDivorceEnterYear()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.divorceEndExplanation().url)

      }

    }

    "return a bad request" when {
      "an invalid date is submitted" in {

        val invalidRequest = FakeRequest().withFormUrlEncodedBody(
          "dateOfDivorce.year" -> "year",
          "dateOfDivorce.month" -> "month",
          "dateOfDivorce.day" -> "day"
        )

        val result = controller.submitDivorceEnterYear(invalidRequest)
        status(result) shouldBe BAD_REQUEST

      }
    }

    "display an error page" when {
      "there is an issue saving to the cache" in {
        when(mockUpdateRelationshipService.saveDivorceDate(any())(any(), any())).thenReturn(failedFuture)

        val divorceDateInThePast = LocalDate.now().minusDays(1)

        val request = buildFakePostRequest("dateOfDivorce.year" -> divorceDateInThePast.getYear.toString,
          "dateOfDivorce.month" -> divorceDateInThePast.getMonthValue.toString,
          "dateOfDivorce.day" -> divorceDateInThePast.getDayOfMonth.toString)

        val result = controller.submitDivorceEnterYear(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "divorceEndExplanation" should {
    "display the divorceEndExplanation page" in {

      val role = Transferor
      val now = LocalDate.now()
      val divorceDate = now.minusDays(1)
      val maEndingDate = now.plusDays(1)
      val paEffectiveDate = now.plusDays(2)

      val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((role, divorceDate)))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate))
        .thenReturn(Future.successful(maEndingDates))

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(ArgumentMatchers.eq(maEndingDates))(any(), any()))
        .thenReturn(Future.successful(maEndingDates))

      val viewModel = DivorceEndExplanationViewModel(role, divorceDate, maEndingDates)

      val result = controller.divorceEndExplanation()(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.divorce_end_explanation(viewModel)

    }

    "display an error page" when {

      "an error occurs retrieving divorce data" in {

        when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
          .thenReturn(Future.failed(CacheMissingRelationshipRecords()))

        val result = controller.divorceEndExplanation()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }

      "an error has occurred whilst saving cache data" in {

        val role = Transferor
        val divorceDate = LocalDate.now().minusDays(1)
        val maEndingDate = LocalDate.now().plusDays(1)
        val paEffectiveDate = LocalDate.now().plusDays(2)

        val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

        when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
          .thenReturn(Future.successful((role, divorceDate)))

        when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate))
          .thenReturn(Future.successful(maEndingDates))

        when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(ArgumentMatchers.eq(maEndingDates))(any(), any()))
          .thenReturn(failedFuture)

        val result = controller.divorceEndExplanation()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }

  }

  "confirmEmail" should {
    "display the confirm email page" when {
      "an email is recovered from the cache" in {
        val email = EmailAddress("test@test.com")

        when(mockUpdateRelationshipService.getEmailAddress(any(), any()))
          .thenReturn(Future.successful(Some(email)))

        val result = controller.confirmEmail(request)
        status(result) shouldBe OK

        val populatedForm = emailForm.fill(EmailAddress(email))

        result rendersTheSameViewAs views.html.coc.email(populatedForm)

      }

      "no email is recovered from the cache" in {
        when(mockUpdateRelationshipService.getEmailAddress(any(), any())).thenReturn(Future.successful(None))

        val result = controller.confirmEmail(request)
        status(result) shouldBe OK

        result rendersTheSameViewAs views.html.coc.email(emailForm)
      }

      "fail to get data from cache" in {
        when(mockUpdateRelationshipService.getEmailAddress(any(), any())).thenReturn(failedFuture)

        val result = controller.confirmEmail(request)
        status(result) shouldBe OK

        result rendersTheSameViewAs views.html.coc.email(emailForm)

      }
    }
  }

  "confirmYourEmailActionUpdate" should {
    "redirect to the confirmUpdate page" in {

      val emailAddress = EmailAddress("example@example.com")
      when(mockUpdateRelationshipService.saveEmailAddress(ArgumentMatchers.eq(emailAddress))(any(), any())).
        thenReturn(Future.successful(emailAddress))

      val request = buildFakePostRequest("transferor-email" -> emailAddress)
      val result = controller.confirmYourEmailActionUpdate()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmUpdate().url)

    }

    "return a bad request" when {
      "a form error has occurred" in {

        val request = buildFakePostRequest("transferor-email" -> "")
        val result = controller.confirmYourEmailActionUpdate()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.saveEmailAddress(any())(any(), any())).thenReturn(failedFuture)

        val result = controller.claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }


  "confirmUpdate" should {
    "display the confirmUpdate page" in {

      val loggedInUser = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some("first"), Some("surname"))))
      val divorceDate = LocalDate.now().minusDays(1)
      val emailAddress = "email@email.com"
      val maEndingDate = LocalDate.now().plusDays(1)
      val paEffectiveDate = LocalDate.now().plusDays(2)

      val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

      val confirmUpdateAnswers = ConfirmationUpdateAnswers(loggedInUser, Some(divorceDate), emailAddress, maEndingDates)

      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
          .thenReturn(Future.successful(confirmUpdateAnswers))

      val result = controller.confirmUpdate()(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.confirmUpdate(ConfirmUpdateViewModel(confirmUpdateAnswers))

    }

    "return an InternalServerError" when {

      "there is an issue accessing the cache" in {

        when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
          .thenReturn(failedFuture)

        val result = controller.confirmUpdate()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

    }

  }


  "submitConfirmUpdate" should {
    "redirect to the finish update page" in {
      when(mockUpdateRelationshipService.updateRelationship(any())(any(), any(), any()))
        .thenReturn(Future.successful(mock[UpdateRelationshipRequestHolder]))

      val result = controller.submitConfirmUpdate(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.finishUpdate().url)

    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.updateRelationship(any())(any(), any(), any()))
          .thenReturn(failedFuture)

        val result = controller.submitConfirmUpdate(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "finishUpdate" should {
    "return a success" in {

      val email = "email@email.com"

      when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
        .thenReturn(Future.successful(EmailAddress(email)))

      when(mockUpdateRelationshipService.removeCache(any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val result = controller.finishUpdate()(request)
      status(result) shouldBe OK

      result rendersTheSameViewAs views.html.coc.finished(email)

    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
          .thenReturn(Future.failed(CacheMissingEmail()))

        val result = controller.submitConfirmUpdate(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "handleError" should {
    val auhtRequest: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(
      request,
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
        (new BadFetchRequest, "technical.issue.heading"),
        (new Exception, "technical.issue.heading")
      )

      for ((error, message) <- errors) {
        s"a $error has been thrown" in {
          val result = Future.successful(controller.handleError(HeaderCarrier(), auhtRequest)(error))
          status(result) shouldBe INTERNAL_SERVER_ERROR
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("error").text() shouldBe messages(message)
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
          val result = Future.successful(controller.handleError(HeaderCarrier(), auhtRequest)(error))
          status(result) shouldBe OK
          val doc = Jsoup.parse(contentAsString(result))
          doc.getElementById("error").text() shouldBe messages(message)
        }
      }
    }

    "redirect" when {
      "a errors.CacheRelationshipAlreadyUpdated exception has been thrown" in {
        val result = Future.successful(controller.handleError(HeaderCarrier(), auhtRequest)(new CacheRelationshipAlreadyUpdated))
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.finishUpdate().url)
      }
    }
  }
}
