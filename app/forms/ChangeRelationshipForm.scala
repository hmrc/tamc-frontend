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

package forms

import play.api.data._
import play.api.data.Forms._
import models.ChangeRelationship
import models.EndReasonCode
import org.joda.time.LocalDate
import uk.gov.hmrc.play.mappers.DateTuple._
import config.ApplicationConfig
import services.TimeService
import play.api.data.validation.Constraint
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import org.joda.time.LocalDate

object ChangeRelationshipForm {

  val changeRelationshipForm = Form[ChangeRelationship](
    mapping(
      "role" -> optional(text),
      "endReason" -> optional(text),
      "historicActiveRecord" -> optional(boolean))(
      (role, endReason, historicActiveRecord) => ChangeRelationship(role, endReason, historicActiveRecord))(
      (input: ChangeRelationship) => Some((input.role, input.endReason, input.historicActiveRecord))))

  private def getOptionalLocalDate(day: Option[Int], month: Option[Int], year: Option[Int]): Option[LocalDate] =
    (day, month, year) match {
      case (Some(d), Some(m), Some(y)) => Some(new LocalDate(y, m, d))
      case _ => None
    }

  private def dateOfDivorce() = dateTuple()

  private def checkDateRange(): Constraint[Option[LocalDate]] = Constraint[Option[LocalDate]]("date.range") { dod =>
    dod match {
      case None => Invalid(ValidationError("pages.form.field.dod.error.required"))
      case Some(date) if date.isAfter(TimeService.getCurrentDate) => Invalid(ValidationError("pages.form.field.dom.error.max-date"))
      case Some(date) if date.isBefore(ApplicationConfig.TAMC_MIN_DATE.plusDays(-1)) => Invalid (ValidationError ("pages.form.field.dom.error.min-date") )
      case _ => Valid
    }
  }

  private def dateOfDivorceValidator() = dateTuple().verifying(checkDateRange())

  val updateRelationshipDivorceNoDodForm = Form[ChangeRelationship](
    mapping(
      "role" -> optional(text),
      "endReason" -> optional(text),
      "historicActiveRecord" -> optional(boolean),
      "creationTimestamp" -> optional(text))(
      (role: Option[String], endReason: Option[String], historicActiveRecord: Option[Boolean], creationTimestamp: Option[String]) => ChangeRelationship(role, endReason, historicActiveRecord, creationTimestamp))(
      (input: ChangeRelationship) => Some((input.role, input.endReason, input.historicActiveRecord, input.endReason))))

  val updateRelationshipDivorceForm = Form[ChangeRelationship](
    mapping(
      "role" -> optional(text),
      "endReason" -> optional(text),
      "historicActiveRecord" -> optional(boolean),
      "creationTimestamp" -> optional(text),
      "dateOfDivorce" -> dateOfDivorceValidator)(ChangeRelationship.apply)(ChangeRelationship.unapply))

  val updateRelationshipForm = Form[ChangeRelationship](
    mapping(
      "role" -> optional(text),
      "endReason" -> optional(text).verifying(
        error = "error.end-reason.required",
        constraint = List(Some("DIVORCE"), Some("CANCEL"), Some("REJECT"), Some("EARNINGS"), Some("BEREAVEMENT")).contains(_)),
      "historicActiveRecord" -> optional(boolean),
      "creationTimestamp" -> optional(text),
      "dateOfDivorce" -> dateOfDivorce())(ChangeRelationship.apply)(ChangeRelationship.unapply))

  val divorceForm = Form[ChangeRelationship](
    mapping(
      "role" -> optional(text),
      "endReason" -> optional(text).verifying(
        error = "error.divorce-reason.required",
        constraint = List(Some("DIVORCE_CY"), Some("DIVORCE_PY")).contains(_)),
      "historicActiveRecord" -> optional(boolean),
      "creationTimestamp" -> optional(text),
      "dateOfDivorce" -> dateOfDivorce())(ChangeRelationship.apply)(ChangeRelationship.unapply))
}
