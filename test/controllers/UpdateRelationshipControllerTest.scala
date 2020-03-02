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
import forms.EmailForm.emailForm
import forms.coc.{CheckClaimOrCancelDecisionForm, DivorceSelectYearForm, MakeChangesDecisionForm}
import models.{MarriageAllowanceEndingDates, _}
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{TimeService, _}
import test_utils._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import viewModels._

import scala.concurrent.Future
import scala.language.postfixOps

class UpdateRelationshipControllerTest extends ControllerBaseSpec with ControllerViewTestHelper {

  val generatedNino = new Generator().nextNino
  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockListRelationshipService: ListRelationshipService = mock[ListRelationshipService]
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

  def controller(auth: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner],
                 timeService: TimeService = mockTimeService): UpdateRelationshipController =
    new UpdateRelationshipController(
      messagesApi,
      auth,
      mockUpdateRelationshipService,
      timeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])


  "history" should {

    "display the history summary page with a status of OK" in {

      val relationshipRecords = createRelationshipRecords()

      val historySummaryViewModel = HistorySummaryViewModel(relationshipRecords)

      when(mockUpdateRelationshipService.retrieveRelationshipRecords(ArgumentMatchers.eq(generatedNino))(any(), any()))
        .thenReturn(Future.successful(relationshipRecords))

      when(mockUpdateRelationshipService.saveRelationshipRecords(ArgumentMatchers.eq(relationshipRecords))(any(), any()))
        .thenReturn(Future.successful(relationshipRecords))

      val result = controller().history()(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.history_summary(historySummaryViewModel)

    }

    "redirect to the transfer controller" when {
      "there is no active (primary) record and non permanent authentication" in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(ArgumentMatchers.eq(generatedNino))(any(), any()))
            .thenReturn(Future.failed(throw NoPrimaryRecordError()))

        val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.transfer().url)
      }
    }

    "redirect to how-it-works" when {
      "here is no active (primary) record and permanent authentication" in {
        when(mockUpdateRelationshipService.retrieveRelationshipRecords(any())(any(), any()))
          .thenReturn(Future.failed(throw NoPrimaryRecordError()))

        val result: Future[Result] = controller().history()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.howItWorks().url)
      }
    }

    // TODO write tests with regards to error handling in line with TAMC changes

  }


  "decision" should {
    "display the decision page with cached data" in {
      val cacheData = Some("checkMarriageAllowanceClaim")
      val validFormWithData = CheckClaimOrCancelDecisionForm.form.fill(cacheData)
      when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(cacheData))

      val result = controller().decision(request)
      status(result) shouldBe OK
      //result rendersTheSameViewAs views.html.coc.decision(validFormWithData)

    }

    "display a decision page without cached data" when {
      "there is no data return from the cache" in {

        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any())).thenReturn(Future.successful(None))
        val validForm = CheckClaimOrCancelDecisionForm.form
        val result = controller().decision(request)
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.decision(validForm)
      }

      "a non fatal error has occurred when trying to get cached data" in {

        when(mockUpdateRelationshipService.getCheckClaimOrCancelDecision(any(), any()))
          .thenReturn(failedFuture)
        val result = controller().decision(request)
        val validForm = CheckClaimOrCancelDecisionForm.form
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.decision(validForm)
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

        val result = controller().submitDecision(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.claims().url)
      }

    }

    "redirect to the make change page" when {
      "a user selects the stopMarriageAllowance option" in {

        val userAnswer = CheckClaimOrCancelDecisionForm.StopMarriageAllowance

        val request = FakeRequest().withFormUrlEncodedBody(
          CheckClaimOrCancelDecisionForm.DecisionChoice -> userAnswer
        )

        when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
          .thenReturn(Future.successful(CheckClaimOrCancelDecisionForm.StopMarriageAllowance))

        val result = controller().submitDecision(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.makeChange().url)
      }
    }

    "return a bad request" when {
      "the form submission has a blank value" in {
        val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> "")
        val result = controller().submitDecision(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

  }

  "claims" should {
    "display the claims page" in {

      val relationshipRecords = createRelationshipRecords()

      when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.successful(relationshipRecords))

      val claimsViewModel = ClaimsViewModel(relationshipRecords.primaryRecord, relationshipRecords.nonPrimaryRecords)
      val result = controller().claims(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.claims(claimsViewModel)
    }

    "display an error page" when {
      "there is no cached data found" in {
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.failed(throw CacheMissingRelationshipRecords()))

        val result = controller().claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        //result rendersTheSameViewAs views.html.errors.try_later()
      }
    }

  }

  "makeChange" should {
    "display the make change page" when {
      "there is valid data in the cache" in {

        val userAnswer = Some(EndMarriageAllowanceReason.toCaseObject(MakeChangesDecisionForm.Divorce))
        when(mockUpdateRelationshipService.getMakeChangesDecision(any(),any())).thenReturn(Future.successful(userAnswer))

        val result = controller().makeChange()(request)
        status(result) shouldBe OK
        //result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form.fill(userAnswer.map(_.toString)))
      }

      "there is no data in the cache" in {
        when(mockUpdateRelationshipService.getMakeChangesDecision(any(),any())).thenReturn(Future.successful(None))

        val result = controller().makeChange()(request)
        status(result) shouldBe OK
        //result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form)
      }

      "a non fatal error has occurred when trying to get cached data" in {
        when(mockUpdateRelationshipService.getMakeChangesDecision(any(),any())).thenReturn(failedFuture)

        val result = controller().makeChange()(request)
        status(result) shouldBe OK
        //result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form)
      }
    }
  }

  "submitMakeChange" should {
    "redirect to the divorce enter year page" when {
      "a user selects the Divorce option" in {

        val userAnswer = MakeChangesDecisionForm.Divorce

        val request = FakeRequest().withFormUrlEncodedBody(
          MakeChangesDecisionForm.StopMAChoice -> userAnswer
        )

        when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
          .thenReturn(Future.successful(userAnswer))

        val result = controller().submitMakeChange()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.divorceEnterYear().url)

      }

      "redirect to the stop allowance page" when {

        val relationshipRecords = createRelationshipRecords("Recipient")
        val userAnswers = List(MakeChangesDecisionForm.Earnings, MakeChangesDecisionForm.Cancel)

        userAnswers.foreach { userAnswer =>

          s"a recipient selects $userAnswer" in {

            val request = FakeRequest().withFormUrlEncodedBody(
              MakeChangesDecisionForm.StopMAChoice -> userAnswer
            )

            when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
              .thenReturn(Future.successful(userAnswer))

            when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
              .thenReturn(Future.successful(relationshipRecords))

            val result = controller().submitMakeChange()(request)
            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.stopAllowance())

          }

        }
      }

      "redirect to the change of income page" when {
        "a transferor selects Household Income changes" in {

          val userAnswer = MakeChangesDecisionForm.Earnings
          val relationshipRecords = createRelationshipRecords()

          val request = FakeRequest().withFormUrlEncodedBody(
            MakeChangesDecisionForm.StopMAChoice -> userAnswer
          )

          when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
            .thenReturn(Future.successful(userAnswer))

          when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
            .thenReturn(Future.successful(relationshipRecords))

          val result = controller().submitMakeChange()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.changeOfIncome())

        }
      }


      "redirect to the cancel page" when {
        "a transferor selects Do not want Marriage Allowance anymore" in {

          val userAnswer = MakeChangesDecisionForm.Cancel
          val relationshipRecords = createRelationshipRecords()

          val request = FakeRequest().withFormUrlEncodedBody(
            MakeChangesDecisionForm.StopMAChoice -> userAnswer
          )

          when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
            .thenReturn(Future.successful(userAnswer))

          when(mockUpdateRelationshipService.getRelationshipRecords(any(), any()))
            .thenReturn(Future.successful(relationshipRecords))

          val result = controller().submitMakeChange()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.cancel())

        }
      }


      "redirect to the bereavement page" when {
        "a user selects the bereavement option" in {

          val userAnswer = MakeChangesDecisionForm.Bereavement

          val request = FakeRequest().withFormUrlEncodedBody(
            MakeChangesDecisionForm.StopMAChoice -> userAnswer
          )

          when(mockUpdateRelationshipService.saveCheckClaimOrCancelDecision(ArgumentMatchers.eq(userAnswer))(any(), any()))
            .thenReturn(Future.successful(userAnswer))

          val result = controller().submitMakeChange()(request)
          status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.bereavement())

        }
      }

      "return a bad request" when {
        "the form submission has a blank value" in {
          val request = FakeRequest().withFormUrlEncodedBody(CheckClaimOrCancelDecisionForm.DecisionChoice -> "")
          val result = controller().submitDecision(request)
          status(result) shouldBe BAD_REQUEST
        }
      }

    }
  }

  "stopAllowance" should {
    "display the stop allowance page" in {
      val result = controller().stopAllowance(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.stopAllowance()
    }
  }

  "cancel" should {
    "display the cancel page" in {

      val nowDate = new LocalDate()
      val marriageAllowanceEndingDates = MarriageAllowanceEndingDates(nowDate, nowDate)

      when(mockUpdateRelationshipService.getMAEndingDatesForCancelation).thenReturn(Future.successful(marriageAllowanceEndingDates))

      val result = controller().cancel(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.cancel(marriageAllowanceEndingDates)
    }

    "display an error page" when {
      "there are issues saving data to the cache" in {
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(failedFuture)

        val result = controller().claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        //result rendersTheSameViewAs views.html.errors.try_later()
      }
    }

  }

  "changeOfIncome" should {
    "display the changeOfIncome page" in {
      val result  = controller().changeOfIncome(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.change_in_earnings()
    }
  }

  "bereavement" should {
    "display the bereavement page" when {
      "there is data returned from the cache" in {

        val relationshipRecords = createRelationshipRecords()
        when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.successful(relationshipRecords))
        val result = controller().bereavement(request)

        status(result) shouldBe OK
        //result rendersTheSameViewAs views.html.coc.bereavement(relationshipRecords.primaryRecord.role)

      }

      "display an error page" when {
        "there is no cached data found" in {
          when(mockUpdateRelationshipService.getRelationshipRecords(any(), any())).thenReturn(Future.failed(throw CacheMissingRelationshipRecords()))

          val result = controller().bereavement(request)
          status(result) shouldBe INTERNAL_SERVER_ERROR

          //result rendersTheSameViewAs views.html.errors.try_later()
        }
      }
    }
  }


  "divorceEnterYear" should {
    "display the enter a divorce year page" when {
      "there is data in the cache" in {

        val divorceDateInThePast = LocalDate.now().minusDays(1)
        when(mockUpdateRelationshipService.getDivorceDate(any(), any())).thenReturn(Future.successful(Some(divorceDateInThePast)))

        val result = controller().divorceEnterYear(request)
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.divorce_select_year(DivorceSelectYearForm.form.fill(divorceDateInThePast))
      }

      "there is no data in the cache" in {
        when(mockUpdateRelationshipService.getDivorceDate(any(), any()))
          .thenReturn(Future.successful(None))

        val result = controller().divorceEnterYear(request)
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.divorce_select_year(DivorceSelectYearForm.form)
      }

    }

    "a non fatal error has occurred when trying to get cached data" in {
      when(mockUpdateRelationshipService.getDivorceDate(any(), any())).thenReturn(failedFuture)

      val result = controller().makeChange()(request)
      status(result) shouldBe OK
      //result rendersTheSameViewAs views.html.coc.reason_for_change(MakeChangesDecisionForm.form)
    }
  }

  "submitDivorceEnterYear" should {
    "redirect to the divorce end explanation page" when {
      "the user enters a valid divorce date in the past" in {

        val divorceDateInThePast = LocalDate.now().minusDays(1)

        val request = FakeRequest().withFormUrlEncodedBody(
          "dateOfDivorce.day" -> divorceDateInThePast.dayOfMonth().toString,
          "dateOfDivorce.month" -> divorceDateInThePast.monthOfYear().toString,
          "dateOfDivorce.year" -> divorceDateInThePast.year().toString
        )

        when(mockUpdateRelationshipService.saveDivorceDate(any())(any(), any()))
          .thenReturn(Future.successful(divorceDateInThePast))

        val result = controller().submitDivorceEnterYear()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.divorceEndExplanation().url)

      }

    }

    "return a bad request" when {
      "an invalid date is submitted" in {

        val invalidRequest = FakeRequest().withFormUrlEncodedBody(
          "dateOfDivorce.day" -> "day",
          "dateOfDivorce.month" -> "month",
          "dateOfDivorce.year" -> "year"
        )

        val result = controller().submitDivorceEnterYear(invalidRequest)
        status(result) shouldBe BAD_REQUEST

      }
    }

    "display an error page" when {
      "there is an issue saving to the cache" in {
        when(mockUpdateRelationshipService.saveDivorceDate(any())(any(), any())).thenReturn(failedFuture)

        val divorceDateInThePast = LocalDate.now().minusDays(1)

        val request = FakeRequest().withFormUrlEncodedBody(
          "dateOfDivorce.day" -> divorceDateInThePast.dayOfMonth().toString,
          "dateOfDivorce.month" -> divorceDateInThePast.monthOfYear().toString,
          "dateOfDivorce.year" -> divorceDateInThePast.year().toString
        )

        val result = controller().submitDivorceEnterYear(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
        //result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "divorceEndExplanation" should {
    "display the divorceEndExplanation page" in {

      val role = Transferor
      val divorceDate = LocalDate.now().minusDays(1)
      val maEndingDate = LocalDate.now().plusDays(1)
      val paEffectiveDate = LocalDate.now().plusDays(2)

      val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
        .thenReturn(Future.successful((role, divorceDate)))

      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate))
        .thenReturn(Future.successful(maEndingDates))

      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(maEndingDates))
        .thenReturn(Future.successful(maEndingDates))

      val viewModel = DivorceEndExplanationViewModel(role, divorceDate, maEndingDates)

      val result = controller().divorceEndExplanation()(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.divorce_end_explanation(viewModel)

    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any())).thenReturn(failedFuture)

        val result = controller().claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        //result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "confirmEmail" should {
    "display the confirm email page" when {
      "an email is recovered from the cache" in {
        val email = "test@test.com"

        when(mockUpdateRelationshipService.getEmailAddress(any(), any()))
          .thenReturn(Future.successful(Some(email)))

        val viewModel = EmailViewModel("backlink")
        val result = controller().confirmEmail(request)
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.email(emailForm.fill(EmailAddress(email)), viewModel)

      }

      "no email is recovered from the cache" in {
        when(mockUpdateRelationshipService.getEmailAddress(any(), any())).thenReturn(Future.successful(None))

        val viewModel = EmailViewModel("backlink")
        val result = controller().confirmEmail(request)
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.email(emailForm, viewModel)
      }

      "fail to get data from cache" in {
        when(mockUpdateRelationshipService.getEmailAddress(any(), any())).thenReturn(failedFuture)

        val viewModel = EmailViewModel("backlink")
        val result = controller().confirmEmail(request)
        status(result) shouldBe OK

        //result rendersTheSameViewAs views.html.coc.email(emailForm, viewModel)

      }
    }
  }

  "confirmYourEmailActionUpdate" should {
    "redirect to the confirmUpdate page" in {

      val emailAddress = "example@example.com"
      when(mockUpdateRelationshipService.saveEmailAddress(emailAddress)(any(), any())).thenReturn(Future.successful(emailAddress))

      val request = FakeRequest().withFormUrlEncodedBody("transferor-email" -> emailAddress)
      val result = controller().confirmYourEmailActionUpdate()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmUpdate().url)

    }

    "return a bad request" when {
      "a form error has occurred" in {
        val request = FakeRequest().withFormUrlEncodedBody("transferor-email" -> "")
        val result = controller().confirmYourEmailActionUpdate()(request)
        status(result) shouldBe BAD_REQUEST
      }
    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.saveEmailAddress(any())(any(), any())).thenReturn(failedFuture)

        val result = controller().claims(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        //result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }


  "confirmUpdate" should {
    "display the confirmUpdate page" in {

      val fullName = "testName"
      val divorceDate = LocalDate.now().minusDays(1)
      val emailAddress = "email@email.com"
      val maEndingDate = LocalDate.now().plusDays(1)
      val paEffectiveDate = LocalDate.now().plusDays(2)

      val maEndingDates = MarriageAllowanceEndingDates(maEndingDate, paEffectiveDate)

      val confirmUpdateAnswers = ConfirmationUpdateAnswers(fullName, Some(divorceDate), emailAddress, maEndingDates)

      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
          .thenReturn(Future.successful((confirmUpdateAnswers)))

      val result = controller().confirmUpdate()(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.confirmUpdate(ConfirmUpdateViewModel(confirmUpdateAnswers))

    }

    "return InternalServerError" when {
      "there is no cache data returned" in {

        when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
          .thenReturn(Future.successful((failedFuture)))

        val result = controller().confirmUpdate()(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }


  "submitConfirmUpdate" should {
    "redirect to the finish update page" in {
      when(mockUpdateRelationshipService.updateRelationship(generatedNino)(any(), any(), any()))
        .thenReturn(Future.successful())

      val result = controller().submitConfirmUpdate(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe controllers.routes.UpdateRelationshipController.finishUpdate().url

    }

    //TODO other errors

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.updateRelationship(generatedNino)(any(), any(), any()))
          .thenReturn(failedFuture)

        val result = controller().submitConfirmUpdate(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

  "finishUpdate" should {
    "return a success" in {

      val email = "email@email.com"

      when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
        .thenReturn(Future.successful(email))

      when(mockUpdateRelationshipService.removeCache(any(), any()))
        .thenReturn(Future.successful())

      val result = controller().finishUpdate()(request)
      status(result) shouldBe OK

      //result rendersTheSameViewAs views.html.coc.finished(EmailAddress(email))

    }

    "display an error page" when {
      "an error has occurred whilst accessing the cache" in {
        when(mockUpdateRelationshipService.getEmailAddressForConfirmation(any(), any()))
          .thenReturn(failedFuture)

        val result = controller().submitConfirmUpdate(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR

        //result rendersTheSameViewAs views.html.errors.try_later()
      }
    }
  }

//  "handleError" should {
//    val auhtRequest: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(
//      request,
//      PermanentlyAuthenticated,
//      Some(ConfidenceLevel.L200),
//      isSA = false,
//      Some("GovernmentGateway"),
//      Nino(TestData.Ninos.nino1)
//    )
//
//    "return internal server error" when {
//      val errors = List(
//        (new CacheMissingUpdateRecord, "technical.issue.heading"),
//        (new CacheUpdateRequestNotSent, "technical.issue.heading"),
//        (new CannotUpdateRelationship, "technical.issue.heading"),
//        (new CitizenNotFound, "technical.cannot-find-details.para1"),
//        (new BadFetchRequest, "technical.technical-error.para1"),
//        (new Exception, "technical.issue.heading")
//      )
//
//      for ((error, message) <- errors) {
//        s"a $error has been thrown" in {
//          val result = Future.successful(controller().handleError(HeaderCarrier(), auhtRequest)(error))
//          status(result) shouldBe INTERNAL_SERVER_ERROR
//          val doc = Jsoup.parse(contentAsString(result))
//          doc.getElementById("error").text() shouldBe messagesApi(message)
//        }
//      }
//    }
//
//    "return OK" when {
//      val errors = List(
//        (new TransferorNotFound, "transferor.not.found"),
//        (new RecipientNotFound, "recipient.not.found.para1")
//      )
//
//      for ((error, message) <- errors) {
//        s"a $error has been thrown" in {
//          val result = Future.successful(controller().handleError(HeaderCarrier(), auhtRequest)(error))
//          status(result) shouldBe OK
//          val doc = Jsoup.parse(contentAsString(result))
//          doc.getElementById("error").text() shouldBe messagesApi(message)
//        }
//      }
//    }
//
//    "redirect" when {
//      "a errors.CacheRelationshipAlreadyUpdated exception has been thrown" in {
//        val result = Future.successful(controller().handleError(HeaderCarrier(), auhtRequest)(new CacheRelationshipAlreadyUpdated))
//        status(result) shouldBe SEE_OTHER
//        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.finishUpdate().url)
//      }
//    }
//  }
}
