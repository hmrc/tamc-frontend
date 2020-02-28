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

package forms.coc

import config.ApplicationConfig
import forms.FormsBaseSpec
import org.joda.time.LocalDate
import play.api.data.FormError
import play.api.data.validation.{Invalid, ValidationError}
import play.api.test.FakeRequest
import services.TimeService
import uk.gov.hmrc.time.TaxYear
import views.helpers.TextGenerator

import scala.collection.mutable

class DivorceSelectYearFormTest extends FormsBaseSpec {

  val today = LocalDate.now()

  "DivorceSelectYearForm" should {
    "bind" when {
      "Todays date is input" in {
        val date = LocalDate.now()

        val formInput = Map[String, String](
          "dateOfDivorce.day" -> date.getDayOfMonth.toString,
          "dateOfDivorce.month" -> date.getMonthOfYear.toString,
          "dateOfDivorce.year" -> date.getYear.toString
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val value = form.value

        value shouldBe LocalDate.now()
      }

      "bind when date is yesterday" in {
        val yesterday = LocalDate.now().minusDays(1)


        val formInput = Map[String,String](
          "dateOfDivorce.day" -> yesterday.getDayOfMonth.toString,
          "dateOfDivorce.month" -> yesterday.getMonthOfYear.toString,
          "dateOfDivorce.year" -> yesterday.getYear.toString
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val value = form.value

        value shouldBe LocalDate.now().minusDays(1)


      }

      "bind when date is 1st January 1900" in {
        val minimumLimit = new LocalDate(1900,1,1)

        val formInput = Map[String,String](
          "dateOfDivorce.day" -> minimumLimit.getDayOfMonth.toString,
          "dateOfDivorce.month" -> minimumLimit.getMonthOfYear.toString,
          "dateOfDivorce.year" -> minimumLimit.getYear.toString
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val value = form.value

        value shouldBe LocalDate.now().minusDays(1)

      }
    }

    "not bind" when {

      "Date is in the future" in {
        val tomorrow = LocalDate.now().plusDays(1)

        val formInput = Map[String,String](
          "dateOfDivorce.day" -> tomorrow.getDayOfMonth.toString,
          "dateOfDivorce.month" -> tomorrow.getMonthOfYear.toString,
          "dateOfDivorce.year" -> tomorrow.getYear.toString
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors

        errors shouldBe Seq(Invalid(ValidationError("pages.divorce.date.error.max.date",
          LocalDate.now().plusDays(1))))
      }

      List(("day", None, today.getMonthOfYear, today.getYear),
        ("month", today.getDayOfMonth, None, today.getYear),
        ("year", today.getDayOfMonth, today.getMonthOfYear, None)).foreach {
          date =>
            s"${date._1} is not provided" in {
              val formInput = Map[String, String](
                "dateOfDivorce.day" -> date._2.toString,
                "dateOfDivorce.month" -> date._3.toString,
                "dateOfDivorce.year" -> date._4.toString
              )

              val form = DivorceSelectYearForm.form.bind(formInput)
              val errors = form.errors

              errors shouldBe Seq(Invalid(ValidationError("pages.divorce.date.error.mandatory")))
            }

      }

      "Invalid characters are input" in {
        val formInput = Map[String, String](
          "dateOfDivorce.day" -> "as",
          "dateOfDivorce.month" -> ".!",
          "dateOfDivorce.year" -> "Ωå∑π"
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors

        errors shouldBe Seq(Invalid(ValidationError("pages.divorce.date.error.non.numeric")))
      }

      "date input is before 1st January 1900" in {
        val nineteenthCentury = new LocalDate(1899, 12, 31)

        val formInput = Map[String, String](
          "dateOfDivorce.day" -> nineteenthCentury.getDayOfMonth.toString,
          "dateofDivorce.month" -> nineteenthCentury.getMonthOfYear.toString,
          "dateOfDivorce.year" -> nineteenthCentury.getYear.toString
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors


        errors shouldBe Seq(Invalid(ValidationError("pages.divorce.date.error.min.date",
          TextGenerator().ukDateTransformer(ApplicationConfig.TAMC_MIN_DATE))))
      }

      List("0", "32").foreach {
        day =>
          "day is not valid value" in {
            val formInput = Map[String, String](
              "dateOfDivorce.day" -> day,
              "dateofDivorce.month" -> today.getMonthOfYear.toString,
              "dateOfDivorce.year" -> today.getYear.toString
            )

            val form = DivorceSelectYearForm.form.bind(formInput)
            val errors = form.errors

            errors shouldBe Seq(Invalid(ValidationError("pages.divorce.date.error.invalid")))
        }
      }

      "month is not a valid value" in {
        val formInput = Map[String, String](
          "dateOfDivorce.day" -> today.getDayOfMonth.toString,
          "dateofDivorce.month" -> "13",
          "dateOfDivorce.year" -> today.getYear.toString
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors

        errors shouldBe Seq(Invalid(ValidationError("pages.divorce.date.error.invalid")))
      }
    }
  }
}
