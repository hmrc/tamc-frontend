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

package controllers.transfer

import controllers.BaseController
import controllers.actions.AuthenticatedActionRefiner
import forms.DateOfMarriageForm.dateOfMarriageForm
import javax.inject.Inject
import models._
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc._
import services.{CachingService, TimeService, TransferService}
import uk.gov.hmrc.play.language.LanguageUtils
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer

import scala.concurrent.Future

class DateOfMarriageController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    authenticate: AuthenticatedActionRefiner,
                                    registrationService: TransferService,
                                    cachingService: CachingService,
                                    timeService: TimeService
                                  )(implicit templateRenderer: TemplateRenderer,
                                    formPartialRetriever: FormPartialRetriever) extends BaseController {


  def onPageLoad: Action[AnyContent] = authenticate {
    implicit request =>
      Ok(views.html.date_of_marriage(marriageForm = dateOfMarriageForm(today = timeService.getCurrentDate)))
  }

  def onPageLoadWithCy: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(controllers.transfer.routes.DateOfMarriageController.onPageLoad()).withLang(Lang("cy"))
        .flashing(LanguageUtils.FlashWithSwitchIndicator)
  }

  def onPageLoadWithEn: Action[AnyContent] = authenticate {
    implicit request =>
      Redirect(controllers.transfer.routes.DateOfMarriageController.onPageLoad()).withLang(Lang("en"))
        .flashing(LanguageUtils.FlashWithSwitchIndicator)
  }

  def onSubmit: Action[AnyContent] = authenticate.async {
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
        }) recover HandleError.apply
  }
}