/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Logger
import play.api.libs.json._

sealed trait RelationshipEndReason {
  def value: String
}

object RelationshipEndReason {

  case object Death extends RelationshipEndReason {
    val value = "DEATH"
  }

  case object Divorce extends RelationshipEndReason {
    val value = "DIVORCE"
  }

  case object InvalidParticipant extends RelationshipEndReason {
    val value = "INVALID_PARTICIPANT"
  }

  case object Cancelled extends RelationshipEndReason {
    val value = "CANCELLED"
  }

  case object Rejected extends RelationshipEndReason {
    val value = "REJECTED"
  }

  case object Hmrc extends RelationshipEndReason {
    val value = "HMRC"
  }

  case object Closed extends RelationshipEndReason {
    val value = "CLOSED"
  }

  case object Merger extends RelationshipEndReason {
    val value = "MERGER"
  }

  case object Retrospective extends RelationshipEndReason {
    val value = "RETROSPECTIVE"
  }

  case object System extends RelationshipEndReason {
    val value = "SYSTEM"
  }

  case object Active extends RelationshipEndReason {
    val value = "Active"
  }

  case object Default extends RelationshipEndReason {
    val value = "DEFAULT"
  }

  object Enumeration{

    def withValue(v: String): Option[RelationshipEndReason] = values.find(_.value == v)

    val values = Set(
      Death, Divorce, InvalidParticipant, Cancelled, Rejected, Hmrc, Closed, Merger, Retrospective, System, Active, Default
    )
  }

  implicit val formats: Format[RelationshipEndReason] = new Format[RelationshipEndReason] {
    override def writes(o: RelationshipEndReason): JsValue = JsString(o.value)

    override def reads(json: JsValue): JsResult[RelationshipEndReason] = {
      val reason = json.as[String]
      Enumeration.withValue(reason) match {
        case Some(r) => JsSuccess(r)
        case None =>
          Logger.warn(s"Invalid relationship end status has been received: $reason. Treating it as Default status.")
          JsSuccess(Default)
      }
    }
  }
}