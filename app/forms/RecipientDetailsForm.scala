/*
 * Copyright 2025 HM Revenue & Customs
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

import models.RecipientDetailsFormInput

import java.time.LocalDate
import play.api.data.Forms.mapping
import play.api.data.Form
import uk.gov.hmrc.domain.Nino

import javax.inject.Inject

class RecipientDetailsForm@Inject()(registrationForm: RegistrationForm) {

  def recipientDetailsForm(today: LocalDate, transferorNino: Nino) = Form[RecipientDetailsFormInput](
    mapping(
      "name" -> registrationForm.firstName,
      "last-name" -> registrationForm.lastName,
      "gender" -> registrationForm.gender,
      "nino" -> registrationForm.nino.verifying("pages.form.field.nino.error.self", recipientNino => !utils.areEqual(transferorNino, recipientNino)))(RecipientDetailsFormInput.apply)(RecipientDetailsFormInput.unapply))
}
