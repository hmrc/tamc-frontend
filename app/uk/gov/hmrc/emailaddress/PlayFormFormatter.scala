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

package uk.gov.hmrc.emailaddress

import play.api.data.Forms.{of, optional, text, tuple}
import play.api.data.format.Formatter
import play.api.data.validation._
import play.api.data.{FormError, Mapping}

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import scala.util.Try

object PlayFormFormatter {

  private def nonEmptyTrimmer(error: String): Mapping[String] =
    of(new Formatter[String] {
      def unbind(key: String, value: String): Map[String, String] = Map(key -> value)

      def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
        data.get(key)
          .filterNot(_ == null)
          .map(_.trim)
          .filterNot(_.isEmpty())
          .toRight(Seq(FormError(key, error)))
      }
    })

  def valueIsPresent(errorRequired: String = "error.required"): Mapping[String] =
    nonEmptyTrimmer(error = errorRequired)

  /**
    * Defines a maximum length constraint for [[uk.gov.hmrc.emailaddress.EmailAddress]] values
    *
    * @param error error message with default value `"error.maxLength(maxLength)"`
    * @param name  constraint's name with default value `"constraint.maxLength(maxLength)"`
    */
  def emailMaxLength(maxLength: Int, name: String = "constraint.maxLength", error: String = "error.maxLength"): Constraint[EmailAddress] =
    Constraint[EmailAddress](name, maxLength) {
      email =>
        if (email == null) Invalid(ValidationError(error, maxLength))
        else if (email.value.length <= maxLength) Valid
        else Invalid(ValidationError(error, maxLength))
    }

  def validDateTuple(missingPartError: String = "error.enter_full_date",
                     allAbsentError: String = "error.enter_a_date",
                     nonNumericError: String = "error.enter_numbers",
                     invalidError: String = "error.enter_valid_date"): Mapping[ZonedDateTime] = {

    def verifyDigits(triple: (String, String, String)): Boolean =
      triple._1.forall(_.isDigit) && triple._2.forall(_.isDigit) && triple._3.forall(_.isDigit)

    tuple(
      "year" -> optional(text),
      "month" -> optional(text),
      "day" -> optional(text)
    )
      .verifying(datePartsArePresent(allAbsentError = allAbsentError, missingPartError = missingPartError))
      .transform[(String, String, String)](x => (x._1.get.trim, x._2.get.trim, x._3.get.trim), x => (Some(x._1), Some(x._2), Some(x._3)))
      .verifying(nonNumericError, verifyDigits _)
      .verifying(invalidError, x => !verifyDigits(x) || Try(LocalDateTime.of(x._1.toInt, x._2.toInt, x._3.toInt, 0, 0)).isSuccess)
      .transform[ZonedDateTime](
        x => ZonedDateTime.of(
          x._1.toInt, x._2.toInt, x._3.toInt, 0, 0, 0, 0, ZoneId.systemDefault()),
        x => (x.getYear.toString, x.getMonthValue.toString, x.getDayOfMonth.toString)
      )
  }

  private def datePartsArePresent(name: String = "constraint.datepresent",
                                  allAbsentError: String,
                                  missingPartError: String): Constraint[(Option[String], Option[String], Option[String])] = {

    Constraint[(Option[String], Option[String], Option[String])](name) {
      tup =>
        if (tup._1.isDefined && tup._2.isDefined && tup._3.isDefined) Valid else {
          if (tup._1.isEmpty && tup._2.isEmpty && tup._3.isEmpty) {
            Invalid(ValidationError(allAbsentError))
          } else {
            Invalid(ValidationError(missingPartError))
          }
        }
    }
  }
}
