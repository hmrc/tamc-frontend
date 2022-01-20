/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import utils.LoggerHelper
import viewModels._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class   UpdateRelationshipController @Inject()(
  authenticate: AuthenticatedActionRefiner,
  updateRelationshipService: UpdateRelationshipService,
  cc: MessagesControllerComponents,
  historySummary: views.html.coc.history_summary,
  decisionV: views.html.coc.decision,
  claimsV: views.html.coc.claims,
  reasonForChange: views.html.coc.reason_for_change,
  stopAllowanceV: views.html.coc.stopAllowance,
  cancelV: views.html.coc.cancel,
  confirmUpdateV: views.html.coc.confirmUpdate,
  bereavementV: views.html.coc.bereavement,
  divorceSelectYearV: views.html.coc.divorce_select_year,
  divorceEndExplanationV: views.html.coc.divorce_end_explanation,
  emailV: views.html.coc.email,
  finished: views.html.coc.finished,
  tryLater: views.html.errors.try_later,
  citizenNotFound: views.html.errors.citizen_not_found,
  transferorNotFound: views.html.errors.transferor_not_found,
  recipientNotFound: views.html.errors.recipient_not_found,
  divorceSelectYearForm: DivorceSelectYearForm,
  historySummaryViewModelImpl: HistorySummaryViewModelImpl,
  claimsViewModelImpl: ClaimsViewModelImpl,
  divorceEndExplanationViewModelImpl: DivorceEndExplanationViewModelImpl,
  confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl)(implicit formPartialRetriever: FormPartialRetriever, ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def history(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.retrieveRelationshipRecords(request.nino) flatMap { relationshipRecords =>
        updateRelationshipService.saveRelationshipRecords(relationshipRecords) map { _ =>
          val viewModel = historySummaryViewModelImpl(relationshipRecords.primaryRecord.role,
            relationshipRecords.hasMarriageAllowanceBeenCancelled,
            relationshipRecords.loggedInUserInfo)
          Ok(historySummary(viewModel))
        }
      } recover handleError
  }

  def decision: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getCheckClaimOrCancelDecision map { claimOrCancelDecision =>
        Ok(decisionV(CheckClaimOrCancelDecisionForm.form.fill(claimOrCancelDecision)))
      } recover {
        case NonFatal(_) => Ok(decisionV(CheckClaimOrCancelDecisionForm.form))
      }
  }
