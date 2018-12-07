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
import details.{CitizenDetailsService, PersonDetails, PersonDetailsSuccessResponse}
import play.api.mvc._
import services.CachingService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class PersonalDetailsCacheAction @Inject()(
                                            citizenDetailsService: CitizenDetailsService,
                                            cachingService: CachingService
                                          )(implicit ec: ExecutionContext) extends ActionFilter[AuthenticatedUserRequest] {

  override protected def filter[A](request: AuthenticatedUserRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    println("\n\n\n\n hazel's persona deets \n\n\n\n")
    cachingService.getCachedData.onComplete(println)
    println("\n\n\n\n\n")

    //TODO caching Service is not saving Name and notification record

    """
      |Success(
      |Some(
      |CacheData(
      |None, <- no User record
      |Some(RecipientRecord(UserRecord(999059794,2015,None,None),RegistrationFormInput(Claire,Forester,Gender(F),NY059794B,2011-02-22),List(TaxYear(2018,Some(true)), TaxYear(2017,None), TaxYear(2016,None), TaxYear(2015,None)))),
      |None, <- no notificationRecord
      |None,
      |Some(List(2018, 2015)),Some(RecipientDetailsFormInput(Claire,Forester,Gender(F),NY059794B)),Some(DateOfMarriageFormInput(2011-02-22)))))""".stripMargin

    citizenDetailsService.getPersonDetails(request.nino).map {
      case PersonDetailsSuccessResponse(PersonDetails(_)) => None
        //TODO implement
      case _ => ??? //Some(Ok("citizen details did not return successfulResponce"))
    }
  }
}

"""
|Some(
|  CacheData(
|  Some(UserRecord(_, _, _, _)),
|  Some(RecipientRecord(UserRecord(_, _, _, _), _, _)),
|  notificationRecord, _, _, _, _))""".stripMarine