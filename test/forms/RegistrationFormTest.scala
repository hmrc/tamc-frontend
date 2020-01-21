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

import java.util.Locale

import config.ApplicationConfig
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.FormError
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import uk.gov.hmrc.play.test.UnitSpec

class RegistrationFormTest extends UnitSpec with I18nSupport with GuiceOneAppPerSuite with MockitoSugar {

  implicit def messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val messages = messagesApi.preferred(Seq(Lang(Locale.ENGLISH)))

  ".dateOfMarriageValidator" should {

    "bind a valid date successfully" in {
      val formInput = Map[String, String](
        "day" -> "12",
        "month" -> "02",
        "year" -> "2019"
      )
      val res = RegistrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)
      res shouldBe Right(new LocalDate(2019, 2, 12))
    }

    "fail to bind a date which is entirely absent" in {

      val formInput = Map.empty[String, String]
      val res = RegistrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.enter_a_date", Nil)
      ))
    }

    "fail to bind a date is partially absent" in {
      val formInput = Map[String, String](
        "day" -> "12",
        "year" -> "2019"
      )
      val res = RegistrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.enter_full_date", Nil)
      ))
    }

    "fail to bind a date which is earlier the minimum configured date" in {

      val earliestDate = ApplicationConfig.TAMC_MIN_DATE.minusDays(1)
      val checkDate = earliestDate.minusDays(1)

      val formInput = Map[String, String](
        "day" -> checkDate.getDayOfMonth.toString,
        "month" -> checkDate.getMonthOfYear.toString,
        "year" -> checkDate.getYear.toString
      )
      val res = RegistrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", messages("pages.form.field.dom.error.min-date", earliestDate.toString("d MM YYYY")), Nil)
      ))
    }

    "fail to bind a date which is later than today + 1" in {

      val today = LocalDate.now()
      val tooLate = today.plusDays(2)

      val formInput = Map[String, String](
        "day" -> tooLate.getDayOfMonth.toString,
        "month" -> tooLate.getMonthOfYear.toString,
        "year" -> tooLate.getYear.toString
      )
      val res = RegistrationForm.dateOfMarriageValidator(today).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", messages("pages.form.field.dom.error.max-date", today.plusDays(1).toString("d MM YYYY")), Nil)
      ))
    }

    "fail to bind a date containing non numeric" in {

      val formInput = Map[String, String](
        "day" -> "12",
        "month" -> "12",
        "year" -> "2o19"
      )
      val res = RegistrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

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
      val res = RegistrationForm.dateOfMarriageValidator(LocalDate.now()).bind(formInput)

      res shouldBe Left(Seq(
        FormError("", "pages.form.field.dom.error.enter_valid_date", Nil)
      ))
    }
  }
}
