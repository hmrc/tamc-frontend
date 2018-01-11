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

import config.ApplicationConfig
import models.{DateOfMarriageFormInput, Gender, RegistrationFormInput}
import org.joda.time.LocalDate
import play.api.data.Forms.{mapping, of, optional}
import play.api.data.{Form, FormError, Mapping}
import play.api.data.format.Formatter
import play.api.data.validation.Constraints.pattern
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.play.mappers.DateTuple._

object DateOfMarriageForm {
      
  private def dateOfMarriageValidator(today: LocalDate) =
    mandatoryDateTuple("pages.form.field.dom.error.required").
      verifying(error = "pages.form.field.dom.error.min-date", constraint = _.isAfter(ApplicationConfig.TAMC_MIN_DATE.plusDays(-1))).
      verifying(error = "pages.form.field.dom.error.max-date", constraint = _.isBefore(today.plusDays(1)))

  def dateOfMarriageForm(today: LocalDate) = Form[DateOfMarriageFormInput](
    mapping(
      "dateOfMarriage" -> dateOfMarriageValidator(today))(DateOfMarriageFormInput.apply)(DateOfMarriageFormInput.unapply))
}
