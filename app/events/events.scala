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

package events

import models.CacheData
import models.CitizenName
import models.UserRecord
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.audit.model.DataEvent
import play.api.mvc.Request
import config.ApplicationConfig
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.mvc.AnyContent
import models.UpdateRelationshipCacheData
import uk.gov.hmrc.http.HeaderCarrier

object CreateRelationshipSuccessEvent {
  def apply(cacheData: CacheData, journey: String)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_SUCCESSFUL,
      Map(
        "event" -> ("create-relationship-"+journey),
        "data" -> cacheData.toString()))
}

object UpdateRelationshipSuccessEvent {
  def apply(cacheData: UpdateRelationshipCacheData)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_SUCCESSFUL,
      Map(
        "event" -> "update-relationship",
        "data" -> cacheData.toString()))
}

object CreateRelationshipFailureEvent {
  def apply(cacheData: CacheData, journey : String, error: Throwable)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> ("create-relationship-"+journey),
        "error" -> error.toString(),
        "data" -> cacheData.toString()))
}

object RelationshipAlreadyCreatedEvent {
  def apply(cacheData: CacheData)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "relationship-exists",
        "data" -> cacheData.toString()))
}

object UpdateRelationshipFailureEvent {
  def apply(cacheData: UpdateRelationshipCacheData, error: Throwable)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "update-relationship",
        "error" -> error.toString(),
        "data" -> cacheData.toString()))
}
object CreateRelationshipCacheFailureEvent {
  def apply(error: Throwable)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "create-relationship",
        "error" -> error.toString()))
}

object UpdateRelationshipCacheFailureEvent {
  def apply(error: Throwable)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "update-relationship",
        "error" -> error.toString()))
}

/*
object TransferorRelationshipDataInconsistent {
  def apply(transferorRecord: UserRecord, cache: Option[CacheData])(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "transferor-data-inconsistent",
        "error" -> "TransferorRelationshipDataInconsistent",
        "transferor" -> transferorRecord.toString(),
        "cache" -> cache.toString()))
}
*/

object RecipientFailureEvent {
  def apply(nino: Nino, error: Throwable)(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "recipient-error",
        "error" -> error.toString(),
        "data" -> nino.value))
}

object RiskTriageRedirectEvent {
  def apply()(implicit hc: HeaderCarrier) =
    new BusinessEvent(
      AuditType.Tx_SUCCESSFUL,
      Map(
        "event" -> "authorisation-attempt",
        "data" -> "TRIAGE"))
}

private object AuditType {
  val Tx_FAILED = "TxFailed"
  val Tx_SUCCESSFUL = "TxSuccessful" //TxSuccessful should be TxSucceeded but any changes would impact previous reports
}

class BusinessEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier)
  extends DataEvent(auditSource = "tamc-frontend", auditType = auditType, detail = detail, tags = hc.headers.toMap)
