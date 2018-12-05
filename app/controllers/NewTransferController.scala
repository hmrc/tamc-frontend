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

import config.TamcContext
import forms.DateOfMarriageForm.dateOfMarriageForm
import forms.RecipientDetailsForm.recipientDetailsForm
import forms.ChangeRelationshipForm.changeRelationshipForm
import javax.inject.Inject
import models.RelationshipRecordList
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CachingService, TimeService, TransferService, UpdateRelationshipService}
import utils.TamcBreadcrumb

class NewTransferController @Inject() (
                                        override val messagesApi: MessagesApi,
                                        authenticatedActionRefiner: AuthenticatedActionRefiner,
                                        registrationService: TransferService,
                                        timeService: TimeService,
                                        updateRelationshipService: UpdateRelationshipService
                                      )(implicit tamcContext: TamcContext) extends BaseController with I18nSupport with TamcBreadcrumb {

  def history(): Action[AnyContent] = authenticatedActionRefiner.async {
      implicit request =>
          updateRelationshipService.listRelationship(request.nino) map {
            case (RelationshipRecordList(activeRelationship, historicRelationships, loggedInUserInfo, activeRecord, historicRecord, historicActiveRecord), canApplyPreviousYears) => {
              if (!activeRecord && !historicRecord) {
                if (!request.isLoggedIn) {
                  Redirect(controllers.routes.NewTransferController.transfer())
                } else {
                  Redirect(controllers.routes.MultiYearPtaEligibilityController.howItWorks())
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


  def transfer: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      registrationService.getEligibleTransferorName map {
        name => {
          Ok(views.html.transfer(recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino), name))
        }
      }
  }

  def transferAction: Action[AnyContent] = authenticatedActionRefiner.async {
    implicit request =>
      recipientDetailsForm(today = timeService.getCurrentDate, transferorNino = request.nino).bindFromRequest.fold(
        formWithErrors =>
          registrationService.getEligibleTransferorName map {
            name => {
              BadRequest(views.html.transfer(formWithErrors, name))
            }
          },
        recipientData => {
          CachingService.saveRecipientDetails(recipientData)
          registrationService.getEligibleTransferorName map {
            name => Redirect(controllers.routes.NewTransferController.dateOfMarriage())
          }
        })
  }


  def dateOfMarriage: Action[AnyContent] = authenticatedActionRefiner.async {
      implicit request =>
          registrationService.getEligibleTransferorName map {
            name => Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate), name))
          }
  }

  def dateOfMarriageAction: Action[AnyContent] = { ??? }
//    TamcAuthPersonalDetailsAction {
//    implicit auth =>
//      implicit request =>
//        implicit details =>
//          dateOfMarriageForm(today = timeService.getCurrentDate).bindFromRequest.fold(
//            formWithErrors =>
//              registrationService.getEligibleTransferorName map {
//                name => {
//                  BadRequest(views.html.date_of_marriage(formWithErrors, name))
//                }
//              },
//            marriageData => {
//              CachingService.saveDateOfMarriage(marriageData)
//              registrationService.getRecipientDetailsFormData flatMap {
//                case RecipientDetailsFormInput(name, lastName, gender, nino) => {
//                  val dataToSend = new RegistrationFormInput(name, lastName, gender, nino, marriageData.dateOfMarriage)
//                  registrationService.isRecipientEligible(utils.getUserNino(auth), dataToSend) flatMap {
//                    _ => Future.successful(Redirect(controllers.routes.TransferController.eligibleYears()))
//                  }
//                }
//              }
//            })
//  }

}
