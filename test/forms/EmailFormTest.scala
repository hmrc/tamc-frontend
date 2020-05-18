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

import play.api.data.FormError
import test_utils.TAMCSetupSpec
import uk.gov.hmrc.emailaddress.EmailAddress

class EmailFormTest extends TAMCSetupSpec {

  ".email" should {
    "bind a valid email address" in {
      val formInput = Map[String, String](
        "transferor-email" -> "example@email.com"
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Right(new EmailAddress("example@email.com"))
    }

    "bind a valid email address2" in {
      val formInput = Map[String, String](
        "transferor-email" -> "example@c.email.com"
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Right(new EmailAddress("example@c.email.com"))
    }

    "bind a valid email address3" in {
      val formInput = Map[String, String](
        "transferor-email" -> "example.c@email.com"
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Right(new EmailAddress("example.c@email.com"))
    }

    "fail to bind, with a specific valid character failure for am email address containing invalid characters" in {
      val formInput = Map[String, String](
        "transferor-email" -> """example"@email.com"""
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Left(Seq(
        FormError("transferor-email", Seq("pages.form.field.transferor-email.error.character"), Nil)
      ))
    }

    "fail to bind, with a general format failure for am invalid email address that otherwise contains only valid chars " in {
      val formInput = Map[String, String](
        "transferor-email" -> """exampleemail.com"""
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Left(Seq(
        FormError("transferor-email", Seq("pages.form.field.transferor-email.error.email"), Nil)
      ))
    }

    "fail to bind, with a max length failure, for am email address that is too long " in {
      val formInput = Map[String, String](
        "transferor-email" ->
          """exampleLKJHLKJHKLJHLKJHLKJHKLJHKJLHKLJHLKJHKLHKLJHKJLHKLHKLJHKLJHKLJHKJHKLJHKJHKLHKLJHKLJHKLJHKLJHKLJHKLJLJHLHLKJHJK@email.com""".stripMargin
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Left(Seq(
        FormError("transferor-email", Seq("pages.form.field.transferor-email.error.maxLength"), Seq(100))
      ))
    }
  }
}
