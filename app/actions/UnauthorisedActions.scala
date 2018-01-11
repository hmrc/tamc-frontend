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

package actions

import play.api.Logger
import play.api.mvc.{Action, AnyContent, Request, Result}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.play.frontend.controller.UnauthorisedAction

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UnauthorisedActions extends JourneyEnforcers {

  def unauthorisedAction(body: (Request[_] => Result)) = UnauthorisedAction(body)

  def journeyEnforcedAction(body: (Request[_] => Result)): Action[AnyContent] = Action.async {
    implicit request =>
      isGdsOrPtaJourney(request) match {
        case true => unauthorisedAction(body)(request)
        case _ => Future {
          // $COVERAGE-OFF$
          Logger.info("User has not visited eligibility page, redirecting to eligibilityCheck")
          // $COVERAGE-ON
          Redirect(controllers.routes.MultiYearGdsEligibilityController.eligibilityCheck())
        }
      }
  }
}
