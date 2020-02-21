/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json._

sealed trait EndMarriageAllowanceReason{
  val desEnumerationValue: Option[String] = None
}

case object Divorce extends EndMarriageAllowanceReason {
  override val desEnumerationValue = Some("Divorce/Separation")
}

case object Cancel extends EndMarriageAllowanceReason {
  override val desEnumerationValue = Some("Cancelled by Transferor")
}

case object Earnings extends EndMarriageAllowanceReason

case object Bereavement extends EndMarriageAllowanceReason

sealed trait DesRelationshipEndEnumeration extends EndMarriageAllowanceReason {

}

object EndMarriageAllowanceReason {

  implicit val writesEndRelationship = new Writes[EndMarriageAllowanceReason] {
    override def writes(endRelationship: EndMarriageAllowanceReason) = {
      endRelationship match {
        case Divorce => JsString("Divorce")
        case Cancel => JsString("Cancel")
        case Earnings => JsString("Earnings")
        case Bereavement => JsString("Bereavement")
      }
    }
  }

  implicit val readsEndRelationship = new Reads[EndMarriageAllowanceReason] {
    override def reads(value: JsValue): JsResult[EndMarriageAllowanceReason] = {
      value match {
        case JsString("Divorce") => JsSuccess(Divorce)
        case JsString("Cancel") => JsSuccess(Cancel)
        case JsString("Earnings") => JsSuccess(Earnings)
        case JsString("Bereavement") => JsSuccess(Bereavement)

      }
    }
  }

  def toCaseObject(endReason: String): EndMarriageAllowanceReason = endReason match {
    case "Divorce" => Divorce
    case "Cancel" => Cancel
    case "Earnings" => Earnings
    case "Bereavement" => Bereavement
  }
}