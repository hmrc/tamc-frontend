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

import com.google.inject.Inject
import controllers.actions.AuthenticatedActionRefiner
import errors._
import forms.EmailForm.emailForm
import forms.coc.{CheckClaimOrCancelDecisionForm, DivorceSelectYearForm, MakeChangesDecisionForm}
import models._
import models.auth.UserRequest
import play.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Result}
import services._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import viewModels._

import scala.concurrent.Future
import scala.util.control.NonFatal

class UpdateRelationshipController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              authenticate: AuthenticatedActionRefiner,
                                              updateRelationshipService: UpdateRelationshipService,
                                              timeService: TimeService
                                            )(implicit templateRenderer: TemplateRenderer,
                                              formPartialRetriever: FormPartialRetriever) extends BaseController {

  def history(): Action[AnyContent] = authenticate.async {
    implicit request =>
      (updateRelationshipService.retrieveRelationshipRecords(request.nino) flatMap { relationshipRecords =>
        updateRelationshipService.saveRelationshipRecords(relationshipRecords) map { _ =>
            val viewModel = HistorySummaryViewModel(relationshipRecords)
            Ok(views.html.coc.history_summary(viewModel))
        }
      }) recover handleError
  }

  private def noPrimaryRecordRedirect(request: UserRequest[_]): Result = {
    if (!request.authState.permanent) {
      Redirect(controllers.routes.TransferController.transfer())
    } else {
     Redirect(controllers.routes.EligibilityController.howItWorks())
    }
  }

  def decision: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getCheckClaimOrCancelDecision map { claimOrCancelDecision =>
        Ok(views.html.coc.decision(CheckClaimOrCancelDecisionForm.form.fill(claimOrCancelDecision)))
      } recover {
        case NonFatal(_) => Ok(views.html.coc.decision(CheckClaimOrCancelDecisionForm.form))
      }
  }

  def submitDecision: Action[AnyContent] = authenticate.async {
    implicit request =>

      CheckClaimOrCancelDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.coc.decision(formWithErrors)))
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
        Ok(views.html.coc.claims(viewModel))
      }) recover handleError
  }


  def makeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getMakeChangesDecision map { makeChangesData =>
        Ok(views.html.coc.reason_for_change(MakeChangesDecisionForm.form.fill(makeChangesData.map(_.toString))))
      } recover {
        case NonFatal(_) => Ok(views.html.coc.reason_for_change(MakeChangesDecisionForm.form))
      }
  }

  def submitMakeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      MakeChangesDecisionForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.coc.reason_for_change(formWithErrors)))
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
      Future.successful(Ok(views.html.coc.stopAllowance()))
  }


  def cancel: Action[AnyContent] = authenticate.async {
    implicit request =>
      val cancelDates = updateRelationshipService.getMAEndingDatesForCancelation
      updateRelationshipService.saveMarriageAllowanceEndingDates(cancelDates) map { _ =>
        Ok(views.html.coc.cancel(cancelDates))
      } recover handleError
  }

  def changeOfIncome: Action[AnyContent] = authenticate.async {
    implicit request =>
      Future.successful(Ok(views.html.coc.change_in_earnings()))
  }

  def bereavement: Action[AnyContent] = authenticate.async {
    implicit request =>
      (updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
        Ok(views.html.coc.bereavement(relationshipRecords.primaryRecord.role))
      }) recover handleError
  }

  def divorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getDivorceDate map { optionalDivorceDate =>
        optionalDivorceDate.fold(Ok(views.html.coc.divorce_select_year(DivorceSelectYearForm.form))){ divorceDate =>
          Ok(views.html.coc.divorce_select_year(DivorceSelectYearForm.form.fill(divorceDate)))
        }
      } recover {
        case NonFatal(_) =>
          Ok(views.html.coc.divorce_select_year(DivorceSelectYearForm.form))
      }
  }

  def submitDivorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      DivorceSelectYearForm.form.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.coc.divorce_select_year(formWithErrors)))
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
        Ok(views.html.coc.divorce_end_explanation(viewModel))
      }) recover handleError
  }


  def confirmEmail: Action[AnyContent] = authenticate.async {
    implicit request =>
      //TODO browser back in place of referer

      lazy val emptyEmailView = views.html.coc.email(emailForm)

      updateRelationshipService.getEmailAddress map {
        case Some(email) => Ok(views.html.coc.email(emailForm.fill(EmailAddress(email))))
        case None => Ok(emptyEmailView)
      } recover {
        case NonFatal(_) => Ok(emptyEmailView)
      }
  }


  def confirmYourEmailActionUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors => {
          Future.successful(BadRequest(views.html.coc.email(formWithErrors)))
        },
        email =>
          updateRelationshipService.saveEmailAddress(email.value) map {
            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmUpdate())
          }
      ) recover handleError
  }

  def confirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getConfirmationUpdateAnswers map { confirmationUpdateAnswers =>
          Ok(views.html.coc.confirmUpdate(ConfirmUpdateViewModel(confirmationUpdateAnswers)))
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
      } yield Ok(views.html.coc.finished(EmailAddress(email)))) recover handleError

  }

  private def handleError(implicit hc: HeaderCarrier, request: UserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>

        val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

        def handle(logger: (String, Throwable) => Unit, result: Result): Result = {
          logger(message, throwable)
          result
        }

        throwable match {
          case _: NoPrimaryRecordError => noPrimaryRecordRedirect(request)
          case _: CacheRelationshipAlreadyUpdated => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.finishUpdate()))
          case _: CacheMissingUpdateRecord => handle(Logger.warn, InternalServerError(views.html.errors.try_later()))
          case _: CacheUpdateRequestNotSent => handle(Logger.warn, InternalServerError(views.html.errors.try_later()))
          case _: CannotUpdateRelationship => handle(Logger.warn, InternalServerError(views.html.errors.try_later()))
          case _: CitizenNotFound => handle(Logger.warn, InternalServerError(views.html.errors.citizen_not_found()))
          case _: BadFetchRequest => handle(Logger.warn, InternalServerError(views.html.errors.bad_request()))
          case _: TransferorNotFound => handle(Logger.warn, Ok(views.html.errors.transferor_not_found()))
          case _: RecipientNotFound => handle(Logger.warn, Ok(views.html.errors.recipient_not_found()))
          case _ => handle(Logger.error, InternalServerError(views.html.errors.try_later()))
        }
    }

}
