/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import play.api.Logging
import play.api.libs.json._

sealed trait DesRelationshipEndReason {
  def value: String
}

object DesRelationshipEndReason extends Logging {

  case object Death extends DesRelationshipEndReason {
    val value = "DEATH"
  }

  case object Divorce extends DesRelationshipEndReason {
    val value = "DIVORCE"
  }

  case object InvalidParticipant extends DesRelationshipEndReason {
    val value = "INVALID_PARTICIPANT"
  }

  case object Cancelled extends DesRelationshipEndReason {
    val value = "CANCELLED"
  }

  case object Rejected extends DesRelationshipEndReason {
    val value = "REJECTED"
  }

  case object Hmrc extends DesRelationshipEndReason {
    val value = "HMRC"
  }

  case object Closed extends DesRelationshipEndReason {
    val value = "CLOSED"
  }

  case object Merger extends DesRelationshipEndReason {
    val value = "MERGER"
  }

  case object Retrospective extends DesRelationshipEndReason {
    val value = "RETROSPECTIVE"
  }

  case object System extends DesRelationshipEndReason {
    val value = "SYSTEM"
  }

  case object Active extends DesRelationshipEndReason {
    val value = "Active"
  }

  case object Default extends DesRelationshipEndReason {
    val value = "DEFAULT"
  }

  object Enumeration{

    def withValue(v: String): Option[DesRelationshipEndReason] = values.find(_.value == v)

    val values = Set(
      Death, Divorce, InvalidParticipant, Cancelled, Rejected, Hmrc, Closed, Merger, Retrospective, System, Active, Default
    )
  }

  implicit val formats: Format[DesRelationshipEndReason] = new Format[DesRelationshipEndReason] {
    override def writes(o: DesRelationshipEndReason): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[DesRelationshipEndReason] = {
      val reason = json.as[String]
      Enumeration.withValue(reason) match {
        case Some(r) => JsSuccess(r)
        case None =>
          logger.warn(s"Invalid relationship end status has been received: $reason. Treating it as Default status.")
          JsSuccess(Default)
      }
    }
  }
}
