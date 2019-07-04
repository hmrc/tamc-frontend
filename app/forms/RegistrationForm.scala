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

package forms

import java.text.SimpleDateFormat

import config.ApplicationConfig
import models.{Gender, RegistrationFormInput}
import org.joda.time.{DateTime, LocalDate}
import play.api.data.Forms.{mapping, of}
import play.api.data.format.Formatter
import play.api.data.validation.Constraints.pattern
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, FormError, Mapping}
import play.api.i18n.Messages
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.PlayFormFormatter.validDateTuple

object RegistrationForm {

  private def maxLengthWithError(maxLength: Int, error: String = "error.maxLength"): Constraint[String] = Constraint[String]("constraint.maxLength", maxLength) { o =>
    if (o == null) Invalid(ValidationError(error, maxLength)) else if (o.length <= maxLength) Valid else Invalid(ValidationError(error, maxLength))
  }

  private def nonEmptyTrimmer(error: String = "error.required"): Mapping[String] =
    of(new Formatter[String] {
      def unbind(key: String, value: String): Map[String, String] = Map(key -> value)

      def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
        data.get(key).map {
          _.trim
        }.filterNot(_.isEmpty()).toRight(Seq(FormError(key, error)))
      }
    })

  private def nameMapping(errorRequired: String, errorMaxLength: String, errorPattern: String): Mapping[String] =
    nonEmptyTrimmer(error = errorRequired).
      verifying(maxLengthWithError(maxLength = 35, error = errorMaxLength)).
      verifying(pattern(regex = """[ \-\(\)A-Za-z]+""".r, error = errorPattern))

  private def nameMappingCustomizer(messageCustomizer: String => String): Mapping[String] =
    nameMapping(
      errorRequired = messageCustomizer.apply("error.required"),
      errorMaxLength = messageCustomizer.apply("error.maxLength"),
      errorPattern = messageCustomizer.apply("error.pattern"))

  private def genderMapping(errorRequired: String = "error.required", errorInvalid: String = "error.invalid"): Mapping[Gender] =
    nonEmptyTrimmer(error = errorRequired).
      verifying(error = errorInvalid, constraint = Gender.isValid(_)).
      transform[Gender](Gender(_), _.gender)

  private def ninoMapping(errorRequired: String = "error.required",
                          errorMaxLength: String = "error.maxLength",
                          errorInvalidChars: String = "error.invalid.chars",
                          errorInvalid: String = "error.invalid"): Mapping[Nino] =
    nonEmptyTrimmer(error = errorRequired).
      transform[String](utils.normaliseNino(_), _.toString()).
      verifying(ninoIsValidFormat(formatError = errorInvalid, charError = errorInvalidChars, maxLengthError = errorMaxLength)).
      transform[Nino](Nino(_), _.nino)

  private def ninoIsValidFormat(name: String = "constraint.ninoFormat",
                                formatError: String,
                                charError: String,
                                maxLengthError: String): Constraint[String] = {
    val validNinoCharRegexString = """^[a-zA-Z0-9]+"""

    Constraint[String](name) {
      nino =>
        if (Nino.isValid(nino)) Valid else {
          if (!nino.matches(validNinoCharRegexString)) {
            Invalid(ValidationError(charError))
          } else if (nino.length > 9) {
            Invalid(ValidationError(maxLengthError))
          } else {
            Invalid(ValidationError(formatError))
          }
        }
    }
  }

  private def firstNameMessageCustomizer(messageKey: String): String = s"pages.form.field.name.$messageKey"

  def firstName: Mapping[String] =
    nameMappingCustomizer(messageCustomizer = firstNameMessageCustomizer)

  private def lastNameMessageCustomizer(messageKey: String): String = s"pages.form.field.last-name.$messageKey"

  def lastName: Mapping[String] =
    nameMappingCustomizer(messageCustomizer = lastNameMessageCustomizer)

  private def genderMessageCustomizer(messageKey: String): String = s"pages.form.field.gender.$messageKey"

  def gender: Mapping[Gender] =
    genderMapping(
      errorRequired = genderMessageCustomizer("error.required"),
      errorInvalid = genderMessageCustomizer("error.invalid")
    )

  private def ninoMessageCustomizer(messageKey: String): String = s"pages.form.field.nino.$messageKey"

  def nino: Mapping[Nino] =
    ninoMapping(
      errorRequired = ninoMessageCustomizer("error.required"),
      errorMaxLength = ninoMessageCustomizer("error.maxLength"),
      errorInvalidChars = ninoMessageCustomizer("error.invalid.chars"),
      errorInvalid = ninoMessageCustomizer("error.invalid"))

  def dateOfMarriageValidator(today: LocalDate)(implicit messages: Messages): Mapping[LocalDate] = {
    val minDate = ApplicationConfig.TAMC_MIN_DATE.minusDays(1)
    val maxDate = today.plusDays(1)

    validDateTuple("pages.form.field.dom.error.enter_full_date",
      "pages.form.field.dom.error.enter_a_date",
      "pages.form.field.dom.error.enter_numbers",
      "pages.form.field.dom.error.enter_valid_date")
      .transform[LocalDate](dt => dt.toLocalDate, ld => new DateTime(ld.getYear(), ld.getMonthOfYear(), ld.getDayOfMonth(), 0, 0).withTimeAtStartOfDay())
      .verifying(error = Messages("pages.form.field.dom.error.min-date", minDate.toString("d MM YYYY")), constraint = _.isAfter(minDate))
      .verifying(error = Messages("pages.form.field.dom.error.max-date", maxDate.toString("d MM YYYY")), constraint = _.isBefore(maxDate))
  }

  def registrationForm(today: LocalDate, transferorNino: Nino)(implicit messages: Messages) = Form[RegistrationFormInput](
    mapping(
      "name" -> firstName,
      "last-name" -> lastName,
      "gender" -> gender,
      "nino" -> nino.verifying("pages.form.field.nino.error.self", recipientNino => !utils.areEqual(transferorNino, recipientNino)),
      "dateOfMarriage" -> dateOfMarriageValidator(today))(RegistrationFormInput.apply)(RegistrationFormInput.unapply))
}
