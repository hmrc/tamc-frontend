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

import com.google.inject.Inject
import controllers.actions.AuthenticatedActionRefiner
import errors._
import forms.EmailForm.emailForm
import forms.coc.{CheckClaimOrCancelDecisionForm, DivorceSelectYearForm, MakeChangesDecisionForm}
import models._
import models.auth.BaseUserRequest
import play.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import viewModels._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import views.html.coc.{bereavement, cancel, change_in_earnings, claims, confirmUpdate, decision, divorce_end_explanation, divorce_select_year, email, finished, history_summary, reason_for_change, stopAllowance}
import views.html.errors.{citizen_not_found, recipient_not_found, transferor_not_found, try_later}

class UpdateRelationshipController @Inject()(
                                              authenticate: AuthenticatedActionRefiner,
                                              updateRelationshipService: UpdateRelationshipService,
                                              cc: MessagesControllerComponents,
                                              bereavementView: bereavement,
                                              historySummary: history_summary,
                                              decision: decision,
                                              claimsView: claims,
                                              reasonForChange: reason_for_change,
                                              stopAllowanceView: stopAllowance,
                                              changeInEarningsView: change_in_earnings,
                                              divorceSelectYearView: divorce_select_year,
                                              divorceEndExplanationView: divorce_end_explanation,
                                              emailView: email,
                                              confirmUpdateView: confirmUpdate,
                                              finishedView: finished,
                                              cancelView: cancel,
                                              tryLaterView: try_later,
                                              citizenNotFoundView: citizen_not_found,
                                              transferorNotFoundView: transferor_not_found,
                                              recipientNotFoundView: recipient_not_found


                                            )(implicit templateRenderer: TemplateRenderer,
                                              formPartialRetriever: FormPartialRetriever,
                                              ec: ExecutionContext) extends BaseController(cc) {

  def history(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.retrieveRelationshipRecords(request.nino) flatMap { relationshipRecords =>
        updateRelationshipService.saveRelationshipRecords(relationshipRecords) map { _ =>
          val viewModel = HistorySummaryViewModel(relationshipRecords.primaryRecord.role,
            relationshipRecords.hasMarriageAllowanceBeenCancelled,
            relationshipRecords.loggedInUserInfo)
          Ok(historySummary(viewModel))
        }
      } recover handleError
  }

  def decision: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getCheckClaimOrCancelDecision map { claimOrCancelDecision =>
        Ok(decision(CheckClaimOrCancelDecisionForm.form.fill(claimOrCancelDecision)))
      } recover {
        case NonFatal(_) => Ok(decision(CheckClaimOrCancelDecisionForm.form))
      }
  }

  def submitDecision: Action[AnyContent] = authenticate.async {
    implicit request =>

      CheckClaimOrCancelDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(decision(formWithErrors)))
        }, {
          case Some(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim) => {
            updateRelationshipService.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.claims())
            }
          }
          case Some(CheckClaimOrCancelDecisionForm.StopMarriageAllowance) => {
            updateRelationshipService.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.StopMarriageAllowance) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.makeChange())
            }
          }
        })
  }

  def claims: Action[AnyContent] = authenticate.async {
    implicit request =>
      (updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
        val viewModel = ClaimsViewModel(relationshipRecords.primaryRecord, relationshipRecords.nonPrimaryRecords)
        Ok(claimsView(viewModel))
      }) recover handleError
  }


  def makeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getMakeChangesDecision map { makeChangesData =>
        Ok(reasonForChange(MakeChangesDecisionForm.form.fill(makeChangesData.map(_.toString))))
      } recover {
        case NonFatal(_) => Ok(reasonForChange(MakeChangesDecisionForm.form))
      }
  }

  def submitMakeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      MakeChangesDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(reasonForChange(formWithErrors)))
        }, {
          case Some(MakeChangesDecisionForm.Divorce) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Divorce) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.divorceEnterYear())
            }
          }
          case Some(MakeChangesDecisionForm.Earnings) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Earnings) flatMap { _ =>
             changeOfIncomeRedirect
            }

          }
          case Some(MakeChangesDecisionForm.Cancel) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Cancel) flatMap { _ =>
              noLongerWantMarriageAllowanceRedirect
            }
          }
          case Some(MakeChangesDecisionForm.Bereavement) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Bereavement) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.bereavement())
            }
          }
        })
  }

  private def noLongerWantMarriageAllowanceRedirect(implicit hc: HeaderCarrier): Future[Result] = {
    updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
      if(relationshipRecords.primaryRecord.role == Recipient){
        Redirect(controllers.routes.UpdateRelationshipController.stopAllowance())
      } else {
        Redirect(controllers.routes.UpdateRelationshipController.cancel())
      }
    }
  }

  private def changeOfIncomeRedirect(implicit hc: HeaderCarrier): Future[Result] = {
    updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
      if(relationshipRecords.primaryRecord.role == Recipient){
        Redirect(controllers.routes.UpdateRelationshipController.stopAllowance())
      } else {
        Redirect(controllers.routes.UpdateRelationshipController.changeOfIncome())
      }
    }
  }

  def stopAllowance: Action[AnyContent] = authenticate.async {
    implicit request =>
      Future.successful(Ok(stopAllowanceView()))
  }


  def cancel: Action[AnyContent] = authenticate.async {
    implicit request =>
      val cancelDates = updateRelationshipService.getMAEndingDatesForCancelation
      updateRelationshipService.saveMarriageAllowanceEndingDates(cancelDates) map { _ =>
        Ok(cancelView(cancelDates))
      } recover handleError
  }

  def changeOfIncome: Action[AnyContent] = authenticate.async {
    implicit request =>
      Future.successful(Ok(changeInEarningsView()))
  }

  def bereavement: Action[AnyContent] = authenticate.async {
    implicit request =>
      (updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
        Ok(bereavementView(relationshipRecords.primaryRecord.role))
      }) recover handleError
  }

  def divorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getDivorceDate map { optionalDivorceDate =>
        optionalDivorceDate.fold(Ok(divorceSelectYearView(DivorceSelectYearForm.form))){ divorceDate =>
          Ok(divorceSelectYearView(DivorceSelectYearForm.form.fill(divorceDate)))
        }
      } recover {
        case NonFatal(_) =>
          Ok(divorceSelectYearView(DivorceSelectYearForm.form))
      }
  }

  def submitDivorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      DivorceSelectYearForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(divorceSelectYearView(formWithErrors)))
        }, {
          case divorceDate =>
            updateRelationshipService.saveDivorceDate(divorceDate) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.divorceEndExplanation())
          }
        }
      ) recover handleError
  }

  def divorceEndExplanation: Action[AnyContent] = authenticate.async {
    implicit request =>
      (for {
        (role, divorceDate) <- updateRelationshipService.getDataForDivorceExplanation
        datesForDivorce = updateRelationshipService.getMAEndingDatesForDivorce(role, divorceDate)
        _ <- updateRelationshipService.saveMarriageAllowanceEndingDates(datesForDivorce)
      } yield {
       val viewModel = DivorceEndExplanationViewModel(role, divorceDate, datesForDivorce)
        Ok(divorceEndExplanationView(viewModel))
      }) recover handleError
  }


  def confirmEmail: Action[AnyContent] = authenticate.async {
    implicit request =>
      lazy val emptyEmailView = emailView(emailForm)
      updateRelationshipService.getEmailAddress map {
        case Some(email) => Ok(emailView(emailForm.fill(email)))
        case None => Ok(emptyEmailView)
      } recover {
        case NonFatal(_) => Ok(emptyEmailView)
      }
  }

  def confirmYourEmailActionUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(emailView(formWithErrors)))
        },
        email =>
          updateRelationshipService.saveEmailAddress(email) map {
            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmUpdate())
          }
      ) recover handleError
  }

  def confirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getConfirmationUpdateAnswers map { confirmationUpdateAnswers =>
          Ok(confirmUpdateView(ConfirmUpdateViewModel(confirmationUpdateAnswers)))
      } recover handleError
  }

  def submitConfirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.updateRelationship(request.nino) map {
        _ => Redirect(controllers.routes.UpdateRelationshipController.finishUpdate())
      } recover handleError
  }

  def finishUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      (for {
        email <- updateRelationshipService.getEmailAddressForConfirmation
        _ <- updateRelationshipService.removeCache
      } yield {
        Ok(finishedView(email))
      }) recover handleError
  }

  def handleError(implicit hc: HeaderCarrier, request: BaseUserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>

        val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

        def handle(logger: (String, Throwable) => Unit, result: Result): Result = {
          logger(message, throwable)
          result
        }

        throwable match {
          case _: NoPrimaryRecordError => Redirect(controllers.routes.EligibilityController.howItWorks())
          case _: CacheRelationshipAlreadyUpdated => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.finishUpdate()))
          case _: CacheMissingUpdateRecord => handle(Logger.warn, InternalServerError(tryLaterView()))
          case _: CacheUpdateRequestNotSent => handle(Logger.warn, InternalServerError(tryLaterView()))
          case _: CannotUpdateRelationship => handle(Logger.warn, InternalServerError(tryLaterView()))
          case _: MultipleActiveRecordError => handle(Logger.warn, InternalServerError(tryLaterView()))
          case _: CitizenNotFound => handle(Logger.warn, InternalServerError(citizenNotFoundView()))
          case _: BadFetchRequest => handle(Logger.warn, InternalServerError(tryLaterView()))
          case _: TransferorNotFound => handle(Logger.warn, Ok(transferorNotFoundView()))
          case _: RecipientNotFound => handle(Logger.warn, Ok(recipientNotFoundView()))
          case _ => handle(Logger.error, InternalServerError(tryLaterView()))
        }
    }

}
