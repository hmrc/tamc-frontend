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

package forms

import config.ApplicationConfig
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.data.FormError
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import uk.gov.hmrc.emailaddress.PlayFormFormatter.dayRange
import utils.UnitSpec

import java.time.LocalDate

class RegistrationFormTest extends UnitSpec with GuiceOneServerPerSuite {

  val applicationConfig = app.injector.instanceOf[ApplicationConfig]
  lazy val registrationForm: RegistrationForm = app.injector.instanceOf[RegistrationForm]
  implicit val messages = MessagesImpl(Lang("en"), app.injector.instanceOf[MessagesApi])

  ".dateOfMarriageValidator" should {

    "bind a valid date successfully" in {
      val formInput = Map[String, String](
        "day" -> "12",
        "month" -> "02",
        "year" -> "2019"
      )
      val res = registrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)
      res shouldBe Right(LocalDate.of(2019, 2, 12))
    }

    "fail to bind a date which is entirely absent" in {

      val formInput = Map.empty[String, String]
      val res = registrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.enter_a_date", Nil)
      ))
    }

    "fail to bind a date is partially absent" in {
      val formInput = Map[String, String](
        "day" -> "12",
        "year" -> "2019"
      )
      val res = registrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)


      println("\n\n\n" + res + "\n\n\n")

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.must.include.month", Nil)
      ))
    }

    "fail to bind a date which is earlier the minimum configured date" in {

      val earliestDate = applicationConfig.TAMC_MIN_DATE
      val checkDate = earliestDate.minusDays(1)

      val formInput = Map[String, String](
        "day" -> checkDate.getDayOfMonth.toString,
        "month" -> checkDate.getMonthValue.toString,
        "year" -> checkDate.getYear.toString
      )
      val res = registrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.invalid.year", Seq(LocalDate.now().getYear.toString)))
      )
    }

    "fail to bind a date which is later than today + 1" in {

      val today = LocalDate.now()
      val tooLate = today.plusDays(2)

      val formInput = Map[String, String](
        "day" -> tooLate.getDayOfMonth.toString,
        "month" -> s"0${tooLate.getMonthValue.toString}",
        "year" -> tooLate.getYear.toString
      )
      val res = registrationForm.dateOfMarriageValidator(today).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", messages("pages.form.field.dom.error.max-date"), Nil))
      )
    }

    "fail to bind a date containing non numeric" in {

      val formInput = Map[String, String](
        "day" -> "12",
        "month" -> "12",
        "year" -> "2o19"
      )
      val res = registrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.enter_numbers", Nil)
      ))
    }

    "fail to bind a numeric, but invalid date" in {

      val formInput = Map[String, String](
        "day" -> "30",
        "month" -> "02",
        "year" -> "2018"
      )
      val res: Either[Seq[FormError], LocalDate] = registrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.invalid.day", Seq(dayRange(formInput("month").toInt, formInput("year").toInt))))
      )
    }
  }
}
