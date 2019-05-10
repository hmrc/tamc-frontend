/*
 * Copyright 2019 HM Revenue & Customs
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

import config.ApplicationConfig.{TAMC_JOURNEY_GDS, TAMC_JOURNEY_PTA}
import config.ApplicationConfig
import errors._
import forms.CurrentYearForm.currentYearForm
import forms.DateOfMarriageForm.dateOfMarriageForm
import forms.EarlierYearForm.earlierYearsForm
import forms.EmailForm.emailForm
import forms.EmptyForm
import forms.RecipientDetailsForm.recipientDetailsForm
import javax.inject.Inject
import models._
import models.auth.UserRequest
import org.apache.commons.lang3.exception.ExceptionUtils
import play.Logger
import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.mvc._
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.{ExecutionContext, Future}

class TransferController @Inject()(
                                   override val messagesApi: MessagesApi,
                                   authenticatedActionRefiner: AuthenticatedActionRefiner,
                                   registrationService: TransferService,
                                   cachingService: CachingService,
                                   timeService: TimeService
                                  )(implicit templateRenderer: TemplateRenderer,
                                    formPartialRetriever: FormPartialRetriever) extends BaseController {

  def transfer: Action[AnyContent] = authenticatedActionRefiner {
    implicit request =>
      Ok(views.html.multiyear.transfer.transfer(recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino)))
  }

  def transferAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino).bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.multiyear.transfer.transfer(formWithErrors))),
        recipientData => {
          cachingService.saveRecipientDetails(recipientData).map { _ =>
            Redirect(controllers.routes.TransferController.dateOfMarriage())
          }
        })
  }


  def dateOfMarriage: Action[AnyContent] = authenticatedActionRefiner {
    implicit request =>
      Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate)))
  }

  def dateOfMarriageAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.date_of_marriage(formWithErrors)))
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

  def eligibleYears: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      registrationService.deleteSelectionAndGetCurrentAndExtraYearEligibility map {
        case (false, Nil, _) => throw new NoTaxYearsAvailable
        case (false, extraYears, recipient) if extraYears.nonEmpty => Ok(views.html.multiyear.transfer.previous_years(recipient.data, extraYears, false))
        case (_, extraYears, recipient) =>
          Ok(views.html.multiyear.transfer.eligible_years(currentYearForm(extraYears.nonEmpty), extraYears.nonEmpty, recipient.data.name,
            Some(recipient.data.dateOfMarriage), Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
      } recover handleError
  }

  def eligibleYearsAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      registrationService.getCurrentAndExtraYearEligibility flatMap {
        case (currentYearAvailable, extraYears, recipient) =>
          currentYearForm(extraYears.nonEmpty).bindFromRequest.fold(
            hasErrors =>
              Future {
                BadRequest(views.html.multiyear.transfer.eligible_years(
                  hasErrors,
                  extraYears.nonEmpty,
                  recipient.data.name,
                  Some(recipient.data.dateOfMarriage),
                  Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
              },
            success => {
              registrationService.saveSelectedYears(recipient, if (success.applyForCurrentYear.contains(true)) {
                List[Int](timeService.getCurrentTaxYear)
              } else {
                List[Int]()
              }) map { _ =>
                if (extraYears.isEmpty && currentYearAvailable && (!success.applyForCurrentYear.contains(true))) {
                  throw new NoTaxYearsSelected
                } else if (extraYears.nonEmpty) {
                  Ok(views.html.multiyear.transfer.previous_years(recipient.data, extraYears, currentYearAvailable))
                } else {
                  Redirect(controllers.routes.TransferController.confirmYourEmail())
                }
              }
            })
      } recover handleError
  }

  def previousYears: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      registrationService.getCurrentAndExtraYearEligibility.map {
        case (currentYearAvailable, extraYears, recipient) =>
          Ok(views.html.multiyear.transfer.single_year_select(earlierYearsForm(), recipient.data, extraYears))
      } recover handleError
  }

  def extraYearsAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>

      def toTaxYears(years: List[Int]): List[TaxYear] = {
        years.map(year => TaxYear(year, None))
      }

      registrationService.getCurrentAndExtraYearEligibility flatMap {
        case (_, extraYears, recipient) =>
          earlierYearsForm(extraYears.map(_.year)).bindFromRequest.fold(
            hasErrors =>
              Future {
                BadRequest(views.html.multiyear.transfer.single_year_select(hasErrors.copy(errors = Seq(FormError("selectedYear", List("generic.select.answer"), List()))), recipient.data, extraYears))
              },
            taxYears => {
              registrationService.updateSelectedYears(recipient, taxYears.selectedYear, taxYears.yearAvailableForSelection).map {
                _ =>
                  if (taxYears.furtherYears.isEmpty) {
                    Redirect(controllers.routes.TransferController.confirmYourEmail())
                  } else {
                    Ok(views.html.multiyear.transfer.single_year_select(earlierYearsForm(), recipient.data, toTaxYears(taxYears.furtherYears)))
                  }
              }
            })
      } recover handleError
  }

  def confirmYourEmail: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      cachingService.fetchAndGetEntry[NotificationRecord](ApplicationConfig.CACHE_NOTIFICATION_RECORD) map {
        case Some(NotificationRecord(transferorEmail)) => Ok(views.html.multiyear.transfer.email(emailForm.fill(transferorEmail)))
        case None => Ok(views.html.multiyear.transfer.email(emailForm))
      }
  }

  def confirmYourEmailAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.multiyear.transfer.email(formWithErrors))),
        transferorEmail =>
          registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map {
            _ => Redirect(controllers.routes.TransferController.confirm())
          }) recover handleError
  }

  def confirm: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      registrationService.getConfirmationData(request.nino) map {
        data =>
          Ok(views.html.confirm(data = data, emptyForm = EmptyForm.form))
      } recover handleError
  }

  def confirmAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>

      val getJourneyName: String =
        if (request.authState.permanent) TAMC_JOURNEY_PTA else TAMC_JOURNEY_GDS

      EmptyForm.form.bindFromRequest().fold(
        formWithErrors =>
          Logger.error(s"unexpected error in emty form, SID [${utils.getSid(request)}]"),
        success =>
          success)
      Logger.info("registration service.createRelationship - confirm action.")
      registrationService.createRelationship(request.nino, getJourneyName) map {
        _ => Redirect(controllers.routes.TransferController.finished())
      } recover handleError
  }

  def finished: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      registrationService.getFinishedData(request.nino) map {
        case NotificationRecord(email) =>
          Ok(views.html.finished(transferorEmail = email))
      } recover handleError
  }

  def handleError(implicit hc: HeaderCarrier, request: UserRequest[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>
        val message: String = s"An exception occurred during processing of URI [${request.uri}] reason [$throwable,${throwable.getMessage}] SID [${utils.getSid(request)}] stackTrace [${ExceptionUtils.getStackTrace(throwable)}]"

        def handle(logger: String => Unit, result: Result): Result = {
          logger(message)
          result
        }

        throwable match {
          case _: TransferorNotFound               => handle(Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: RecipientNotFound                => handle(Logger.warn, InternalServerError(views.html.errors.recipient_not_found()))
          case _: TransferorDeceased               => handle(Logger.warn, InternalServerError(views.html.errors.transferer_deceased()))
          case _: CacheMissingTransferor           => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheTransferorInRelationship    => handle(Logger.warn, Ok(views.html.transferor_status()))
          case _: CacheMissingRecipient            => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheRecipientInRelationship     => handle(Logger.warn, InternalServerError(views.html.errors.recipient_relationship_exists()))
          case _: CacheMissingEmail                => handle(Logger.warn, Redirect(controllers.routes.TransferController.confirmYourEmail()))
          case _: CannotCreateRelationship         => handle(Logger.warn, InternalServerError(views.html.errors.relationship_cannot_create()))
          case _: CacheRelationshipAlreadyCreated  => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheCreateRequestNotSent        => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: NoTaxYearsSelected               => handle(Logger.info, Ok(views.html.errors.no_year_selected()))
          case _: NoTaxYearsAvailable              => handle(Logger.info, Ok(views.html.errors.no_eligible_years()))
          case _: NoTaxYearsForTransferor          => handle(Logger.info, InternalServerError(views.html.errors.no_tax_year_transferor()))
          case _: RelationshipMightBeCreated       => handle(Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _                                   => handle(Logger.error, InternalServerError(views.html.errors.try_later()))
        }
    }

}