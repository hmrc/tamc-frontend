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

import actions.{AuthorisedActions, JourneyEnforcers, MarriageAllowanceRegime}
import config.ApplicationConfig
import connectors.ApplicationAuthConnector
import details.{CitizenDetailsService, TamcUser}
import errors._
import forms.CurrentYearForm.currentYearForm
import forms.DateOfMarriageForm.dateOfMarriageForm
import forms.EarlierYearForm.earlierYearsForm
import forms.EmailForm.emailForm
import forms.EmptyForm
import forms.RecipientDetailsForm.recipientDetailsForm
import models._
import play.Logger
import play.api.data.FormError
import play.api.mvc._
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.play.config.RunMode
import utils.TamcBreadcrumb

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

object TransferController extends TransferController with RunMode {
  override lazy val registrationService = TransferService
  override lazy val maAuthRegime = MarriageAllowanceRegime
  override val authConnector = ApplicationAuthConnector
  override val citizenDetailsService = CitizenDetailsService
  override val ivUpliftUrl = ApplicationConfig.ivUpliftUrl
  override val timeService = TimeService
}

trait TransferController extends BaseController with AuthorisedActions with TamcBreadcrumb with JourneyEnforcers {

  val registrationService: TransferService
  val authConnector: ApplicationAuthConnector
  val timeService: TimeService

