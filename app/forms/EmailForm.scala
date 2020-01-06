/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.data.{Form, Mapping}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayFormFormatter.{emailMaxLength, emailPattern, valueIsPresent}

object EmailForm {

  private def messageCustomizer(messageKey: String): String = s"pages.form.field.transferor-email.${messageKey}"
  private val initialCharCheckRegexStr = """^[a-zA-Z0-9\-\_\.\@]+"""

  private def emailIsValidFormat(name: String = "constraint.emailFormat",
                                 formatError: String,
                                 charError: String): Constraint[String] =
    Constraint[String](name) {
      email =>
        if (EmailAddress.isValid(email)) Valid else {
          if(!email.matches(initialCharCheckRegexStr)){
            Invalid(ValidationError(charError))
          } else {
            Invalid(ValidationError(formatError))
          }
        }
    }

  private def email: Mapping[EmailAddress] =
    valueIsPresent(errorRequired = messageCustomizer("error.required"))
    .verifying(emailIsValidFormat(formatError = messageCustomizer("error.email"), charError = messageCustomizer("error.character")))
    .transform[EmailAddress](EmailAddress(_), _.value)
    .verifying {
      emailPattern(regex = """^([a-zA-Z0-9\-\_]+[.])*[a-zA-Z0-9\-\_]+@([a-zA-Z0-9-]{2,}[.])+[a-zA-Z0-9-]+$""".r, error = messageCustomizer("error.email"))
    }
    .verifying {
      emailMaxLength(maxLength = 100, error = messageCustomizer("error.maxLength"))
    }

  val emailForm = Form("transferor-email" -> email)
}
