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

import com.google.inject.Inject
import details.{CitizenDetailsService, PersonDetailsSuccessResponse}
import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsCacheAction @Inject()(
                                            citizenDetailsService: CitizenDetailsService
                                          )(implicit ec: ExecutionContext) extends ActionFilter[AuthenticatedUserRequest] {

  override protected def filter[A](request: AuthenticatedUserRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    citizenDetailsService.getPersonDetails(request.nino).map {
      case PersonDetailsSuccessResponse(_) => None
        //TODO implement
      case _ => ??? //Some(Ok("citizen details did not return successfulResponce"))
    }
  }
}