//TODO - Refactor to address warning
  def submitDecision: Action[AnyContent] = authenticate.async {
    implicit request =>

      CheckClaimOrCancelDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(decisionV(formWithErrors)))
        }, {
          case Some(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim) =>
            updateRelationshipService.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.CheckMarriageAllowanceClaim) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.claims)
            }

          case Some(CheckClaimOrCancelDecisionForm.StopMarriageAllowance) =>
            updateRelationshipService.saveCheckClaimOrCancelDecision(CheckClaimOrCancelDecisionForm.StopMarriageAllowance) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.makeChange)
            }
        })
  }

  def claims: Action[AnyContent] = authenticate.async {
    implicit request =>
      (updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
        val viewModel = claimsViewModelImpl(relationshipRecords.primaryRecord, relationshipRecords.nonPrimaryRecords)
        Ok(claimsV(viewModel))
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
  //TODO - Refactor to address warning
  def submitMakeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      MakeChangesDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(reasonForChange(formWithErrors)))
        }, {
          case Some(MakeChangesDecisionForm.Divorce) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Divorce) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.divorceEnterYear)
            }
          }
          case Some(MakeChangesDecisionForm.Cancel) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Cancel) flatMap { _ =>
              noLongerWantMarriageAllowanceRedirect
            }
          }
          case Some(MakeChangesDecisionForm.Bereavement) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Bereavement) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.bereavement)
            }
          }
        })
  }

  private def noLongerWantMarriageAllowanceRedirect(implicit hc: HeaderCarrier): Future[Result] = {
    updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
      if(relationshipRecords.primaryRecord.role == Recipient){
        Redirect(controllers.routes.UpdateRelationshipController.stopAllowance)
      } else {
        Redirect(controllers.routes.UpdateRelationshipController.cancel)
      }
    }
  }


  def stopAllowance: Action[AnyContent] = authenticate.async {
    implicit request =>
      Future.successful(Ok(stopAllowanceV()))
  }


  def cancel: Action[AnyContent] = authenticate.async {
    implicit request =>
      val cancelDates = updateRelationshipService.getMAEndingDatesForCancelation
      updateRelationshipService.saveMarriageAllowanceEndingDates(cancelDates) map { _ =>
        Ok(cancelV(cancelDates))
      } recover handleError
  }


  def bereavement: Action[AnyContent] = authenticate.async {
    implicit request =>
      (updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
        Ok(bereavementV(relationshipRecords.primaryRecord.role))
      }) recover handleError
  }

  def divorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getDivorceDate map { optionalDivorceDate =>
        optionalDivorceDate.fold(Ok(divorceSelectYearV(divorceSelectYearForm.form))){ divorceDate =>
          Ok(divorceSelectYearV(divorceSelectYearForm.form.fill(divorceDate)))
        }
      } recover {
        case NonFatal(_) =>
          Ok(divorceSelectYearV(divorceSelectYearForm.form))
      }
  }

  def submitDivorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      divorceSelectYearForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(divorceSelectYearV(formWithErrors)))
        }, {
          case divorceDate =>
            updateRelationshipService.saveDivorceDate(divorceDate) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.divorceEndExplanation)
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
       val viewModel = divorceEndExplanationViewModelImpl(role, divorceDate, datesForDivorce)
        Ok(divorceEndExplanationV(viewModel))
      }) recover handleError
  }


  def confirmEmail: Action[AnyContent] = authenticate.async {
    implicit request =>
      lazy val emptyEmailView = emailV(emailForm)
      updateRelationshipService.getEmailAddress map {
        case Some(email) => Ok(emailV(emailForm.fill(email)))
        case None => Ok(emptyEmailView)
      } recover {
        case NonFatal(_) => Ok(emptyEmailView)
      }
  }

  def confirmYourEmailActionUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(emailV(formWithErrors)))
        },
        email =>
          updateRelationshipService.saveEmailAddress(email) map {
            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmUpdate)
          }
      ) recover handleError
  }

  def confirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getConfirmationUpdateAnswers map { confirmationUpdateAnswers =>
          Ok(confirmUpdateV(confirmUpdateViewModelImpl(confirmationUpdateAnswers)))
      } recover handleError
  }

  def submitConfirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.updateRelationship(request.nino) map {
        _ => Redirect(controllers.routes.UpdateRelationshipController.finishUpdate)
      } recover handleError
  }

  def finishUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      (for {
        email <- updateRelationshipService.getEmailAddressForConfirmation
        _ <- updateRelationshipService.removeCache
      } yield {
        Ok(finished(email))
      }) recover handleError
  }

  def handleError(implicit request: BaseUserRequest[_]): PartialFunction[Throwable, Result] = {
    def message(throwable: Throwable): String =
      s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

        def handle(throwable: Throwable, logger: (String, Throwable) => Unit, result: Result): Result = {
          logger(message(throwable), throwable)
          result
        }

    val pf: PartialFunction[Throwable, Result] = {
      case _: NoPrimaryRecordError => Redirect(controllers.routes.EligibilityController.howItWorks)
      case t: CacheRelationshipAlreadyUpdated => handle(t, warn, Redirect(controllers.routes.UpdateRelationshipController.finishUpdate))
      case t: CacheMissingUpdateRecord => handle(t, warn, InternalServerError(tryLater()))
      case t: CacheUpdateRequestNotSent => handle(t, warn, InternalServerError(tryLater()))
      case t: CannotUpdateRelationship => handle(t, warn, InternalServerError(tryLater()))
      case t: MultipleActiveRecordError => handle(t, warn, InternalServerError(tryLater()))
      case t: CitizenNotFound => handle(t, warn, InternalServerError(citizenNotFound()))
      case t: BadFetchRequest => handle(t, warn, InternalServerError(tryLater()))
      case t: TransferorNotFound => handle(t, warn, Ok(transferorNotFound()))
      case t: RecipientNotFound => handle(t, warn, Ok(recipientNotFound()))
      case t => handle(t, error, InternalServerError(tryLater()))
        }
    pf
    }
  }
