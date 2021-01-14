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

import config.ApplicationConfig
import controllers.actions.AuthenticatedActionRefiner
import errors._
import forms.CurrentYearForm.currentYearForm
import forms.DateOfMarriageForm.dateOfMarriageForm
import forms.EarlierYearForm.earlierYearsForm
import forms.EmailForm.emailForm
import forms.RecipientDetailsForm.recipientDetailsForm
import javax.inject.Inject
import models._
import models.auth.BaseUserRequest
import org.apache.commons.lang3.exception.ExceptionUtils
import play.Logger
import play.api.data.FormError
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc._
import play.twirl.api.Html
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future

class TransferController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    authenticate: AuthenticatedActionRefiner,
                                    registrationService: TransferService,
                                    cachingService: CachingService,
                                    timeService: TimeService,
                                    appConfig: ApplicationConfig
                                  )(implicit templateRenderer: TemplateRenderer,
                                    formPartialRetriever: FormPartialRetriever) extends BaseController {

  def transfer: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.multiyear.transfer.transfer(recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino), applicationConfig = appConfig))
  }

  def transferAction: Action[AnyContent] = authenticate.async {
    implicit request =>
      recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino).bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.multiyear.transfer.transfer(formWithErrors, applicationConfig = appConfig))),
        recipientData => {
          cachingService.saveRecipientDetails(recipientData).map { _ =>
            Redirect(controllers.routes.TransferController.dateOfMarriage())
          }
        })
  }

  def dateOfMarriage: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate), applicationConfig = appConfig))
  }

  def dateOfMarriageWithCy: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(controllers.routes.TransferController.dateOfMarriage()).withLang(Lang("cy"))
  }

  def dateOfMarriageWithEn: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(controllers.routes.TransferController.dateOfMarriage()).withLang(Lang("en"))
  }

  def dateOfMarriageAction: Action[AnyContent] = authenticate.async {
    implicit request =>
      dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.date_of_marriage(formWithErrors, appConfig)))
        ,
        marriageData => {
          cachingService.saveDateOfMarriage(marriageData)


          registrationService.getRecipientDetailsFormData flatMap {
            case RecipientDetailsFormInput(name, lastName, gender, nino) => {
              val dataToSend = new RegistrationFormInput(name, lastName, gender, nino, marriageData.dateOfMarriage)
              registrationService.isRecipientEligible(request.nino, dataToSend) map {
                _ => Redirect(controllers.routes.TransferController.eligibleYears())
              }
            }
          }
        }) recover handleError
  }

  def eligibleYears: Action[AnyContent] = authenticate.async {
    implicit request =>
        registrationService.deleteSelectionAndGetCurrentAndPreviousYearsEligibility map {
        case CurrentAndPreviousYearsEligibility(false, Nil, _, _) =>
          throw new NoTaxYearsAvailable
        case CurrentAndPreviousYearsEligibility(false, previousYears, registrationInput, _)
          if previousYears.nonEmpty => Ok(views.html.multiyear.transfer.previous_years(registrationInput, previousYears, currentYearAvailable = false, applicationConfig = appConfig))
        case CurrentAndPreviousYearsEligibility(_, previousYears, registrationInput, _) =>
          Ok(views.html.multiyear.transfer.eligible_years(currentYearForm(previousYears.nonEmpty), previousYears.nonEmpty, registrationInput.name,
            Some(registrationInput.dateOfMarriage), Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear)), appConfig))
      } recover handleError
  }

  def eligibleYearsAction: Action[AnyContent] = authenticate.async {
    implicit request =>
      registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
        case CurrentAndPreviousYearsEligibility(currentYearAvailable, previousYears, registrationInput, _) =>
          currentYearForm(previousYears.nonEmpty).bindFromRequest.fold(
            hasErrors =>
              Future {
                BadRequest(views.html.multiyear.transfer.eligible_years(
                  hasErrors,
                  previousYears.nonEmpty,
                  registrationInput.name,
                  Some(registrationInput.dateOfMarriage),
                  Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear)),
                  appConfig: ApplicationConfig))
              },
            success => {

              val selectedYears = if(success.applyForCurrentYear.contains(true)) {
                List[Int](timeService.getCurrentTaxYear)
              } else {
                List[Int]()
              }

              registrationService.saveSelectedYears(selectedYears) map { _ =>
                if (previousYears.isEmpty && currentYearAvailable && (!success.applyForCurrentYear.contains(true))) {
                  throw new NoTaxYearsSelected
                } else if (previousYears.nonEmpty) {
                  Ok(views.html.multiyear.transfer.previous_years(registrationInput, previousYears, currentYearAvailable, applicationConfig = appConfig))
                } else {
                  Redirect(controllers.routes.TransferController.confirmYourEmail())
                }
              }
            })
      } recover handleError
  }

  def previousYears: Action[AnyContent] = authenticate.async {
    implicit request =>
      registrationService.getCurrentAndPreviousYearsEligibility.map {
        case CurrentAndPreviousYearsEligibility(_, previousYears, registrationInput, _) =>
          Ok(views.html.multiyear.transfer.single_year_select(earlierYearsForm(), registrationInput, previousYears, appConfig))
      } recover handleError
  }

  def extraYearsAction: Action[AnyContent] = authenticate.async {
    implicit request =>

      def toTaxYears(years: List[Int]): List[TaxYear] = {
        years.map(year => TaxYear(year, None))
      }

      registrationService.getCurrentAndPreviousYearsEligibility.flatMap {
        case CurrentAndPreviousYearsEligibility(_, extraYears, registrationInput, availableYears) =>
          earlierYearsForm(extraYears.map(_.year)).bindFromRequest.fold(
            hasErrors =>
              Future {
                BadRequest(views.html.multiyear.transfer.single_year_select(hasErrors.copy(errors = Seq(FormError("selectedYear", List("generic.select.answer"), List()))), registrationInput, extraYears, appConfig))
              },
            taxYears => {
              registrationService.updateSelectedYears(availableYears, taxYears.selectedYear, taxYears.yearAvailableForSelection).map {
                _ =>
                  if (taxYears.furtherYears.isEmpty) {
                    Redirect(controllers.routes.TransferController.confirmYourEmail())
                  } else {
                    Ok(views.html.multiyear.transfer.single_year_select(earlierYearsForm(), registrationInput, toTaxYears(taxYears.furtherYears), appConfig))
                  }
              }
            })
      } recover handleError
  }

  def confirmYourEmail: Action[AnyContent] = authenticate.async {
    implicit request =>
      cachingService.fetchAndGetEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD) map {
        case Some(NotificationRecord(transferorEmail)) => Ok(views.html.multiyear.transfer.email(emailForm.fill(transferorEmail), applicationConfig = appConfig))
        case None => Ok(views.html.multiyear.transfer.email(emailForm, applicationConfig = appConfig))
      }
  }

  def confirmYourEmailAction: Action[AnyContent] = authenticate.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.multiyear.transfer.email(formWithErrors, applicationConfig = appConfig))),
        transferorEmail =>
          registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map {
            _ => Redirect(controllers.routes.TransferController.confirm())
          }) recover handleError
  }

  def confirm: Action[AnyContent] = authenticate.async {
    implicit request =>
      registrationService.getConfirmationData(request.nino) map {
        data =>
          Ok(views.html.confirm(data = data, appConfig))
      } recover handleError
  }

  def confirmAction: Action[AnyContent] = authenticate.async {
    implicit request =>

      registrationService.createRelationship(request.nino) map {
        _ => Redirect(controllers.routes.TransferController.finished())
      } recover handleError
  }

  def finished: Action[AnyContent] = authenticate.async {
    implicit request =>
      registrationService.getFinishedData(request.nino) map {
        case NotificationRecord(email) =>
          cachingService.remove()
          Ok(views.html.finished(transferorEmail = email, appConfig))
      } recover handleError
  }

  def cannotUseService: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.errors.transferer_deceased(appConfig))
  }

  def handleError(implicit hc: HeaderCarrier, request: BaseUserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>
        val message: String = s"An exception occurred during processing of URI [${request.uri}] reason [$throwable,${throwable.getMessage}] SID [${utils.getSid(request)}] stackTrace [${ExceptionUtils.getStackTrace(throwable)}]"

        def handle(logger: String => Unit, result: Result): Result = {
          logger(message)
          result
        }

        def handleWithException(ex: Throwable, view: Html): Result = {
          Logger.error(ex.getMessage(), ex)
          InternalServerError(view)
        }

        throwable match {
          case _: TransferorNotFound               => handle(Logger.warn, Ok(views.html.errors.transferor_not_found(appConfig)))
          case _: RecipientNotFound                => handle(Logger.warn, Ok(views.html.errors.recipient_not_found(appConfig)))
          case _: TransferorDeceased               => handle(Logger.warn, Redirect(controllers.routes.TransferController.cannotUseService()))
          case _: RecipientDeceased                => handle(Logger.warn, Redirect(controllers.routes.TransferController.cannotUseService()))
          case _: CacheMissingTransferor           => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheTransferorInRelationship    => handle(Logger.warn, Ok(views.html.transferor_status(appConfig)))
          case _: CacheMissingRecipient            => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheMissingEmail                => handle(Logger.warn, Redirect(controllers.routes.TransferController.confirmYourEmail()))
          case _: CacheRelationshipAlreadyCreated  => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheCreateRequestNotSent        => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: NoTaxYearsSelected               => handle(Logger.info, Ok(views.html.errors.no_year_selected(appConfig)))
          case _: NoTaxYearsAvailable              => handle(Logger.info, Ok(views.html.errors.no_eligible_years(appConfig)))
          case _: NoTaxYearsForTransferor          => handle(Logger.info, Ok(views.html.errors.no_tax_year_transferor(appConfig)))
          case _: RelationshipMightBeCreated       => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case ex: CannotCreateRelationship        => handleWithException(ex, views.html.errors.relationship_cannot_create(appConfig))
          case ex: CacheRecipientInRelationship    => handleWithException(ex, views.html.errors.recipient_relationship_exists(appConfig))
          case ex                                  => handleWithException(ex, views.html.errors.try_later(appConfig))
        }
    }

}
