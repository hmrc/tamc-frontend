/*
 * Copyright 2016 HM Revenue & Customs
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

import org.joda.time.LocalDate
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import actions.AuthorisedActions
import actions.MarriageAllowanceRegime
import connectors.ApplicationAuthConnector
import errors.CacheMissingEmail
import errors.CacheMissingRecipient
import errors.CacheMissingTransferor
import errors.CannotCreateRelationship
import errors.RecipientNotFound
import errors.TransferorNotFound
import forms.EmailForm.emailForm
import forms.RegistrationForm.registrationForm
import forms.RecipientDetailsForm.recipientDetailsForm
import forms.DateOfMarriageForm.dateOfMarriageForm
import models._
import play.Logger
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import services.TransferService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.controller.FrontendController
import config.ApplicationConfig
import play.api.mvc.Cookie
import utils.TamcBreadcrumb
import errors.CacheTransferorInRelationship
import errors.CacheRecipientInRelationship
import errors.CacheRelationshipAlreadyCreated
import errors.CacheCreateRequestNotSent
import play.api.data.Form
import forms.EmptyForm
import actions.JourneyEnforcers
import uk.gov.hmrc.play.http.SessionKeys
import errors.TransferorDeceased
import details.CitizenDetailsService
import details.TamcUser
import scala.concurrent.ExecutionContext.Implicits.global
import forms.MultiYearForm._
import services.TimeService
import services.CachingService
import forms.CurrentYearForm.currentYearForm
import errors.NoTaxYearsSelected
import errors.NoTaxYearsAvailable
import errors.NoTaxYearsAvailable
import errors.NoTaxYearsForTransferor

object TransferController extends TransferController with RunMode {
  override lazy val registrationService = TransferService
  override lazy val maAuthRegime = MarriageAllowanceRegime
  override val authConnector = ApplicationAuthConnector
  override val citizenDetailsService = CitizenDetailsService
  override val ivUpliftUrl = ApplicationConfig.ivUpliftUrl
  override val timeService = TimeService
}

trait TransferController extends FrontendController with AuthorisedActions with TamcBreadcrumb with JourneyEnforcers {

  val registrationService: TransferService
  val authConnector: ApplicationAuthConnector
  val timeService: TimeService

  def transfer = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getEligibleTransferorName map {
            name => { Ok(views.html.transfer(recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = utils.getUserNino(auth)), name)) }
          } recover (handleError)
  }

  def transferAction = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = utils.getUserNino(auth)).bindFromRequest.fold(
            formWithErrors =>
              registrationService.getEligibleTransferorName map {
                name => { BadRequest(views.html.transfer(formWithErrors, name)) }
              },
            recipientData => {
              CachingService.saveRecipientDetails(recipientData)
              registrationService.getEligibleTransferorName map {
                name => Redirect(controllers.routes.TransferController.dateOfMarriage())
              }}) recover (handleError)
  }

  def dateOfMarriage = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getEligibleTransferorName map {
            name => Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate), name))
          } recover (handleError)
  }

  def dateOfMarriageAction = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
            formWithErrors =>
              registrationService.getEligibleTransferorName map {
                name => { BadRequest(views.html.date_of_marriage(formWithErrors, name)) }
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
            }) recover (handleError)
  }

  def eligibleYears = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.deleteSelectionAndGetCurrentAndExtraYearEligibility map {
            case (false, extraYears, recipient) if (extraYears.isEmpty) =>
              throw new NoTaxYearsAvailable
            case (false, extraYears, recipient) if (!extraYears.isEmpty) =>
              Ok(views.html.previous_years(recipient.data, extraYears, false))
            case (currentYearAvailable, extraYears, recipient) =>
              Ok(views.html.eligible_years(
                currentYearForm(!extraYears.isEmpty),
                !extraYears.isEmpty,
                recipient.data.name,
                Some(recipient.data.dateOfMarriage),
                Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
          } recover (handleError)
  }

  def eligibleYearsAction = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getCurrentAndExtraYearEligibility flatMap {
            case (currentYearAvailable, extraYears, recipient) =>
              currentYearForm(!extraYears.isEmpty).bindFromRequest.fold(
                hasErrors =>
                  Future {
                    BadRequest(views.html.eligible_years(
                      hasErrors,
                      !extraYears.isEmpty,
                      recipient.data.name,
                      Some(recipient.data.dateOfMarriage),
                      Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
                  },
                success => {
                  registrationService.saveSelectedYears(recipient, if (success.applyForCurrentYear == Some(true)) { List[Int](timeService.getCurrentTaxYear) } else { List[Int]() }) map {
                    _ =>
                      if (extraYears.isEmpty && currentYearAvailable && (success.applyForCurrentYear != Some(true))) {
                        throw new NoTaxYearsSelected
                      } else if (!extraYears.isEmpty) {
                        Ok(views.html.previous_years(recipient.data, extraYears, currentYearAvailable))
                      } else {
                        Redirect(controllers.routes.TransferController.confirmYourEmail())
                      }
                  }
                })
          } recover (handleError)
  }

  def previousYears =  TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getCurrentAndExtraYearEligibility flatMap {
            case (currentYearAvailable, extraYears, recipient) =>
                 Future.successful(Ok(views.html.multi_year_select(multiYearForm(), recipient.data, extraYears)))
          }recover (handleError)
  }

  def extraYearsAction = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getCurrentAndExtraYearEligibility flatMap {
            case (_, extraYears, recipient) =>
              multiYearForm(extraYears.map(_.year)).bindFromRequest.fold(
                hasErrors =>
                  Future {
                    BadRequest(views.html.multi_year_select(hasErrors, recipient.data, extraYears))
                  },
                selectedTaxYears => {
                  registrationService.updateSelectedYears(recipient, selectedTaxYears.years.filter(_ != 0).map(_.toInt)).map {
                    _ => Redirect(controllers.routes.TransferController.confirmYourEmail())
                  }
                })
          } recover (handleError)
  }

  def confirmYourEmail = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getTransferorNotification map {
            case Some(NotificationRecord(transferorEmail)) => Ok(views.html.email(emailForm.fill(transferorEmail)))
            case None                                      => Ok(views.html.email(emailForm))
          } recover (handleError)
  }

  def confirmYourEmailAction = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          emailForm.bindFromRequest.fold(
            formWithErrors =>
              Future.successful(BadRequest(views.html.email(formWithErrors))),
            transferorEmail =>
              registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map {
                _ => Redirect(controllers.routes.TransferController.confirm())
              }) recover (handleError)
  }

  def confirm = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getConfirmationData map {
            case data =>
              Ok(views.html.confirm(data = data, emptyForm = EmptyForm.form))
          } recover (handleError)
  }

  def confirmAction = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          EmptyForm.form.bindFromRequest().fold(
            formWithErrors =>
              Logger.error(s"unexpected error in emty form, SID [${utils.getSid(request)}]"),
            success =>
              success)
          registrationService.createRelationship(utils.getUserNino(auth), getJourneyName(), request2lang(request)) map {
            _ => Redirect(controllers.routes.TransferController.finished())
          } recover (handleError)
  }

  def finished = TamcAuthPersonalDetailsAction {
    implicit auth =>
      implicit request =>
        implicit details =>
          registrationService.getFinishedData(utils.getUserNino(auth)) map {
            case NotificationRecord(email) =>
              if (isPtaJourney)
                Ok(views.html.pta.finished(transferorEmail = email))
              else
                Ok(views.html.finished(transferorEmail = email))
          } recover (handleError)
  }

  def handleError(implicit hc: HeaderCarrier,
                  ec: ExecutionContext,
                  user: TamcUser,
                  request: Request[_]): PartialFunction[Throwable, Result] =
    PartialFunction[Throwable, Result] {
      throwable: Throwable =>

        val message: String = s"An exception occurred during processing of URI [${request.uri}] reason [${throwable},${throwable.getMessage}] SID [${utils.getSid(request)}]"

        def handle(message: String, logger: String => Unit, result: Result): Result = {
          logger(message)
          result
        }

        throwable match {
          case _: TransferorNotFound              => handle(message, Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: RecipientNotFound               => handle(message, Logger.warn, InternalServerError(views.html.errors.recipient_not_found()))
          case _: TransferorDeceased              => handle(message, Logger.warn, InternalServerError(views.html.errors.transferor_not_found()))
          case _: CacheMissingTransferor          => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheTransferorInRelationship   => handle(message, Logger.warn, Ok(views.html.transferor_status()))
          case _: CacheMissingRecipient           => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheRecipientInRelationship    => handle(message, Logger.warn, InternalServerError(views.html.errors.recipient_relationship_exists()))
          case _: CacheMissingEmail               => handle(message, Logger.warn, Redirect(controllers.routes.TransferController.confirmYourEmail()))
          case _: CannotCreateRelationship        => handle(message, Logger.warn, InternalServerError(views.html.errors.relationship_cannot_create()))
          case _: CacheRelationshipAlreadyCreated => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: CacheCreateRequestNotSent       => handle(message, Logger.warn, Redirect(controllers.routes.UpdateRelationshipController.history()))
          case _: NoTaxYearsSelected              => handle(message, Logger.info, Ok(views.html.errors.no_year_selected()))
          case _: NoTaxYearsAvailable             => handle(message, Logger.info, Ok(views.html.errors.no_eligible_years()))
          case _: NoTaxYearsForTransferor         => handle(message, Logger.info, InternalServerError(views.html.errors.no_tax_year_transferor()))
          case _                                  => handle(message, Logger.error, InternalServerError(views.html.errors.try_later()))
        }
    }
}
