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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.data.FormError
import utils.EmailAddress

class PlayFormFormatterSpec extends AnyWordSpec with Matchers {

  val emails: List[String] = EmailAddressGenerators.randomEmailAddresses().distinct

  "PlayFormFormatter.emailAddress mapping" should {

    "emails should not be empty" in {
      assert(emails.nonEmpty)
    }

    emails foreach { email =>
      s"$email map in EmailAddress with success" in {
        EmailAddress(email).isInstanceOf[EmailAddress]
      }
    }

    emails foreach { email =>
      s"$email accept valid emails" in {
        emailAddressMapping.bind(Map("email" -> email)).toOption.get shouldBe EmailAddress(email)
      }
    }

    emails foreach { email =>
      s"$email accept valid emails with padding" in {
        emailAddressMapping.bind(Map("email" -> s" $email ")).toOption.get shouldBe EmailAddress(email)
      }
    }

    "reject missing or empty field" in {
      emptyEmailInputData.foreach(
        address =>
          emailAddressMapping.bind(data = address).swap.getOrElse(List(FormError("", ""))) shouldBe List(FormError("email", List("error.required"), List())))
    }

    "reject missing or empty field (using custom error message)" in {
      emptyEmailInputData.foreach(
        address =>
          emailAddressCustomErrorsMapping.bind(data = address).swap.getOrElse(List(FormError("", ""))) shouldBe List(FormError("email", List("field.required"), List())))
    }

    invalidEmailInputData foreach { email =>
      s"$email reject invalid email" in {
        emailAddressMapping.bind(data = email).swap.getOrElse(List(FormError("", ""))) shouldBe List(FormError("email", List("error.email"), List()))
      }
    }

    invalidEmailInputData foreach { email =>
      s"$email reject invalid email (using custom error message)" in {
        emailAddressCustomErrorsMapping.bind(data = email).swap.getOrElse(List(FormError("", ""))) shouldBe List(FormError("email", List("field.invalid"), List()))
      }
    }

    "reject too long email address (when using 'emailMaxLength' constraint)" in {
      emailAddressMapping.verifying {
       maxLengthConstraint
      }.bind(Map("email" -> "aaa@bbb.ccc")).swap.getOrElse(List(FormError("", ""))) shouldBe
          List(FormError("email", List("error.maxLength"), List(10)))
      }
    }

  private def emptyEmailInputData: Seq[Map[String, String]] =
    Seq(
      Map(),
      Map("email" -> null),
      Map("email" -> ""),
      Map("email" -> " "),
      Map("email" -> "        "),
      Map("email" -> "\t\t\t\t")
    )

  private def invalidEmailInputData: Seq[Map[String, String]] =
    Seq(
      Map("email" -> "test"),
      Map("email" -> "test@"),
      Map("email" -> "@test"),
      Map("email" -> "test@example.comtest@example.com"),
      Map("email" -> "test@example..com"),
      Map("email" -> "test@example...com"),
      Map("email" -> "test@example....com"),
      Map("email" -> "test@example com")
    )

  private def emailAddressMapping =
    PlayFormFormatter.valueIsPresent()
      .verifying(error = "error.email", constraint = EmailAddress.isValid)
      .transform[EmailAddress](EmailAddress(_), _.value).withPrefix("email")

  private def emailAddressCustomErrorsMapping =
    PlayFormFormatter.valueIsPresent(errorRequired = "field.required")
      .verifying(error = "field.invalid", constraint = EmailAddress.isValid)
      .transform[EmailAddress](EmailAddress(_), _.value).withPrefix("email")

  private def maxLengthConstraint =
    PlayFormFormatter.emailMaxLength(maxLength = 10)
}
