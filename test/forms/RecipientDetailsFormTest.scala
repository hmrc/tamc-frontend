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

import models.{Gender, RecipientDetailsFormInput}
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.play.test.UnitSpec

import scala.util.Random

class RecipientDetailsFormTest extends UnitSpec with I18nSupport with GuiceOneAppPerSuite with MockitoSugar {

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val messages = messagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  ".recipientDetailsForm nino mapping" should {

    val validNino = new Generator(new Random).nextNino
    val form = RecipientDetailsForm.recipientDetailsForm(LocalDate.now(), validNino)

    "fail to bind a nino containing invalid chars, with a explicit invalid char error" in {
      val formInput = Map[String, String](
        "name" -> "name",
        "last-name" -> "last-name",
        "gender" -> "M",
        "nino" -> "AB123*"
      )
      val res = form.mapping.bind(formInput)

      res shouldBe Left(Seq(
        FormError("nino", "pages.form.field.nino.error.invalid.chars", Nil)
      ))
    }

    "fail to bind an invalid nino where no invalid characters are present, but the nino is not in valid form" in {
      val formInput = Map[String, String](
        "name" -> "name",
        "last-name" -> "last-name",
        "gender" -> "M",
        "nino" -> "A1123456A"
      )
      val res = form.mapping.bind(formInput)

      res shouldBe Left(Seq(
        FormError("nino", "pages.form.field.nino.error.invalid", Nil)
      ))
    }

    "fail to bind an invalid nino which is longer than 9 characters" in {
      val formInput = Map[String, String](
        "name" -> "name",
        "last-name" -> "last-name",
        "gender" -> "M",
        "nino" -> "A1123456AA"
      )
      val res = form.mapping.bind(formInput)

      res shouldBe Left(Seq(
        FormError("nino", "pages.form.field.nino.error.maxLength", Nil)
      ))
    }

    "bind a valid nino successfully" in {

      val testValidNino = new Generator(new Random).nextNino

      val formInput = Map[String, String](
        "name" -> "name",
        "last-name" -> "last-name",
        "gender" -> "M",
        "nino" -> testValidNino.nino
      )
      val res = form.mapping.bind(formInput)

      res shouldBe Right(RecipientDetailsFormInput("name","last-name",Gender("M"),testValidNino))
    }
  }
}