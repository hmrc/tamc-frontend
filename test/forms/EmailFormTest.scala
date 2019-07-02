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

import java.util.Locale

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec

class EmailFormTest extends UnitSpec with I18nSupport with GuiceOneAppPerSuite with MockitoSugar {

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val messages = messagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  ".email" should {
    "bind a valid email address" in {
      val formInput = Map[String, String](
        "transferor-email" -> "example@email.com"
      )
      val res = EmailForm.emailForm.mapping.bind(formInput)
      res shouldBe Right(new EmailAddress("example@email.com"))
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
