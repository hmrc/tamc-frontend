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

package errors

import models.ResponseStatus
import play.api.libs.json.{Json, OFormat}

case class MarriageAllowanceError(status: ResponseStatus)
object MarriageAllowanceError {
  implicit val format: OFormat[MarriageAllowanceError] = Json.format[MarriageAllowanceError]
}

case class DesEnumerationNotFound() extends ServiceError
case class UnknownParticipant() extends ServiceError
case class TransferorNotFound() extends ServiceError
case class RecipientNotFound() extends ServiceError
case class CannotCreateRelationship() extends ServiceError
case class CacheMissingTransferor() extends ServiceError
case class CacheMapNoFound() extends ServiceError
case class CacheRelationshipAlreadyCreated() extends ServiceError
case class CacheTransferorInRelationship() extends ServiceError
case class CacheMissingRecipient() extends ServiceError
case class CacheMissingDivorceDate() extends ServiceError
case class CacheMissingRelationshipRecords() extends ServiceError
case class CacheRecipientInRelationship() extends ServiceError
case class CacheMissingEmail() extends ServiceError
case class CacheMissingEndReason() extends ServiceError
case class CacheMissingMAEndingDates() extends ServiceError
case class CacheCreateRequestNotSent() extends ServiceError
case class TransferorDeceased() extends ServiceError
case class RecipientDeceased() extends ServiceError
case class NoTaxYearsSelected() extends ServiceError
case class NoTaxYearsAvailable() extends ServiceError
case class NoTaxYearsForTransferor() extends ServiceError
case class CacheRelationshipAlreadyUpdated() extends ServiceError
case class CacheUpdateRequestNotSent() extends ServiceError
case class CannotUpdateRelationship() extends ServiceError
case class CacheMissingUpdateRecord() extends ServiceError
case class CitizenNotFound() extends ServiceError
case class BadFetchRequest() extends ServiceError
case class NoPrimaryRecordError() extends ServiceError
case class MultipleActiveRecordError() extends ServiceError
case class RelationshipMightBeCreated() extends ServiceError
case class OtherError(error: MarriageAllowanceError) extends ServiceError

sealed abstract class ServiceError extends RuntimeException

object ErrorResponseStatus {

  val CITIZEN_NOT_FOUND = "TAMC:ERROR:CITIZEN-NOT-FOUND"
  val BAD_REQUEST = "TAMC:ERROR:BAD-REQUEST"
  val SERVER_ERROR = "ERROR:500"
  val SERVICE_UNAVILABLE = "ERROR:503"
  val CANNOT_CREATE_RELATIONSHIP = "TAMC:ERROR:CANNOT-CREATE-RELATIONSHIP"
  val CANNOT_UPDATE_RELATIONSHIP = "TAMC:ERROR:CANNOT-UPDATE-RELATIONSHIP"
  val TRANSFEROR_NOT_FOUND = "TAMC:ERROR:TRANSFEROR-NOT-FOUND"
  val RELATION_MIGHT_BE_CREATED = "TAMC:ERROR:RELATION-MIGHT-BE-CREATED"
  val RECIPIENT_DECEASED = "TAMC:ERROR:RECIPIENT-DECEASED"
  val OTHER_ERROR = "TAMC:ERROR:OTHER-ERROR"
  val RECIPIENT_NOT_FOUND = "TAMC:ERROR:RECIPIENT-NOT-FOUND"
  val TRANSFEROR_DECEASED = "TAMC:ERROR:TRANSFERER-DECEASED"
}
