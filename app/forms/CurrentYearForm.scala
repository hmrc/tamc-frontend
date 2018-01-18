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

import models.MultiYearInput
import models.Gender
import models.RegistrationFormInput
import play.api.data.FormError
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.Forms.number
import play.api.data.Forms.of
import play.api.data.Forms.list
import play.api.data.Forms.boolean
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
import models.CurrentYearInput

object CurrentYearForm {
  def currentYearForm(historicYearsAvailable: Boolean = false) = Form[CurrentYearInput](mapping(
    "applyForCurrentYear" ->
      optional(boolean).verifying("pages.form.field-required.applyForCurrentYear", _.isDefined)
    )(CurrentYearInput.apply)(CurrentYearInput.unapply))
}
