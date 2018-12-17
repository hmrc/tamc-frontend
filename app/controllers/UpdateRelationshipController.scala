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

import com.google.inject.Inject
import config._
import errors._
import forms.ChangeRelationshipForm.{changeRelationshipForm, divorceForm, updateRelationshipDivorceForm, updateRelationshipForm}
import forms.EmailForm.emailForm
import forms.EmptyForm
import models._
import models.auth.UserRequest
import org.joda.time.LocalDate
import play.Logger
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, Request, Result}
import services.{CachingService, TimeService, TransferService, UpdateRelationshipService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class UpdateRelationshipController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             authenticatedActionRefiner: AuthenticatedActionRefiner,
                                             updateRelationshipService: UpdateRelationshipService,
                                             registrationService: TransferService,
                                             timeService: TimeService
                                            )(implicit tamcContext: TamcContext) extends BaseController {

  def history(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.listRelationship(request.nino) map {
        case (RelationshipRecordList(activeRelationship, historicRelationships, loggedInUserInfo, activeRecord, historicRecord, historicActiveRecord), canApplyPreviousYears) => {
          if (!activeRecord && !historicRecord) {
            if (!request.authState.permanent) {
              Redirect(controllers.routes.TransferController.transfer())
            } else {
              Redirect(controllers.routes.EligibilityController.howItWorks())
            }
          } else {
            Ok(views.html.coc.your_status(
              changeRelationshipForm = changeRelationshipForm,
              activeRelationship = activeRelationship,
              historicRelationships = historicRelationships,
              loggedInUserInfo = loggedInUserInfo,
              activeRecord = activeRecord,
              historicRecord = historicRecord,
              historicActiveRecord = historicActiveRecord,
              canApplyPreviousYears = canApplyPreviousYears,
              endOfYear = Some(timeService.taxYearResolver.endOfCurrentTaxYear)))
          }
        }
      } recover handleError
  }

  def makeChange(): Action[AnyContent] = authenticatedActionRefiner {
    implicit request =>
      changeRelationshipForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.warn("unexpected error in makeChange()")
          Redirect(controllers.routes.UpdateRelationshipController.history())
        },
        formData => {
          Ok(views.html.coc.reason_for_change(changeRelationshipForm.fill(formData)))
        }
      )
  }

  def updateRelationshipAction(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipForm.bindFromRequest.fold(
        formWithErrors => Future {
          Logger.warn("unexpected error in updateRelationshipAction()")
          val form = formWithErrors.fill(ChangeRelationship(formWithErrors.data.get("role"), None, Some(formWithErrors.data.get("historicActiveRecord").forall(_.equals("true")))))
          BadRequest(views.html.coc.reason_for_change(form))
        },
        formData => {
          CachingService.saveRoleRecord(formData.role.get).flatMap { _ ⇒
            (formData.endReason, formData.role) match {
              case (Some(EndReasonCode.CANCEL), _) => Future.successful {
                Redirect(controllers.routes.UpdateRelationshipController.confirmCancel())
              }
              case (Some(EndReasonCode.REJECT), _) =>
                updateRelationshipService.saveEndRelationshipReason(EndRelationshipReason(endReason = EndReasonCode.REJECT, timestamp = formData.creationTimestamp)) map {
                  _ => Redirect(controllers.routes.UpdateRelationshipController.confirmReject())
                }
              case (Some(EndReasonCode.DIVORCE), _) => Future.successful {
                Ok(views.html.coc.divorce_select_year(changeRelationshipForm.fill(formData)))
              }
              case (Some(EndReasonCode.EARNINGS), _) => Future.successful {
                Ok(views.html.coc.change_in_earnings_recipient())
              }
              case (Some(EndReasonCode.BEREAVEMENT), _) => Future.successful {
                Ok(views.html.coc.bereavement_recipient())
              }
              case (None, _) ⇒
                throw new Exception("Missing EndReasonCode")
              case _ => Future.successful {
                BadRequest(views.html.coc.reason_for_change(updateRelationshipForm.fill(formData)))
              }
            }
          }
        }
      )
  }

  def changeOfIncome: Action[AnyContent] = authenticatedActionRefiner {
    implicit request =>
      Ok(views.html.coc.change_in_earnings())
  }

  def bereavement: Action[AnyContent] = authenticatedActionRefiner {
    implicit request =>
      Ok(views.html.coc.bereavement_transferor())
  }

  def confirmYourEmailActionUpdate: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.coc.email(formWithErrors))),
        transferorEmail =>
          registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map {
            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmUpdate())
          }
      ) recover handleError
  }

  def divorceYear: Action[AnyContent] = authenticatedActionRefiner.async {
      implicit request =>
          updateRelationshipService.getUpdateRelationshipCacheDataForDateOfDivorce map {
            case Some(UpdateRelationshipCacheData(_, roleRecord, _, historicRelationships, _, relationshipEndReasonRecord,_)) =>
              val divorceForm = updateRelationshipDivorceForm.fill(ChangeRelationship(role= roleRecord, endReason = Some(relationshipEndReasonRecord.get.endReason), historicActiveRecord = Some(historicRelationships.isEmpty)))
              Ok(views.html.coc.divorce_select_year(changeRelationshipForm = divorceForm))
            case _ => throw CacheMissingUpdateRecord()
          } recover handleError
  }

  def divorceSelectYear(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipDivorceForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful {
            Logger.warn("unexpected end reason in divorceSelectYearAction")
            val form = formWithErrors.fill(ChangeRelationship(formWithErrors.data.get("role"), Some(EndReasonCode.DIVORCE), Some(formWithErrors.data.get("historicActiveRecord").forall(_.equals("true")))))
            BadRequest(views.html.coc.divorce_select_year(form))
          },
        formData =>
          updateRelationshipService.isValidDivorceDate(formData.dateOfDivorce) map {
            case true =>
              Ok(views.html.coc.divorce(
                changeRelationshipForm = updateRelationshipDivorceForm.fill(formData),
                cyEffectiveUntilDate = timeService.getEffectiveUntilDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = formData.dateOfDivorce)),
                cyEffectiveDate = Some(timeService.getEffectiveDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = formData.dateOfDivorce))),
                pyEffectiveDate = Some(timeService.getEffectiveDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_PY, dateOfDivorce = formData.dateOfDivorce)))))
            case false => Ok(views.html.coc.divorce_invalid_dod(updateRelationshipDivorceForm.fill(formData)))
          }
      )
  }

  private def getOptionalLocalDate(day: Option[String], month: Option[String], year: Option[String]): Option[LocalDate] =
    (day, month, year) match {
      case (Some(d), Some(m), Some(y)) => Some(new LocalDate(y.toInt, m.toInt, d.toInt))
      case _                           => None
    }

  def divorceAction(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      divorceForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful {
            Logger.warn("unexpected end reason while cancelling the relationship")
            val form = formWithErrors.fill(ChangeRelationship(
              formWithErrors.data.get("role"),
              Some(EndReasonCode.DIVORCE),
              Some(formWithErrors.data.get("historicActiveRecord").forall(_.equals("true"))),
              dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year"))))
            BadRequest(views.html.coc.divorce(form,
              cyEffectiveUntilDate = timeService.getEffectiveUntilDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year")))),
              cyEffectiveDate = Some(timeService.getEffectiveDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_CY, dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year"))))),
              pyEffectiveDate = Some(timeService.getEffectiveDate(EndRelationshipReason(endReason = EndReasonCode.DIVORCE_PY,dateOfDivorce = getOptionalLocalDate(formWithErrors.data.get("dateOfDivorce.day"), formWithErrors.data.get("dateOfDivorce.month"), formWithErrors.data.get("dateOfDivorce.year")))))))
          },
        formData =>
          updateRelationshipService.saveEndRelationshipReason(EndRelationshipReason(formData.endReason.get, formData.dateOfDivorce)) map {
            _ => Redirect(controllers.routes.UpdateRelationshipController.confirmEmail())
          }
      )
  }

  def confirmEmail: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.getUpdateNotification map {
        case Some(NotificationRecord(transferorEmail)) => Ok(views.html.coc.email(emailForm.fill(transferorEmail)))
        case None                                      => Ok(views.html.coc.email(emailForm))
      } recover handleError
  }

  def confirmReject(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.getUpdateRelationshipCacheForReject map {
        cache =>
          val selectedRelationship = updateRelationshipService.getRelationship(cache.get)
          val effectiveDate = Some(updateRelationshipService.getEndDate(cache.get.relationshipEndReasonRecord.get, selectedRelationship))
          Ok(views.html.coc.confirm_updates(EndReasonCode.REJECT, effectiveDate = effectiveDate,isEnded=Some(selectedRelationship.participant1EndDate.isDefined
            && selectedRelationship.participant1EndDate.nonEmpty && !selectedRelationship.participant1EndDate.get.equals(""))))
      }
  }

  def confirmCancel(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.saveEndRelationshipReason(EndRelationshipReason(EndReasonCode.CANCEL)) map {
        endReason =>
          Ok(views.html.coc.confirm_updates(endReason.endReason, effectiveUntilDate = timeService.getEffectiveUntilDate(endReason), effectiveDate = Some(timeService.getEffectiveDate(endReason))))
      }
  }

  def confirmUpdate: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.getConfirmationUpdateData map {
        case (data, Some(updateCache)) =>

          val reason = data.endRelationshipReason

          val (isEnded, relationEndDate, relevantDate) = reason match {
            case EndRelationshipReason(EndReasonCode.DIVORCE_PY, _, _) =>
              (false, None, Some(timeService.getEffectiveDate(reason)))
            case EndRelationshipReason(EndReasonCode.DIVORCE_CY, _, _) =>
              (false, None, timeService.getEffectiveUntilDate(reason))
            case EndRelationshipReason(EndReasonCode.CANCEL, _, _)     =>
              (false, None, timeService.getEffectiveUntilDate(reason))
            case EndRelationshipReason(EndReasonCode.REJECT, _, _)     =>

              val selectedRelationship = updateRelationshipService.getRelationship(updateCache)

              val isEnded = !selectedRelationship.participant1EndDate.contains("")

              val relationshipEndDate = if (isEnded) {
                Some(updateRelationshipService.getRelationEndDate(selectedRelationship))
              } else None

              (isEnded, relationshipEndDate, Some(updateRelationshipService.getEndDate(reason, selectedRelationship)))
          }
          Ok(views.html.coc.confirm(data, relevantDate, isEnded = isEnded, relationEndDate = relationEndDate))

        case (_, None) ⇒
          throw new Exception("No UpdateRelationshipCacheData found")
      } recover handleError
  }

  def confirmUpdateAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      EmptyForm.form.bindFromRequest().fold(
        formWithErrors =>
          Logger.warn(s"unexpected error in emty form while confirmUpdateAction, SID [${utils.getSid(request)}]"),
        success =>
          success)
      updateRelationshipService.updateRelationship(request.nino, request2lang(request)) map {
        _ => Redirect(controllers.routes.UpdateRelationshipController.finishUpdate())
      } recover handleError
  }

  def finishUpdate: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.getupdateRelationshipFinishedData(request.nino) map {
        case (NotificationRecord(email), rsn @ EndRelationshipReason(EndReasonCode.REJECT, _, _)) =>
          Ok(views.html.coc.finished(transferorEmail = email, reason = rsn))
        case (NotificationRecord(email), rsn) =>
          Ok(views.html.coc.finished(transferorEmail = email, reason = rsn, effectiveUntilDate = timeService.getEffectiveUntilDate(rsn), effectiveDate = Some(timeService.getEffectiveDate(rsn))))
      } recover handleError
  }

  def handleError(implicit hc: HeaderCarrier, ec: ExecutionContext, request: UserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>

        val message: String = s"An exception occurred during processing of URI [${request.uri}] SID [${utils.getSid(request)}]"

        def handle(message: String, logger: (String, Throwable) => Unit, result: Result): Result = {
          logger(message, throwable)
          result
        }

        throwable match {
          case _: CacheRelationshipAlreadyUpdated => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.finishUpdate()))
          case _: CacheMissingUpdateRecord        => handle(message, Logger.warn, InternalServerError(views.html.errors.try_later()))
          case _: CacheUpdateRequestNotSent       => handle(message, Logger.warn, InternalServerError(views.html.errors.try_later()))
          case _: CannotUpdateRelationship        => handle(message, Logger.warn, InternalServerError(views.html.errors.try_later()))
          case _: CitizenNotFound                 => handle(message, Logger.warn, InternalServerError(views.html.errors.citizen_not_found()))
          case _: BadFetchRequest                 => handle(message, Logger.warn, InternalServerError(views.html.errors.bad_request()))
          case _: TransferorNotFound              => handle(message, Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: RecipientNotFound               => handle(message, Logger.warn, InternalServerError(views.html.errors.recipient_not_found()))
          case _                                  => handle(message, Logger.error, InternalServerError(views.html.errors.try_later()))
        }
    }


}
