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

package details

import scala.concurrent.Future
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.HttpGet
import uk.gov.hmrc.play.http.NotFoundException
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext.fromLoggingDetails
import utils.WSHttp
import metrics.Metrics
import services.CachingService
import play.api.Logger

sealed trait PersonDetailsResponse
case class PersonDetailsSuccessResponse(personDetails: PersonDetails) extends PersonDetailsResponse
case class PersonDetailsNotFoundResponse() extends PersonDetailsResponse
case class PersonDetailsErrorResponse(cause: Exception) extends PersonDetailsResponse

object CitizenDetailsService extends CitizenDetailsService with uk.gov.hmrc.play.config.ServicesConfig {
  override def httpGet = WSHttp
  override def citizenDetailsUrl = baseUrl("citizen-details")
  override def cachingService = CachingService
}

trait CitizenDetailsService {

  def httpGet: HttpGet
  def citizenDetailsUrl: String
  def cachingService: CachingService

  def getPersonDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] =
    cachingService.getPersonDetails.flatMap {
      case Some(details) => Future { PersonDetailsSuccessResponse(details) }
      case None          => getDetailsAndSave(nino)
    }

  private def getDetailsAndSave(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] =
    getDetailsFromCid(nino).flatMap {
      case PersonDetailsSuccessResponse(pd) =>
        cachingService.savePersonDetails(pd).map {
          pd => PersonDetailsSuccessResponse(pd)
        }
      case otherResponse => Future { otherResponse }
    }

  def getDetailsFromCid(nino: Nino)(implicit hc: HeaderCarrier): Future[PersonDetailsResponse] = {
    val timer = Metrics.citizenDetailStartTimer()
    httpGet.GET[PersonDetails](s"$citizenDetailsUrl/citizen-details/$nino/designatory-details") map {
      personDetails =>
        timer.stop()
        Metrics.incrementSuccessCitizenDetail()
        PersonDetailsSuccessResponse(personDetails)
    } recover {
      case err: NotFoundException =>
        timer.stop()
        Metrics.incrementFailedCitizenDetail()
        Logger.warn("Unable to find personal details record in citizen-details for current user")
        PersonDetailsNotFoundResponse()
      case err: Exception =>
        timer.stop()
        Metrics.incrementFailedCitizenDetail()
        Logger.warn("Error getting personal details record from citizen-details for current user", err)
        PersonDetailsErrorResponse(err)
    }
  }
}
