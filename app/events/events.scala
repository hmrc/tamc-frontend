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

package events

import models.{CacheData, UpdateRelationshipRequestHolder}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.model.DataEvent

object CreateRelationshipSuccessEvent {
  def apply(cacheData: CacheData)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_SUCCESSFUL,
      Map(
        "event" -> "create-relationship-pta",
        "data" -> cacheData.toString
      )
    )
}

object UpdateRelationshipSuccessEvent {
  def apply(updateData: UpdateRelationshipRequestHolder)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_SUCCESSFUL,
      Map(
        "event" -> "update-relationship",
        "data" -> updateData.toString
      )
    )
}

object CreateRelationshipFailureEvent {
  def apply(cacheData: CacheData, error: Throwable)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> ("create-relationship-pta"),
        "error" -> error.toString,
        "data" -> cacheData.toString
      )
    )
}

object RelationshipAlreadyCreatedEvent {
  def apply(cacheData: CacheData)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "relationship-exists",
        "data" -> cacheData.toString
      )
    )
}

object UpdateRelationshipFailureEvent {
  def apply(cacheData: UpdateRelationshipRequestHolder, error: Throwable)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "update-relationship",
        "error" -> error.toString,
        "data" -> cacheData.toString
      )
    )
}

object CreateRelationshipCacheFailureEvent {
  def apply(error: Throwable)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "create-relationship",
        "error" -> error.toString
      )
    )
}

object UpdateRelationshipCacheFailureEvent {
  def apply(error: Throwable)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "update-relationship",
        "error" -> error.toString
      )
    )
}

object RecipientFailureEvent {
  def apply(nino: Nino, error: Throwable)(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_FAILED,
      Map(
        "event" -> "recipient-error",
        "error" -> error.toString,
        "data" -> nino.value
      )
    )
}

object RiskTriageRedirectEvent {
  def apply()(implicit hc: HeaderCarrier): BusinessEvent =
    new BusinessEvent(
      AuditType.Tx_SUCCESSFUL,
      Map(
        "event" -> "authorisation-attempt",
        "data" -> "TRIAGE"
      )
    )
}

private object AuditType {
  val Tx_FAILED = "TxFailed"
  val Tx_SUCCESSFUL = "TxSuccessful" //TxSuccessful should be TxSucceeded but any changes would impact previous reports
}

class BusinessEvent(auditType: String, detail: Map[String, String])(implicit hc: HeaderCarrier)
  extends DataEvent(
    auditSource = "tamc-frontend",
    auditType = auditType,
    detail = detail,
    tags = (hc.headers(HeaderNames.explicitlyIncludedHeaders) ++ hc.extraHeaders ++ hc.otherHeaders).toMap
  )