  def transfer: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getEligibleTransferorName map {
            name => {
              Ok(views.html.transfer(recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = utils.getUserNino(auth)), name))
            }
          } recover handleError
  }

  def transferAction: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = utils.getUserNino(auth)).bindFromRequest.fold(
            formWithErrors =>
              registrationService.getEligibleTransferorName map {
                name => {
                  BadRequest(views.html.transfer(formWithErrors, name))
                }
              },
            recipientData => {
              CachingService.saveRecipientDetails(recipientData)
              registrationService.getEligibleTransferorName map {
                name => Redirect(controllers.routes.TransferController.dateOfMarriage())
              }
            }) recover handleError
  }

  def dateOfMarriage: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getEligibleTransferorName map {
            name => Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate), name))
          } recover handleError
  }

  def dateOfMarriageAction: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
            formWithErrors =>
              registrationService.getEligibleTransferorName map {
                name => {
                  BadRequest(views.html.date_of_marriage(formWithErrors, name))
                }
              },
            marriageData => {
              CachingService.saveDateOfMarriage(marriageData)
              registrationService.getRecipientDetailsFormData flatMap {
                case RecipientDetailsFormInput(name, lastName, gender, nino) => {
                  val dataToSend = new RegistrationFormInput(name, lastName, gender, nino, marriageData.dateOfMarriage)
                  registrationService.isRecipientEligible(utils.getUserNino(auth), dataToSend) flatMap {
                    _ => Future.successful(Redirect(controllers.routes.TransferController.eligibleYears()))
                  }
                }
              }
            }) recover handleError
  }

  def eligibleYears: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.deleteSelectionAndGetCurrentAndExtraYearEligibility map {
            case (false, extraYears, recipient) if extraYears.isEmpty => throw new NoTaxYearsAvailable
            case (false, extraYears, recipient) if extraYears.nonEmpty => Ok(views.html.previous_years(recipient.data, extraYears, false))
            case (_, extraYears, recipient) =>
              Ok(views.html.eligible_years(currentYearForm(extraYears.nonEmpty), extraYears.nonEmpty, recipient.data.name,
                Some(recipient.data.dateOfMarriage), Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
          } recover handleError
  }

  def eligibleYearsAction: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getCurrentAndExtraYearEligibility flatMap {
            case (currentYearAvailable, extraYears, recipient) =>
              currentYearForm(extraYears.nonEmpty).bindFromRequest.fold(
                hasErrors =>
                  Future {
                    BadRequest(views.html.eligible_years(
                      hasErrors,
                      extraYears.nonEmpty,
                      recipient.data.name,
                      Some(recipient.data.dateOfMarriage),
                      Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
                  },
                success => {
                  registrationService.saveSelectedYears(recipient, if (success.applyForCurrentYear == Some(true)) {
                    List[Int](timeService.getCurrentTaxYear)
                  } else {
                    List[Int]()
                  }) map {
                    _ =>
                      if (extraYears.isEmpty && currentYearAvailable && (success.applyForCurrentYear != Some(true))) {
                        throw new NoTaxYearsSelected
                      } else if (extraYears.nonEmpty) {
                        Ok(views.html.previous_years(recipient.data, extraYears, currentYearAvailable))
                      } else {
                        Redirect(controllers.routes.TransferController.confirmYourEmail())
                      }
                  }
                })
          } recover handleError
  }

  def previousYears: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getCurrentAndExtraYearEligibility flatMap {
            case (currentYearAvailable, extraYears, recipient) =>
              Future.successful(Ok(views.html.single_year_select(earlierYearsForm(), recipient.data, extraYears)))
          } recover handleError
  }

  def extraYearsAction: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getCurrentAndExtraYearEligibility flatMap {
            case (_, extraYears, recipient) =>
              earlierYearsForm(extraYears.map(_.year)).bindFromRequest.fold(
                hasErrors =>
                  Future {
                    BadRequest(views.html.single_year_select(hasErrors.copy(errors = Seq(FormError("selectedYear", List("generic.select.answer"), List()))), recipient.data, extraYears))
                  },
                taxYears => {
                  registrationService.updateSelectedYears(recipient, taxYears.selectedYear, taxYears.yearAvailableForSelection).map {
                    _ =>
                      if (taxYears.furtherYears.isEmpty) {
                        Redirect(controllers.routes.TransferController.confirmYourEmail())
                      } else {
                        Ok(views.html.single_year_select(earlierYearsForm(), recipient.data, toTaxYears(taxYears.furtherYears)))
                      }
                  }
                })
          } recover handleError
  }

  def toTaxYears(years: List[Int]): List[TaxYear] = {
    years.map(year => TaxYear(year, None))
  }

  def confirmYourEmail: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getTransferorNotification map {
            case Some(NotificationRecord(transferorEmail)) => Ok(views.html.email(emailForm.fill(transferorEmail)))
            case None => Ok(views.html.email(emailForm))
          } recover handleError
  }

  def confirmYourEmailAction: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          emailForm.bindFromRequest.fold(
            formWithErrors =>
              Future.successful(BadRequest(views.html.email(formWithErrors))),
            transferorEmail =>
              registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map {
                _ => Redirect(controllers.routes.TransferController.confirm())
              }) recover handleError
  }

  def confirm: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getConfirmationData map {
            case data =>
              Ok(views.html.confirm(data = data, emptyForm = EmptyForm.form))
          } recover handleError
  }

  def confirmAction: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          EmptyForm.form.bindFromRequest().fold(
            formWithErrors =>
              Logger.error(s"unexpected error in emty form, SID [${utils.getSid(request)}]"),
            success =>
              success)
          Logger.info("registration service.createRelationship - confirm action.")
          registrationService.createRelationship(utils.getUserNino(auth), getJourneyName(), request2lang(request)) map {
            _ => Redirect(controllers.routes.TransferController.finished())
          } recover handleError
  }

  def finished: Action[AnyContent] = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getFinishedData(utils.getUserNino(auth)) map {
            case NotificationRecord(email) =>
              if (isPtaJourney)
                Ok(views.html.pta.finished(transferorEmail = email))
              else
                Ok(views.html.finished(transferorEmail = email))
          } recover handleError
  }

  def handleError(implicit hc: HeaderCarrier, ec: ExecutionContext, user: TamcUser, request: Request[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>
        val message: String = s"An exception occurred during processing of URI [${request.uri}] reason [$throwable,${throwable.getMessage}] SID [${utils.getSid(request)}]"

        def handle(message: String, logger: String => Unit, result: Result): Result = {
          logger(message)
          result
        }

        throwable match {
          case _: TransferorNotFound => handle(message, Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: RecipientNotFound => handle(message, Logger.warn, InternalServerError(views.html.errors.recipient_not_found()))
          case _: TransferorDeceased => handle(message, Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: CacheMissingTransferor => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheTransferorInRelationship => handle(message, Logger.warn, Ok(views.html.transferor_status()))
          case _: CacheMissingRecipient => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheRecipientInRelationship => handle(message, Logger.warn, InternalServerError(views.html.errors.recipient_relationship_exists()))
          case _: CacheMissingEmail => handle(message, Logger.warn, Redirect(controllers.routes.TransferController.confirmYourEmail()))
          case _: CannotCreateRelationship => handle(message, Logger.warn, InternalServerError(views.html.errors.relationship_cannot_create()))
          case _: CacheRelationshipAlreadyCreated => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheCreateRequestNotSent => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: NoTaxYearsSelected => handle(message, Logger.info, Ok(views.html.errors.no_year_selected()))
          case _: NoTaxYearsAvailable => handle(message, Logger.info, Ok(views.html.errors.no_eligible_years()))
          case _: NoTaxYearsForTransferor => handle(message, Logger.info, InternalServerError(views.html.errors.no_tax_year_transferor()))
          case _: RelationshipMightBeCreated => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _ => handle(message, Logger.error, InternalServerError(views.html.errors.try_later()))
        }
    }
}
