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
import config.ApplicationConfig
import controllers.actions.AuthenticatedActionRefiner
import errors._
import forms.ChangeRelationshipForm
import forms.EmailForm.emailForm
import forms.coc.{CheckClaimOrCancelDecisionForm, DivorceSelectYearForm, MakeChangesDecisionForm}
import models._
import models.auth.UserRequest
import org.joda.time.LocalDate
import play.Logger
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, Result}
import services._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.Referral
import viewModels.{ClaimsViewModel, ConfirmCancelViewModel, DivorceEndExplanationViewModel, EmailViewModel, HistorySummaryViewModel}

import scala.concurrent.Future
import scala.util.control.NonFatal

class UpdateRelationshipController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              authenticate: AuthenticatedActionRefiner,
                                              updateRelationshipService: UpdateRelationshipService,
                                              registrationService: TransferService,
                                              cachingService: CachingService,
                                              timeService: TimeService
                                            )(implicit templateRenderer: TemplateRenderer,
                                              formPartialRetriever: FormPartialRetriever) extends BaseController with Referral {

  def history(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.retrieveRelationshipRecords(request.nino) flatMap { relationshipRecords =>
        if (relationshipRecords.recordStatus != Active && relationshipRecords.recordStatus != ActiveHistoric) {
          if (!request.authState.permanent) {
            Future.successful(Redirect(controllers.routes.TransferController.transfer()))
          } else {
            Future.successful(Redirect(controllers.routes.EligibilityController.howItWorks()))
          }
        } else {
          //TODO cache the actual object
          updateRelationshipService.saveRelationshipRecords(relationshipRecords) map { _ =>
            val viewModel = HistorySummaryViewModel(relationshipRecords)
            //TODO fail when loggedInUSerInfo is None
            Ok(views.html.coc.history_summary(relationshipRecords.loggedInUserInfo, viewModel))
          }
        }
      } recover handleError
  }

  def decision: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getCheckClaimOrCancelDecision map { claimOrCancelDecision =>
        Ok(views.html.coc.decision(CheckClaimOrCancelDecisionForm.form.fill(claimOrCancelDecision)))
      } recover {
        //open empty view even with there are cache problem or fail to map data to view
        case NonFatal(_) =>
          Ok(views.html.coc.decision(CheckClaimOrCancelDecisionForm.form))
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
          //TODO It would fail on the following inputs:
          //None, Some((x: String forSome x not in (CheckMarriageAllowanceClaim, StopMarriageAllowance)))
        })
  }

  def claims: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getRelationshipRecords map { relationshipRecords =>

        val viewModel = ClaimsViewModel(
          relationshipRecords.activeRelationship,
          relationshipRecords.historicRelationships,
          relationshipRecords.recordStatus)

        Ok(views.html.coc.claims(viewModel))
      }
    //TODO add recover or something here?
  }

  def makeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getMakeChangesDecision map { makeChangesData =>
        Ok(views.html.coc.reason_for_change(MakeChangesDecisionForm.form.fill(makeChangesData)))
      }
  }

  //TODO add tests!!!
  def submitMakeChange(): Action[AnyContent] = authenticate.async {
    implicit request =>
      MakeChangesDecisionForm.form.bindFromRequest.fold(
        //TODO Need to test this code???
        formWithErrors => {
          Future.successful(BadRequest(views.html.coc.reason_for_change(formWithErrors)))
        }, {
          case Some(MakeChangesDecisionForm.Divorce) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Divorce) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.divorceEnterYear())
            }
          }
          case Some(MakeChangesDecisionForm.IncomeChanges) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.IncomeChanges) flatMap { _ =>
             changeOfIncomeRedirect
            }

          }
          case Some(MakeChangesDecisionForm.NoLongerRequired) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.NoLongerRequired) flatMap { _ =>
              noLongerWantMarriageAllowanceRedirect
            }
          }
          case Some(MakeChangesDecisionForm.Bereavement) => {
            updateRelationshipService.saveMakeChangeDecision(MakeChangesDecisionForm.Bereavement) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.bereavement())
            }
          }
          //            TODO It would fail on the following inputs: None, Some((x: String forSome x not in (Bereavement, Divorce, IncomeChanges, NoLongerRequired)))
        })
  }

  private def noLongerWantMarriageAllowanceRedirect(implicit hc: HeaderCarrier): Future[Result] = {
    updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
      if(relationshipRecords.role == Recipient){
        Redirect(controllers.routes.UpdateRelationshipController.stopAllowance())
      } else {
        Redirect(controllers.routes.UpdateRelationshipController.cancel())
      }
    }
  }


  private def changeOfIncomeRedirect(implicit hc: HeaderCarrier): Future[Result] = {
    updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
      if(relationshipRecords.role == Recipient){
        Redirect(controllers.routes.UpdateRelationshipController.stopAllowance())
      } else {
        Redirect(controllers.routes.UpdateRelationshipController.changeOfIncome())
      }
    }
  }

  //TODO referor
  def stopAllowance: Action[AnyContent] = authenticate.async {
    implicit request =>
      Future.successful(Ok(views.html.coc.stopAllowance()))
  }

  //TODO referor
  def cancel: Action[AnyContent] = authenticate.async {
    implicit request =>

      val (maEndDate, paEffectiveDate) = updateRelationshipService.getCancelDates
      cachingService.cacheValue[LocalDate](ApplicationConfig.CACHE_MA_END_DATE, maEndDate).map {
        _ =>
        Ok(views.html.coc.cancel(maEndDate, paEffectiveDate))
      }
  }

  //TODO referor
  def changeOfIncome: Action[AnyContent] = authenticate.async {
    implicit request =>
      Future.successful(Ok(views.html.coc.change_in_earnings()))
  }

  def bereavement: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getRelationshipRecords map { relationshipRecords =>
        Ok(views.html.coc.bereavement(relationshipRecords.role))
      }
  }

  def divorceEnterYear: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getDivorceDate map { divorceDate =>
        Ok(views.html.coc.divorce_select_year(DivorceSelectYearForm.form.fill(divorceDate)))
      } recover {
        //open empty view even with there are cache problem or fail to map data to view
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
          case Some(divorceDate) =>
            updateRelationshipService.saveDivorceDate(divorceDate) map { _ =>
              Redirect(controllers.routes.UpdateRelationshipController.divorceEndExplanation())
            }
          //TODO fail for else case. Not sure if this is required
          case _ =>
            ???
        }
      )
  }

  def divorceEndExplanation: Action[AnyContent] = authenticate.async {
    implicit request =>
      for {
        (role, divorceDate) <- updateRelationshipService.getDivorceExplanationData
        (marriageAllowanceEndDate, personalAllowanceEffectiveDate) = updateRelationshipService.getDatesForDivorce(role, divorceDate)
        _ <- cachingService.cacheValue[LocalDate](ApplicationConfig.CACHE_MA_END_DATE, marriageAllowanceEndDate)
        _ <- cachingService.cacheValue[LocalDate](ApplicationConfig.CACHE_PA_EFFECTIVE_DATE, personalAllowanceEffectiveDate)
      } yield {
       val viewModel = DivorceEndExplanationViewModel(role, divorceDate, marriageAllowanceEndDate, personalAllowanceEffectiveDate)

        Ok(views.html.coc.divorce_end_explanation(viewModel))
      }
  }

  //TODO is this the way to fill the views to remove optional in forms?
  def confirmEmail: Action[AnyContent] = authenticate.async {
    implicit request =>
      lazy val viewModel = EmailViewModel(referer)
      updateRelationshipService.getEmailAddress map {
        case Some(email) => Ok(views.html.coc.email(emailForm.fill(EmailAddress(email)), viewModel))
        case None => Ok(views.html.coc.email(emailForm, viewModel))
      } recover handleError
  }

  def confirmReject(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getUpdateRelationshipCacheForReject map {
        cache =>
          val selectedRelationship = updateRelationshipService.getRelationship(cache.get)
          val effectiveDate = Some(updateRelationshipService.getEndDate(cache.get.relationshipEndReasonRecord.get, selectedRelationship))
          Ok(views.html.coc.confirm_updates(EndReasonCode.REJECT, effectiveDate = effectiveDate, isEnded = Some(selectedRelationship.participant1EndDate.isDefined
            && selectedRelationship.participant1EndDate.nonEmpty && !selectedRelationship.participant1EndDate.get.equals(""))))
      }
  }

  def confirmCancel(): Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.saveEndRelationshipReason(EndRelationshipReason(EndReasonCode.CANCEL)) map {
        endReason =>
          Ok(views.html.coc.confirm_updates(endReason.endReason, effectiveUntilDate = timeService.getEffectiveUntilDate(endReason), effectiveDate = Some(timeService.getEffectiveDate(endReason))))
      }
  }

  //TODO why was registration service used. rename
  def confirmYourEmailActionUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors => {
          val viewModel = EmailViewModel(referer)
          Future.successful(BadRequest(views.html.coc.email(formWithErrors, viewModel)))
        },
        email =>
          updateRelationshipService.saveEmailAddress(email) map {
            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmUpdate())
          }
        //TODO is this required
      ) recover handleError
  }


  def confirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      UpdateRelationshipService.getConfirmationUpdateAnswers map {
        confirmationUpdateAnswers =>
        Ok(views.html.coc.confirm(ConfirmCancelViewModel(confirmationUpdateAnswers)))
      }
  }

  def confirmUpdateAction: Action[AnyContent] = authenticate.async {
    implicit request =>
//      EmptyForm.form.bindFromRequest().fold(
//        formWithErrors =>
//          Logger.warn(s"unexpected error in empty form while confirmUpdateAction, SID [${utils.getSid(request)}]"),
//        success =>
//          success)
//
//      updateRelationshipService.updateRelationship(request.nino) map {
//        _ => Redirect(controllers.routes.UpdateRelationshipController.finishUpdate())
//      } recover handleError
      ???
  }

  def submitConfirmUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.updateRelationship(request.nino) map {
       _ => Redirect(controllers.routes.UpdateRelationshipController.finishUpdate())
      }
  }


  def historyWithCy: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(controllers.routes.UpdateRelationshipController.history()).withLang(Lang("cy"))
        .flashing(LanguageUtils.FlashWithSwitchIndicator)
  }

  def historyWithEn: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(controllers.routes.UpdateRelationshipController.history()).withLang(Lang("en"))
        .flashing(LanguageUtils.FlashWithSwitchIndicator)
  }

  def updateRelationshipAction(): Action[AnyContent] = authenticate.async {
    implicit request =>
//      updateRelationshipForm.bindFromRequest.fold(
//        formWithErrors => Future {
//          Logger.warn("unexpected error in updateRelationshipAction()")
//          val form = formWithErrors.fill(ChangeRelationship(formWithErrors.data.get("role"), None, Some(formWithErrors.data.get("historicActiveRecord").forall(_.equals("true")))))
//          BadRequest(views.html.coc.reason_for_change(form))
//        },
//        formData => {
//          cachingService.saveRoleRecord(formData.role.get).flatMap { _ =>
//            (formData.endReason, formData.role) match {
//              case (Some(EndReasonCode.CANCEL), _) => Future.successful {
//                Redirect(controllers.routes.UpdateRelationshipController.confirmCancel())
//              }
//              case (Some(EndReasonCode.REJECT), _) =>
//                updateRelationshipService.saveEndRelationshipReason(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = formData.creationTimestamp)) map {
//                  _ => Redirect(controllers.routes.UpdateRelationshipController.confirmReject())
//                }
//              case (Some(EndReasonCode.DIVORCE), _) => Future.successful {
//                Ok(views.html.coc.divorce_select_year(changeRelationshipForm.fill(formData)))
//              }
//              case (Some(EndReasonCode.EARNINGS), _) => Future.successful {
//                Ok(views.html.coc.change_in_earnings_recipient())
//              }
//              case (Some(EndReasonCode.BEREAVEMENT), _) => Future.successful {
//                Ok(views.html.coc.bereavement_recipient())
//              }
//              case (None, _) =>
//                throw new Exception("Missing EndReasonCode")
//              case _ => Future.successful {
//                BadRequest(views.html.coc.reason_for_change(updateRelationshipForm.fill(formData)))
//              }
//            }
//          }
//        }
//      )

      ???
  }


  def divorceYear: Action[AnyContent] = authenticate.async {
    implicit request =>
//      updateRelationshipService.getUpdateRelationshipCacheDataForDateOfDivorce map {
//        case Some(UpdateRelationshipCacheData(_, roleRecord, _, historicRelationships, _, relationshipEndReasonRecord, _)) =>
//          val divorceForm = updateRelationshipDivorceForm.fill(ChangeRelationship(role = roleRecord, endReason = Some(relationshipEndReasonRecord.get.endReason), historicActiveRecord = Some(historicRelationships.isEmpty)))
//          Ok(views.html.coc.divorce_select_year(changeRelationshipForm = divorceForm))
//        case _ => throw CacheMissingUpdateRecord()
//      } recover handleError

      ???
  }

  def divorceAction(): Action[AnyContent] = authenticate.async {
    implicit request =>
//      divorceForm.bindFromRequest.fold(
//        formWithErrors =>
//          Future.successful {
//            Logger.warn("unexpected end reason while cancelling the relationship")
//            val form = formWithErrors.fill(ChangeRelationship(
//              formWithErrors.data.get("role"),
//              Some(EndReasonCode.DIVORCE),
//              Some(formWithErrors.data.get("historicActiveRecord").forall(_.equals("true"))),
//              dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year"))))
//            BadRequest(views.html.coc.divorce(form,
//              cyEffectiveUntilDate = timeService.getEffectiveUntilDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year")))),
//              cyEffectiveDate = Some(timeService.getEffectiveDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year"))))),
//              pyEffectiveDate = Some(timeService.getEffectiveDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_PY, dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year")))))))
//          },
//        formData =>
//          updateRelationshipService.saveEndRelationshipReason(EndRelationshipReason(formData.endReason.get, formData.dateOfDivorce)) map {
//            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmEmail())
//          }
//      )
      ???
  }


  def handleError(implicit hc: HeaderCarrier, request: UserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>

        val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

        def handle(logger: (String, Throwable) => Unit, result: Result): Result = {
          logger(message, throwable)
          result
        }

        throwable match {
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

  private[controllers] def getConfirmationInfoFromReason(reason: EndRelationshipReason,
                                                         cacheData: UpdateRelationshipCacheData): (Boolean, Option[LocalDate], Option[LocalDate]) = {
    reason.endReason match {
      case EndReasonCode.DIVORCE_PY =>
        (false, None, Some(timeService.getEffectiveDate(reason)))

      case EndReasonCode.DIVORCE_CY =>
        (false, None, timeService.getEffectiveUntilDate(reason))

      case EndReasonCode.CANCEL =>
        (false, None, timeService.getEffectiveUntilDate(reason))

      case EndReasonCode.REJECT =>
        val selectedRelationship = updateRelationshipService.getRelationship(cacheData)
        val isEnded = selectedRelationship.participant1EndDate.exists(_ != "")

        val relationshipEndDate = if (isEnded) {
          Some(updateRelationshipService.getRelationEndDate(selectedRelationship))
        } else None

        (isEnded, relationshipEndDate, Some(updateRelationshipService.getEndDate(reason, selectedRelationship)))
    }
  }





  def finishUpdate: Action[AnyContent] = authenticate.async {
    implicit request =>
      updateRelationshipService.getupdateRelationshipFinishedData(request.nino) map {
        case (NotificationRecord(email), _) =>
          Ok(views.html.coc.finished(emailAddress = email))
      } recover handleError
  }

  private def getOptionalLocalDate(day: Option[String], month: Option[String], year: Option[String]): Option[LocalDate] =
    (day, month, year) match {
      case (Some(d), Some(m), Some(y)) => Some(new LocalDate(y.toInt, m.toInt, d.toInt))
      case _ => None
    }
}
