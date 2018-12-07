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

import config.ApplicationConfig.{TAMC_JOURNEY_GDS, TAMC_JOURNEY_PTA}
import config.TamcContext
import errors.{NoTaxYearsAvailable, NoTaxYearsSelected}
import forms.DateOfMarriageForm.dateOfMarriageForm
import forms.RecipientDetailsForm.recipientDetailsForm
import forms.ChangeRelationshipForm.changeRelationshipForm
import forms.CurrentYearForm.currentYearForm
import forms.EarlierYearForm.earlierYearsForm
import forms.EmailForm.emailForm
import forms.EmptyForm
import javax.inject.Inject
import models._
import play.Logger
import play.api.data.FormError
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, ActionBuilder, AnyContent}
import services.{CachingService, TimeService, TransferService, UpdateRelationshipService}
import utils.TamcBreadcrumb

import scala.concurrent.Future

class NewTransferController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       authenticatedActionRefiner: AuthenticatedActionRefiner,
                                       personalDetailsCacheAction: PersonalDetailsCacheAction,
                                       registrationService: TransferService,
                                       timeService: TimeService,
                                       updateRelationshipService: UpdateRelationshipService
                                     )(implicit tamcContext: TamcContext) extends BaseController with I18nSupport with TamcBreadcrumb {

  private val transferControllerAction: ActionBuilder[AuthenticatedUserRequest] = authenticatedActionRefiner andThen personalDetailsCacheAction

  // TODO history needs to be moved to a new Controller along with the rest of UpdateRelationshipController
  def history(): Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      updateRelationshipService.listRelationship(request.nino) map {
        case (RelationshipRecordList(activeRelationship, historicRelationships, loggedInUserInfo, activeRecord, historicRecord, historicActiveRecord), canApplyPreviousYears) => {
          if (!activeRecord && !historicRecord) {
            if (!request.isLoggedIn) {
              Redirect(controllers.routes.NewTransferController.transfer())
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
      }
  }
  ///////

  def transfer: Action[AnyContent] = transferControllerAction {
    implicit request =>
      Ok(views.html.multiyear.transfer.transfer(recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino), Some(request.name)))
  }

  def transferAction: Action[AnyContent] = (authenticatedActionRefiner andThen personalDetailsCacheAction) {
    implicit request =>
      recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino).bindFromRequest.fold(
        formWithErrors =>
          BadRequest(views.html.multiyear.transfer.transfer(formWithErrors, Some(request.name))),
        recipientData => {
          CachingService.saveRecipientDetails(recipientData)
          Redirect(controllers.routes.NewTransferController.dateOfMarriage())
        })
  }


  def dateOfMarriage: Action[AnyContent] = transferControllerAction {
    implicit request =>
      Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate), Some(request.name)))
  }

  def dateOfMarriageAction: Action[AnyContent] = (authenticatedActionRefiner andThen personalDetailsCacheAction).async {
    implicit request =>
      dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
        formWithErrors => Future.successful(BadRequest(views.html.date_of_marriage(formWithErrors, Some(request.name))))
        ,
        marriageData => {
          CachingService.saveDateOfMarriage(marriageData)


          registrationService.getRecipientDetailsFormData flatMap {
            case RecipientDetailsFormInput(name, lastName, gender, nino) => {
              val dataToSend = new RegistrationFormInput(name, lastName, gender, nino, marriageData.dateOfMarriage)
              registrationService.isRecipientEligible(request.nino, dataToSend) map {
                _ => Redirect(controllers.routes.NewTransferController.eligibleYears())
              }
            }
          }
        })
  }

  def eligibleYears: Action[AnyContent] = transferControllerAction.async {
    implicit request =>
      registrationService.deleteSelectionAndGetCurrentAndExtraYearEligibility map {
        case (false, Nil, _) => throw new NoTaxYearsAvailable
        case (false, extraYears, recipient) if extraYears.nonEmpty => Ok(views.html.multiyear.transfer.previous_years(recipient.data, extraYears, false))
        case (_, extraYears, recipient) =>
          Ok(views.html.multiyear.transfer.eligible_years(currentYearForm(extraYears.nonEmpty), extraYears.nonEmpty, recipient.data.name,
            Some(recipient.data.dateOfMarriage), Some(timeService.getStartDateForTaxYear(timeService.getCurrentTaxYear))))
      }
  }

  def eligibleYearsAction: Action[AnyContent] = transferControllerAction.async {
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
                  Redirect(controllers.routes.NewTransferController.confirmYourEmail())
                }
              }
            })
      }
  }

  def previousYears: Action[AnyContent] = transferControllerAction.async {
    implicit request =>
      registrationService.getCurrentAndExtraYearEligibility.map {
        case (currentYearAvailable, extraYears, recipient) =>
          Ok(views.html.multiyear.transfer.single_year_select(earlierYearsForm(), recipient.data, extraYears))
      }
  }

  def extraYearsAction: Action[AnyContent] = transferControllerAction.async {
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
                    Redirect(controllers.routes.NewTransferController.confirmYourEmail())
                  } else {
                    Ok(views.html.multiyear.transfer.single_year_select(earlierYearsForm(), recipient.data, toTaxYears(taxYears.furtherYears)))
                  }
              }
            })
      }
  }

  //TODO registrationService is being a pain
  def confirmYourEmail: Action[AnyContent] = transferControllerAction.async {
    implicit request =>
      registrationService.getTransferorNotification map {
        case Some(NotificationRecord(transferorEmail)) => Ok(views.html.multiyear.transfer.email(emailForm.fill(transferorEmail)))
        case None => Ok(views.html.multiyear.transfer.email(emailForm))
      }
  }

  //Success(Some(CacheData(None,Some(RecipientRecord(UserRecord(999059794,2015,None,None),RegistrationFormInput(Claire,Forester,Gender(F),NY059794B,2011-02-22),List(TaxYear(2018,Some(true)), TaxYear(2017,None), TaxYear(2016,None), TaxYear(2015,None)))),Some(NotificationRecord(sample@sample.com)),None,Some(List(2018, 2017, 2015)),Some(RecipientDetailsFormInput(Claire,Forester,Gender(F),NY059794B)),Some(DateOfMarriageFormInput(2011-02-22)))))


  def confirmYourEmailAction: Action[AnyContent] = transferControllerAction.async {
    implicit request =>
      emailForm.bindFromRequest.fold(
        formWithErrors =>
          Future.successful(BadRequest(views.html.multiyear.transfer.email(formWithErrors))),
        transferorEmail =>
          registrationService.upsertTransferorNotification(NotificationRecord(transferorEmail)) map {
            _ => Redirect(controllers.routes.NewTransferController.confirm())
          })
  }

  def confirm: Action[AnyContent] = transferControllerAction.async {
    implicit request =>
      registrationService.getConfirmationData map {
        data =>
          Ok(views.html.confirm(data = data, emptyForm = EmptyForm.form))
      }
  }

  def confirmAction: Action[AnyContent] = transferControllerAction.async {
    implicit request =>

      def getJourneyName(): String =
        if (request.isLoggedIn) TAMC_JOURNEY_PTA else TAMC_JOURNEY_GDS

      EmptyForm.form.bindFromRequest().fold(
        formWithErrors =>
          Logger.error(s"unexpected error in emty form, SID [${utils.getSid(request)}]"),
        success =>
          success)
      Logger.info("registration service.createRelationship - confirm action.")
      registrationService.createRelationship(request.nino, getJourneyName()) map {
        _ => Redirect(controllers.routes.NewTransferController.finished())
      }
  }

  def finished: Action[AnyContent] = transferControllerAction.async {
    implicit request =>
      registrationService.getFinishedData(request.nino) map {
        case NotificationRecord(email) =>
          if (request.isLoggedIn)
            Ok(views.html.finished(transferorEmail = email))
          else
            Ok(views.html.finished(transferorEmail = email))
      }
  }

}