/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import com.google.inject.Inject
import config.ApplicationConfig
import play.api.libs.json.Json
import errors.ErrorResponseStatus.{BAD_REQUEST, CITIZEN_NOT_FOUND, TRANSFEROR_NOT_FOUND}
import errors.{BadFetchRequest, CitizenNotFound, TransferorNotFound}
import models._
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.HttpReadsInstances.readEitherOf

import scala.concurrent.{ExecutionContext, Future}

class MarriageAllowanceConnector @Inject()(httpClient: HttpClient, applicationConfig: ApplicationConfig) {

  def marriageAllowanceUrl = applicationConfig.marriageAllowanceUrl

  def listRelationship(nino: Nino)(
    implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecordList] =
    httpClient
      .GET[Either[UpstreamErrorResponse, HttpResponse]](
        s"$marriageAllowanceUrl/paye/$nino/list-relationship"
      )
      .flatMap {
        case Right(response) =>
          val relationshipRecordWrapper = response.json.as[RelationshipRecordStatusWrapper]
          relationshipRecordWrapper match {
            case RelationshipRecordStatusWrapper(relationshipRecordWrapper, ResponseStatus("OK")) =>
              Future.successful(relationshipRecordWrapper)
            case RelationshipRecordStatusWrapper(_, ResponseStatus(TRANSFEROR_NOT_FOUND)) =>
              Future.failed(TransferorNotFound())
            case RelationshipRecordStatusWrapper(_, ResponseStatus(CITIZEN_NOT_FOUND)) =>
              Future.failed(CitizenNotFound())
            case RelationshipRecordStatusWrapper(_, ResponseStatus(BAD_REQUEST)) =>
              Future.failed(BadFetchRequest())
            case _ =>
              Future.failed(
                new UnsupportedOperationException("Unable to handle list relationship request")
              )
          }
        case Left(error) =>
          Future.failed(error)
      }

  def getRecipientRelationship(transferorNino: Nino, recipientData: RegistrationFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[UpstreamErrorResponse, GetRelationshipResponse]] =
    httpClient
      .POST[RegistrationFormInput, Either[UpstreamErrorResponse, HttpResponse]](
        s"$marriageAllowanceUrl/paye/$transferorNino/get-recipient-relationship", body = recipientData
      )
      .map {
        case Right(response) =>
          Right(response.json.as[GetRelationshipResponse])
        case Left(error) =>
          Left(error)
      }

  def createRelationship(transferorNino: Nino, data: CreateRelationshipRequestHolder)(
    implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[UpstreamErrorResponse, Option[CreateRelationshipResponse]]] =
    httpClient
      .PUT[CreateRelationshipRequestHolder, Either[UpstreamErrorResponse, HttpResponse]](
        s"$marriageAllowanceUrl/paye/$transferorNino/create-multi-year-relationship/pta", data
      )
      .map {
        case Right(response) =>
          Right(Json.fromJson[CreateRelationshipResponse](response.json).asOpt)
        case Left(error) => Left(error)
      }

  def updateRelationship(transferorNino: Nino, data: UpdateRelationshipRequestHolder)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] =
    httpClient
      .PUT[UpdateRelationshipRequestHolder, HttpResponse](
        s"$marriageAllowanceUrl/paye/$transferorNino/update-relationship", data
      )
}
