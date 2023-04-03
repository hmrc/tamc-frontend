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

package models.admin

import play.api.libs.json._
import play.api.mvc.PathBindable

case class FeatureFlag(name: FeatureFlagName, isEnabled: Boolean, description: Option[String] = None)

object FeatureFlag {
  implicit val format: OFormat[FeatureFlag] = Json.format[FeatureFlag]
}

sealed trait FeatureFlagName {
  val description: Option[String] = None
}

object FeatureFlagName {
  implicit val writes: Writes[FeatureFlagName] = (o: FeatureFlagName) => JsString(o.toString)

  implicit val reads: Reads[FeatureFlagName] = {
    case name if name == JsString(PertaxBackendToggle.toString) => JsSuccess(PertaxBackendToggle)
    case _ => JsError("Unknown FeatureFlagName")
  }

  implicit val formats: Format[FeatureFlagName] =
    Format(reads, writes)

  implicit def pathBindable: PathBindable[FeatureFlagName] = new PathBindable[FeatureFlagName] {

    override def bind(key: String, value: String): Either[String, FeatureFlagName] =
      JsString(value).validate[FeatureFlagName] match {
        case JsSuccess(name, _) =>
          Right(name)
        case _                  =>
          Left(s"The feature flag `$value` does not exist")
      }

    override def unbind(key: String, value: FeatureFlagName): String =
      value.toString
  }

  val allFeatureFlags: Seq[PertaxBackendToggle.type] = List(PertaxBackendToggle)
}

case object PertaxBackendToggle extends FeatureFlagName {
  override def toString: String            = "pertax-backend-toggle"
  override val description: Option[String] = Some(
    "Enable/disable pertax backend during auth"
  )
}

case object NoExistantToggle extends FeatureFlagName {
  override def toString: String            = "none-existant-toggle"
  override val description: Option[String] = Some(
    "A toggle that does snot exist"
  )
}

object FeatureFlagMongoFormats {
  implicit val formats: Format[FeatureFlag] =
    Json.format[FeatureFlag]
}
