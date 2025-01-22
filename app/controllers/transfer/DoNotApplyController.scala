/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.auth.StandardAuthJourney
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.LoggerHelper

import javax.inject.Inject

class DoNotApplyController @Inject()(
                                       authenticate: StandardAuthJourney,
                                       doNotApplyView: views.html.multiyear.transfer.dont_apply_current_tax_year,
                                       cc: MessagesControllerComponents
                                     )
                                     extends BaseController(cc) with LoggerHelper {


  def doNotApply(): Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails {
    implicit request =>
      Ok(doNotApplyView())
  }
}