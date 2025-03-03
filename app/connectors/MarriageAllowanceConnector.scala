/*
 * Copyright 2023 HM Revenue & Customs
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
import errors.ErrorResponseStatus.{BAD_REQUEST, CITIZEN_NOT_FOUND, TRANSFEROR_NOT_FOUND}
import errors.{BadFetchRequest, CitizenNotFound, MarriageAllowanceError, TransferorNotFound}
import models._
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class MarriageAllowanceConnector @Inject()(httpClient: HttpClientV2, applicationConfig: ApplicationConfig) {

  def marriageAllowanceUrl = applicationConfig.marriageAllowanceUrl

  def listRelationship(nino: Nino)(
    implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecordList] = {
    httpClient
      .get(url"$marriageAllowanceUrl/paye/$nino/list-relationship")
      .execute[HttpResponse]
      .flatMap {
        response =>
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
      }.recoverWith {
      case t: Throwable => Future.failed(t)
    }
  }

  def getRecipientRelationship(transferorNino: Nino, recipientData: RegistrationFormInput)
                              (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MarriageAllowanceError, GetRelationshipResponse]] = {
    httpClient
      .post(url"$marriageAllowanceUrl/paye/$transferorNino/get-recipient-relationship")
      .withBody(Json.toJson(recipientData))
      .execute[HttpResponse]
      .map {
        case response if response.status == 200 => Right(response.json.as[GetRelationshipResponse])
        case errorResponse => Left(errorResponse.json.as[MarriageAllowanceError])
      }.recoverWith {
      case t: Throwable => Future.failed(t)
    }
  }

  def createRelationship(transferorNino: Nino, data: CreateRelationshipRequestHolder)(
    implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MarriageAllowanceError, Option[CreateRelationshipResponse]]] = {
    httpClient
      .put(url"$marriageAllowanceUrl/paye/$transferorNino/create-multi-year-relationship/pta")
      .withBody(Json.toJson(data))
      .execute[HttpResponse]
      .map {
        case response if response.status == 200 => Right(Json.fromJson[CreateRelationshipResponse](response.json).asOpt)
        case errorResponse => Left(errorResponse.json.as[MarriageAllowanceError])
      }.recoverWith {
      case t: Throwable => Future.failed(t)
    }
  }

  def updateRelationship(transferorNino: Nino, data: UpdateRelationshipRequestHolder)(
    implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[MarriageAllowanceError, Option[UpdateRelationshipResponse]]] = {
    httpClient
      .put(url"$marriageAllowanceUrl/paye/$transferorNino/update-relationship")
      .withBody(Json.toJson(data))
      .execute[HttpResponse]
      .map {
        case response if response.status == 200 => Right(Json.fromJson[UpdateRelationshipResponse](response.json).asOpt)
        case errorResponse => Left(errorResponse.json.as[MarriageAllowanceError])
      }.recoverWith {
      case t: Throwable => Future.failed(t)
    }
  }
}
