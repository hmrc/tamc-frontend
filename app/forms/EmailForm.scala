/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayFormFormatter.{emailMaxLength, valueIsPresent}

import scala.util.{Success, Try}


object EmailForm {

  val emailForm: Form[EmailAddress] = Form("transferor-email" -> email)

  private def messageCustomizer(messageKey: String): String = s"pages.form.field.transferor-email.${messageKey}"


  private val formatPatternRegex: String =
    """^([a-zA-Z0-9!#$%&’'*+/=?^_`{|}~-]+[\.]?)+([a-zA-Z0-9!#$%&’'*+/=?^_`{|}~-]+)@([a-zA-Z0-9-]+(?:\.[a-zA-Z0-9]{2,})+)$"""

  private def email: Mapping[EmailAddress] =
    valueIsPresent(errorRequired = messageCustomizer("error.required"))
      .verifying {
        validEmail(formatError = messageCustomizer("error.email"))
      }
      .transform[EmailAddress](EmailAddress(_), _.value)
      .verifying {
        emailMaxLength(maxLength = 100, error = messageCustomizer("error.maxLength"))
      }

  private def validEmail(formatError: String,
                         name: String = "constraint.emailFormat"): Constraint[String] =
    Constraint[String](name) {
      email =>
        Try(EmailAddress(email)) match {
          case Success(string) if string.matches(formatPatternRegex)  => Valid
          case _ => Invalid(ValidationError(formatError))
        }
    }
}
