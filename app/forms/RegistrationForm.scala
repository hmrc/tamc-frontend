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

import models.Gender
import models.RegistrationFormInput
import play.api.data.FormError
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.of
import play.api.data.Forms.optional
import play.api.data.Mapping
import play.api.data.format.Formatter
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.pattern
import play.api.data.validation.Invalid
import play.api.data.validation.Valid
import play.api.data.validation.ValidationError
import uk.gov.hmrc.domain.Nino
import org.joda.time.LocalDate
import uk.gov.hmrc.play.mappers.DateTuple._
import config.ApplicationConfig

object RegistrationForm {

  private def maxLengthWithError(maxLength: Int, error: String = "error.maxLength"): Constraint[String] = Constraint[String]("constraint.maxLength", maxLength) { o =>
    if (o == null) Invalid(ValidationError(error, maxLength)) else if (o.size <= maxLength) Valid else Invalid(ValidationError(error, maxLength))
  }

  private def nonEmptyTrimmer(error: String = "error.required"): Mapping[String] =
    of(new Formatter[String] {
      def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
      def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
        data.get(key).map { _.trim }.filterNot(_.isEmpty()).toRight(Seq(FormError(key, error)))
      }
    })

  private def nameMapping(errorRequired: String, errorMaxLength: String, errorPattern: String): Mapping[String] =
    nonEmptyTrimmer(error = errorRequired).
      verifying(maxLengthWithError(maxLength = 35, error = errorMaxLength)).
      verifying(pattern(regex = """[ \.\,\-\"\'\`\(\)A-Za-z]+""".r, error = errorPattern))

  private def nameMappingCustomizer(messageCustomizer: (String => String)): Mapping[String] =
    nameMapping(
      errorRequired = messageCustomizer.apply("error.required"),
      errorMaxLength = messageCustomizer.apply("error.maxLength"),
      errorPattern = messageCustomizer.apply("error.pattern"))

  private def genderMapping(errorRequired: String = "error.required", errorInvalid: String = "error.invalid"): Mapping[Gender] =
    nonEmptyTrimmer(error = errorRequired).
      verifying(error = errorInvalid, constraint = Gender.isValid(_)).
      transform[Gender](Gender(_), _.gender)

  private def ninoMapping(errorRequired: String = "error.required", errorInvalid: String = "error.invalid"): Mapping[Nino] =
    nonEmptyTrimmer(error = errorRequired).
      transform[String](utils.normaliseNino(_), _.toString()).
      verifying(error = errorInvalid, constraint = Nino.isValid(_)).
      transform[Nino](Nino(_), _.nino)

  private def firstNameMessageCustomizer(messageKey: String): String = s"pages.form.field.name.error.${messageKey}"
  private def firstName: Mapping[String] =
    nameMappingCustomizer(
      messageCustomizer = firstNameMessageCustomizer)

  private def lastNameMessageCustomizer(messageKey: String): String = s"pages.form.field.last-name.error.${messageKey}"
  private def lastName: Mapping[String] =
    nameMappingCustomizer(
      messageCustomizer = lastNameMessageCustomizer)

  private def genderMessageCustomizer(messageKey: String): String = s"pages.form.field.gender.error.${messageKey}"
  private def gender: Mapping[Gender] =
    genderMapping(
      errorRequired = genderMessageCustomizer("error.required"),
      errorInvalid = genderMessageCustomizer("error.invalid"))

  private def ninoMessageCustomizer(messageKey: String): String = s"pages.form.field.nino.error.${messageKey}"
  private def nino: Mapping[Nino] =
    ninoMapping(
      errorRequired = ninoMessageCustomizer("error.required"),
      errorInvalid = ninoMessageCustomizer("error.invalid"))
      
  private def dateOfMarriageValidator(today: LocalDate) = {
    mandatoryDateTuple("pages.form.field.dom.error.required").
      verifying(error = "pages.form.field.dom.error.min-date", constraint = _.isAfter(ApplicationConfig.TAMC_MIN_DATE.plusDays(-1))).
      verifying(error = "pages.form.field.dom.error.max-date", constraint = _.isBefore(today.plusDays(1))) }

  def registrationForm(today: LocalDate, transferorNino: Nino) = Form[RegistrationFormInput](
    mapping(
      "name" -> firstName,
      "last-name" -> lastName,
      "gender" -> gender,
      "nino" -> nino.verifying("pages.form.field.nino.error.self", recipientNino => !utils.areEqual(transferorNino, recipientNino)),
      "dateOfMarriage" -> dateOfMarriageValidator(today))(RegistrationFormInput.apply)(RegistrationFormInput.unapply))
}
