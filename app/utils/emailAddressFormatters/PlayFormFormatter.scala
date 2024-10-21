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

package utils.emailAddressFormatters

import play.api.data.Forms.{of, optional, text, tuple}
import play.api.data.format.Formatter
import play.api.data.validation._
import play.api.data.{FormError, Mapping}
import utils.EmailAddress

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

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
   * Defines a maximum length constraint for [[EmailAddress]] values
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

  def validDateTuple(missingYearError: String = "pages.form.field.dom.error.must.include.year",
                     missingMonthError: String = "pages.form.field.dom.error.must.include.month",
                     missingDayError: String = "pages.form.field.dom.error.must.include.day",
                     allAbsentError: String = "pages.form.field.dom.error.enter_a_date",
                     nonNumericError: String = "pages.form.field.dom.error.enter_numbers",
                     invalidError: String = "pages.form.field.dom.error.enter_valid_date",
                     invalidDayError: String = "pages.form.field.dom.error.invalid.day",
                     invalidMonthError: String = "pages.form.field.dom.error.invalid.month",
                     invalidYearError: String = "pages.form.field.dom.error.invalid.year",
                     yearTodayOrPast: String = "pages.form.field.dom.error.max-date"
                    ): Mapping[ZonedDateTime] = {

    def verifyDigits(triple: (String, String, String)): Boolean =
      triple._1.forall(_.isDigit) && triple._2.forall(_.isDigit) && triple._3.forall(_.isDigit)

    tuple(
      "year" -> optional(text),
      "month" -> optional(text),
      "day" -> optional(text)
    )
      .verifying(datePartsArePresent(
        allAbsentError = allAbsentError,
        missingYearError = missingYearError,
        missingMonthError = missingMonthError,
        missingDayError = missingDayError
      )
      ).transform[(String, String, String)](x =>  (x._1.get.trim, x._2.get.trim, x._3.get.trim), x =>  (Some(x._1), Some(x._2), Some(x._3)))
      .verifying(nonNumericError, verifyDigits _)
      .transform[(Int, Int, Int)](x =>  (x._1.toInt, x._2.toInt, x._3.toInt), x =>  (x.toString(), x.toString(), x.toString()))
      .verifying(checkDateRangeValidator(invalidDay = invalidDayError, invalidMonth = invalidMonthError, invalidYear = invalidYearError, yearTodayOrPast = yearTodayOrPast))
      .transform[ZonedDateTime](x => ZonedDateTime.of(x._1, x._2, x._3, 0, 0, 0, 0, ZoneId.systemDefault()),
        x => (x.getYear, x.getMonthValue, x.getDayOfMonth)
      )
  }

  private def datePartsArePresent(name: String = "constraint.datepresent",
                                  allAbsentError: String,
                                  missingYearError: String,
                                  missingMonthError: String,
                                  missingDayError: String): Constraint[(Option[String], Option[String], Option[String])] = {

    Constraint[(Option[String], Option[String], Option[String])](name) {
      case tup if tup._1.isEmpty && tup._2.isEmpty && tup._3.isEmpty => Invalid(ValidationError(allAbsentError))
      case tup if tup._3.isEmpty => Invalid(ValidationError(missingDayError))
      case tup if tup._2.isEmpty => Invalid(ValidationError(missingMonthError))
      case tup if tup._1.isEmpty => Invalid(ValidationError(missingYearError))
      case _ => Valid
    }
  }

  private def checkDateRangeValidator(
                                       name: String = "constraint.datepresent",
                                       invalidDay: String,
                                       invalidMonth: String,
                                       invalidYear: String,
                                       yearTodayOrPast: String
                                     ): Constraint[(Int, Int, Int)] = {

    val currentYear: Int = LocalDateTime.now().getYear

    Constraint[(Int, Int, Int)](name) {
      case tup if tup._2 < 1 || tup._2 > 12 => Invalid(ValidationError(invalidMonth))
      case tup if tup._3 < 1 || tup._3 > dayRange(tup._2, tup._1) => Invalid(ValidationError(invalidDay, dayRange(tup._2, tup._1)))
      case tup if dayRange(tup._2, tup._1) <= 31 && tup._2 <= 12 && tup._1.toString.length == 4 && tup._1 > currentYear => Invalid(ValidationError(yearTodayOrPast))
      case tup if tup._1 < 1900 || tup._1 > currentYear => Invalid(ValidationError(invalidYear, currentYear.toString))
      case _ => Valid
    }

  }

  def dayRange(monthNumber: Int, year: Int): Int = {
    Map(
      1 -> 31,
      2 -> (if (year % 4 == 0) 29 else 28),
      3 -> 31,
      4 -> 30,
      5 -> 31,
      6 -> 30,
      7 -> 31,
      8 -> 31,
      9 -> 30,
      10 -> 31,
      11 -> 30,
      12 -> 31
    )(monthNumber)
  }
}
