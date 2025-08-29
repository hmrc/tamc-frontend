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
import services.CacheService.CACHE_CHOOSE_YEARS
import services.CachingService
import utils.LoggerHelper

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ApplyByPostController @Inject()(
                                       authenticate: StandardAuthJourney,
                                       cachingService: CachingService,
                                       applyByPostView: views.html.multiyear.transfer.apply_by_post,
                                       cc: MessagesControllerComponents
                                     )
                                     (implicit ec: ExecutionContext) extends BaseController(cc) with LoggerHelper {

  def applyByPost: Action[AnyContent] = authenticate.pertaxAuthActionWithUserDetails.async { implicit request =>
    cachingService
      .get[String](CACHE_CHOOSE_YEARS)
      .map {
        case Some(yearOptions) => yearOptions.split(",").toSeq
        case None              => Seq.empty
      }
      .map { selectedTaxYears =>
        Ok(applyByPostView(selectedTaxYears))
      }
  }
}
