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

package controllers.updateRelationship

import com.google.inject.Inject
import controllers.BaseController
import controllers.actions.AuthenticatedActionRefiner
import models.RelationshipRecordList
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.{CachingService, ListRelationshipService, TimeService, TransferService, UpdateRelationshipService}
import uk.gov.hmrc.time
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import forms.ChangeRelationshipForm.changeRelationshipForm
import play.Logger
import controllers.updateRelationship.HandleErrors.handleError

class HistoryController @Inject()(
                                  override val messagesApi: MessagesApi,
                                  authenticate: AuthenticatedActionRefiner,
                                  updateRelationshipService: UpdateRelationshipService,
                                  listRelationshipService: ListRelationshipService,
                                  registrationService: TransferService,
                                  cachingService: CachingService,
                                  timeService: TimeService
                                )(implicit templateRenderer: TemplateRenderer,
                                  formPartialRetriever: FormPartialRetriever) extends BaseController {

  def onPageLoad(): Action[AnyContent] = authenticate.async {
    implicit request =>
      listRelationshipService.listRelationship(request.nino) map {
        case (
          RelationshipRecordList(
            activeRelationship, historicRelationships, loggedInUserInfo,
            activeRecord, historicRecord, historicActiveRecord
          ), canApplyPreviousYears) =>
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
              endOfYear = Some(time.TaxYear.current.finishes)))
          }
      } recover handleError
  }

  def onPageLoadWithCy: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(routes.HistoryController.onPageLoad()).withLang(Lang("cy"))
        .flashing(LanguageUtils.FlashWithSwitchIndicator)
  }

  def onPageLoadWithEn: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(routes.HistoryController.onPageLoad()).withLang(Lang("en"))
        .flashing(LanguageUtils.FlashWithSwitchIndicator)
  }

  def onSubmit(): Action[AnyContent] = authenticate {
    implicit request =>
      changeRelationshipForm.bindFromRequest.fold(
        formWithErrors => {
          Logger.warn("unexpected error in makeChange()")
          Redirect(routes.HistoryController.onPageLoad())
        },
        formData => {
          Ok(views.html.coc.reason_for_change(changeRelationshipForm.fill(formData)))
        }
      )
  }
}