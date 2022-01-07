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

import config.ApplicationConfig
import controllers.actions.AuthenticatedActionRefiner
import errors._
import forms.CurrentYearForm.currentYearForm
import forms.EarlierYearForm.earlierYearsForm
import forms.EmailForm.emailForm
import forms.{DateOfMarriageForm, RecipientDetailsForm}
import models._
import models.auth.BaseUserRequest
import org.apache.commons.lang3.exception.ExceptionUtils
import play.api.data.FormError
import play.api.i18n.Lang
import play.api.mvc._
import play.twirl.api.Html
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import utils.LoggerHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransferController @Inject() (
  authenticate: AuthenticatedActionRefiner,
  registrationService: TransferService,
  cachingService: CachingService,
  timeService: TimeService,
  appConfig: ApplicationConfig,
  cc: MessagesControllerComponents,
  transferV: views.html.multiyear.transfer.transfer,
  dateOfMarriageV: views.html.date_of_marriage,
  previousYearsV: views.html.multiyear.transfer.previous_years,
  eligibleYearsV: views.html.multiyear.transfer.eligible_years,
  singleYearSelect: views.html.multiyear.transfer.single_year_select,
  email: views.html.multiyear.transfer.email,
  finishedV: views.html.finished,
  confirmV: views.html.confirm,
  transfererDeceased: views.html.errors.transferer_deceased,
  transferorNotFound: views.html.errors.transferor_not_found,
  recipientNotFound: views.html.errors.recipient_not_found,
  transferorStatus: views.html.transferor_status,
  noYearSelected: views.html.errors.no_year_selected,
  noEligibleYears: views.html.errors.no_eligible_years,
  noTaxYearTransferor: views.html.errors.no_tax_year_transferor,
  relationshipCannotCreate: views.html.errors.relationship_cannot_create,
  recipientRelationshipExists: views.html.errors.recipient_relationship_exists,
  tryLater: views.html.errors.try_later,
  recipientDetailsForm: RecipientDetailsForm,
  dateOfMarriageForm: DateOfMarriageForm)(implicit templateRenderer: TemplateRenderer, formPartialRetriever: FormPartialRetriever, ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def transfer: Action[AnyContent] = authenticate { implicit request =>
    Ok(
      transferV(recipientDetailsForm.recipientDetailsForm(timeService.getCurrentDate, request.nino))
    )
  }

  def transferAction: Action[AnyContent] = authenticate.async { implicit request =>
    recipientDetailsForm.recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino).bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(transferV(formWithErrors))),
      recipientData =>
        cachingService.saveRecipientDetails(recipientData).map { _ =>
          Redirect(controllers.routes.TransferController.dateOfMarriage)
        }
    )
  }

  def dateOfMarriage: Action[AnyContent] = authenticate { implicit request =>
    Ok(dateOfMarriageV(marriageForm = dateOfMarriageForm.dateOfMarriageForm(today = timeService.getCurrentDate)))
  }

  def dateOfMarriageWithCy: Action[AnyContent] = authenticate { implicit request =>
    Redirect(controllers.routes.TransferController.dateOfMarriage).withLang(Lang("cy"))
  }

  def dateOfMarriageWithEn: Action[AnyContent] = authenticate { implicit request =>
    Redirect(controllers.routes.TransferController.dateOfMarriage).withLang(Lang("en"))
  }

  def dateOfMarriageAction: Action[AnyContent] = authenticate.async { implicit request =>
    dateOfMarriageForm.dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(dateOfMarriageV(formWithErrors))),
      marriageData => {
        cachingService.saveDateOfMarriage(marriageData)

        registrationService.getRecipientDetailsFormData flatMap {
          case RecipientDetailsFormInput(name, lastName, gender, nino) =>
            val dataToSend = new RegistrationFormInput(name, lastName, gender, nino, marriageData.dateOfMarriage)
            registrationService.isRecipientEligible(request.nino, dataToSend) map { _ =>
              Redirect(controllers.routes.TransferController.eligibleYears)
            }
        }
      }
    ) recover handleError
  }

  def eligibleYears: Action[AnyContent] = authenticate.async { implicit request =>
    registrationService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility map {
      case CurrentAndPreviousYearsEligibility(false, Nil, _, _) =>
        throw new NoTaxYearsAvailable
      case CurrentAndPreviousYearsEligibility(false, previousYears, registrationInput, _) if previousYears.nonEmpty =>
        Ok(previousYearsV(registrationInput, previousYears, currentYearAvailable = false))
      case CurrentAndPreviousYearsEligibility(_, previousYears, registrationInput, _) =>
        Ok(
          eligibleYearsV(
            currentYearForm(previousYears.nonEmpty),
            previousYears.nonEmpty,
            registrationInput.name,
            Some(registrationInput.dateOfMarriage),
            Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))
          )
        )
    } recover handleError
  }

  def eligibleYearsAction: Action[AnyContent] = authenticate.async { implicit request =>
    registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
      case CurrentAndPreviousYearsEligibility(currentYearAvailable, previousYears, registrationInput, _) =>
        currentYearForm(previousYears.nonEmpty).bindFromRequest.fold(
          hasErrors =>
            Future {
              BadRequest(
                eligibleYearsV(
                  hasErrors,
                  previousYears.nonEmpty,
                  registrationInput.name,
                  Some(registrationInput.dateOfMarriage),
                  Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))
                )
              )
            },
          success => {

            val selectedYears = if (success.applyForCurrentYear.contains(true)) {
              List[Int](timeService.getCurrentTaxYear)
            } else {
              List[Int]()
            }

            registrationService.saveSelectedYears(selectedYears) map { _ =>
              if (previousYears.isEmpty && currentYearAvailable && (!success.applyForCurrentYear.contains(true))) {
                throw new NoTaxYearsSelected
              } else if (previousYears.nonEmpty) {
                Ok(previousYearsV(registrationInput, previousYears, currentYearAvailable))
              } else {
                Redirect(controllers.routes.TransferController.confirmYourEmail)
              }
            }
          }
        )
    } recover handleError
  }

  def previousYears: Action[AnyContent] = authenticate.async { implicit request =>
    registrationService.getCurrentAndPreviousYearsEligibility.map {
      case CurrentAndPreviousYearsEligibility(_, previousYears, registrationInput, _) =>
        Ok(singleYearSelect(earlierYearsForm(), registrationInput, previousYears))
    } recover handleError
  }

  def extraYearsAction: Action[AnyContent] = authenticate.async { implicit request =>
    def toTaxYears(years: List[Int]): List[TaxYear] =
      years.map(year => TaxYear(year, None))

    registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
      case CurrentAndPreviousYearsEligibility(_, extraYears, registrationInput, availableYears) =>
        earlierYearsForm(extraYears.map(_.year)).bindFromRequest.fold(
          hasErrors =>
            Future {
              BadRequest(
                singleYearSelect(
                  hasErrors.copy(errors =
                    Seq(FormError("selectedYear", List("pages.form.field-required.applyForHistoricYears"), List()))
                  ),
                  registrationInput,
                  extraYears
                )
              )
            },
          taxYears =>
            registrationService
              .updateSelectedYears(availableYears, taxYears.selectedYear, taxYears.yearAvailableForSelection)
              .map { _ =>
                if (taxYears.furtherYears.isEmpty) {
                  Redirect(controllers.routes.TransferController.confirmYourEmail)
                } else {
                  Ok(
                    singleYearSelect(earlierYearsForm(), registrationInput, toTaxYears(taxYears.furtherYears))
                  )
                }
              }
        )
    } recover handleError
  }

  def confirmYourEmail: Action[AnyContent] = authenticate.async { implicit request =>
    cachingService.fetchAndGetEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD) map {
      case Some(NotificationRecord(transferorEmail)) =>
        Ok(email(emailForm.fill(transferorEmail)))
      case None => Ok(email(emailForm))
    }
  }

  def confirmYourEmailAction: Action[AnyContent] = authenticate.async { implicit request =>
    emailForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(email(formWithErrors))),
      transferorEmail =>
        registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map { _ =>
          Redirect(controllers.routes.TransferController.confirm)
        }
    ) recover handleError
  }

  def confirm: Action[AnyContent] = authenticate.async { implicit request =>
    registrationService.getConfirmationData(request.nino) map { data =>
      Ok(confirmV(data = data))
    } recover handleError
  }

  def confirmAction: Action[AnyContent] = authenticate.async { implicit request =>
    registrationService.createRelationship(request.nino) map { _ =>
      Redirect(controllers.routes.TransferController.finished)
    } recover handleError
  }

  def finished: Action[AnyContent] = authenticate.async { implicit request =>
    registrationService.getFinishedData(request.nino) map { case NotificationRecord(email) =>
      cachingService.remove()
      Ok(finishedV(transferorEmail = email))
    } recover handleError
  }

  def cannotUseService: Action[AnyContent] = authenticate { implicit request =>
    Ok(transfererDeceased())
  }

  def handleError(implicit hc: HeaderCarrier, request: BaseUserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] { throwable: Throwable =>
      val message: String =
        s"An exception occurred during processing of URI [${request.uri}] reason [$throwable,${throwable.getMessage}] SID [${utils
          .getSid(request)}] stackTrace [${ExceptionUtils.getStackTrace(throwable)}]"

      def handle(logger: String => Unit, result: Result): Result = {
        logger(message)
        result
      }

      def handleWithException(ex: Throwable, view: Html): Result = {
        error(ex.getMessage(), ex)
        InternalServerError(view)
      }

      throwable match {
        case _: TransferorNotFound => handle(warn, Ok(transferorNotFound()))
        case _: RecipientNotFound  => handle(warn, Ok(recipientNotFound()))
        case _: TransferorDeceased =>
          handle(warn, Redirect(controllers.routes.TransferController.cannotUseService))
        case _: RecipientDeceased =>
          handle(warn, Redirect(controllers.routes.TransferController.cannotUseService))
        case _: CacheMissingTransferor =>
          handle(warn, Redirect(controllers.routes.UpdateRelationshipController.history))
        case _: CacheTransferorInRelationship => handle(warn, Ok(transferorStatus()))
        case _: CacheMissingRecipient =>
          handle(warn, Redirect(controllers.routes.UpdateRelationshipController.history))
        case _: CacheMissingEmail =>
          handle(warn, Redirect(controllers.routes.TransferController.confirmYourEmail))
        case _: CacheRelationshipAlreadyCreated =>
          handle(warn, Redirect(controllers.routes.UpdateRelationshipController.history))
        case _: CacheCreateRequestNotSent =>
          handle(warn, Redirect(controllers.routes.UpdateRelationshipController.history))
        case _: NoTaxYearsSelected      => handle(info, Ok(noYearSelected()))
        case _: NoTaxYearsAvailable     => handle(info, Ok(noEligibleYears()))
        case _: NoTaxYearsForTransferor => handle(info, Ok(noTaxYearTransferor()))
        case _: RelationshipMightBeCreated =>
          handle(warn, Redirect(controllers.routes.UpdateRelationshipController.history))
        case ex: CannotCreateRelationship => handleWithException(ex, relationshipCannotCreate())
        case ex: CacheRecipientInRelationship =>
          handleWithException(ex, recipientRelationshipExists())
        case ex => handleWithException(ex, tryLater())
      }
    }

}
