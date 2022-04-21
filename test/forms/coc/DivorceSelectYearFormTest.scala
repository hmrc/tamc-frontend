/*
 * Copyright 2022 HM Revenue & Customs
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
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.data.FormError
import play.api.i18n.{Lang, MessagesApi, MessagesImpl, MessagesProvider}
import play.api.test.Injecting
import utils.UnitSpec

import java.time.LocalDate

class DivorceSelectYearFormTest extends UnitSpec with GuiceOneServerPerSuite with Injecting {

  lazy val config: ApplicationConfig = inject[ApplicationConfig]
  lazy val today = config.currentLocalDate()
  val defaultDate = LocalDate.of(2000, 1, 1)
  implicit lazy val provider: MessagesProvider = MessagesImpl(Lang("en"), inject[MessagesApi])

  val divorceSelectYearForm = inject[DivorceSelectYearForm]

  "DivorceSelectYearForm" should {
    "bind" when {
      "Todays date is input as the divorce date" in {
        val formInput = createDivorceDateInput(today)
        val form = divorceSelectYearForm.form.bind(formInput)
        val value = form.value.getOrElse(defaultDate)

        value shouldBe today
        form.errors shouldBe empty
      }

      "the divorce date entered is yesterday" in {
        val yesterday = LocalDate.now().minusDays(1)
        val formInput = createDivorceDateInput(yesterday)
        val form = divorceSelectYearForm.form.bind(formInput)
        val value = form.value.getOrElse(defaultDate)

        value shouldBe yesterday
        form.errors shouldBe empty

      }

      "the divorce date entered is 1st January 1900" in {
        val minimumLimit = LocalDate.of(1900, 1, 1)
        val formInput = createDivorceDateInput(minimumLimit)
        val form = divorceSelectYearForm.form.bind(formInput)
        val value = form.value.getOrElse(defaultDate)

        value shouldBe minimumLimit
        form.errors shouldBe empty
      }
    }

    "not bind" when {

      "the divorce date entered is in the future" in {
        val tomorrow = today.plusDays(1)
        val formInput = createDivorceDateInput(tomorrow)
        val form = divorceSelectYearForm.form.bind(formInput)
        val errorMessageKey = extractErrorMessageKey(form.errors)

       errorMessageKey shouldBe "pages.divorce.date.error.max.date"

      }

      "an element of the divorce date has not been provided" when {

        val datesWithLeadingTitle = List(
          ("day", "", today.getMonthValue, today.getYear),
          ("month", today.getDayOfMonth, "", today.getYear),
          ("year", today.getDayOfMonth, today.getMonthValue, "")
        )

        datesWithLeadingTitle.foreach {
          date =>
            s"${date._1} is not provided" in {

              val formInput = createDivorceDateInput(date._2.toString, date._3.toString, date._4.toString)
              val form = divorceSelectYearForm.form.bind(formInput)
              val errorMessageKey = extractErrorMessageKey(form.errors)

              errorMessageKey shouldBe "pages.divorce.date.error.mandatory"
            }
        }

      }

      "Invalid characters are input" in {

        val formInput = createDivorceDateInput("as", ".!", "Ωå∑π")
        val form = divorceSelectYearForm.form.bind(formInput)
        val errorMessageKey = extractErrorMessageKey(form.errors)

        errorMessageKey shouldBe "pages.divorce.date.error.non.numeric"

      }

      "divorce date input is before 1st January 1900" in {
        val nineteenthCentury = LocalDate.of(1899, 12, 31)
        val formInput = createDivorceDateInput(nineteenthCentury)
        val form = divorceSelectYearForm.form.bind(formInput)
        val errorMessageKey = extractErrorMessageKey(form.errors)
        errorMessageKey shouldBe "pages.divorce.date.error.min.date"


      }

      "an invalid day is entered" when {

        List("0", "32").foreach { day =>
          s"$day is not valid value" in {

            val formInput = createDivorceDateInput(day, today.getMonthValue.toString, today.getYear.toString)
            val form = divorceSelectYearForm.form.bind(formInput)
            val errorMessageKey = extractErrorMessageKey(form.errors)
            errorMessageKey shouldBe "pages.divorce.date.error.invalid"

          }
        }
      }

      "month is not a valid value" in {

        val formInput = createDivorceDateInput(today.getDayOfMonth.toString, "13", today.getYear.toString)
        val form = divorceSelectYearForm.form.bind(formInput)
        val errorMessageKey = extractErrorMessageKey(form.errors)
        errorMessageKey shouldBe "pages.divorce.date.error.invalid"

      }

      "year is not a valid value" in {

        val formInput = createDivorceDateInput(today.getDayOfMonth.toString, today.getMonthValue.toString, "19")
        val form = divorceSelectYearForm.form.bind(formInput)
        val errorMessageKey = extractErrorMessageKey(form.errors)
        errorMessageKey shouldBe "pages.divorce.date.error.invalid"
      }
    }
  }

  private def createDivorceDateInput(divorceDate: LocalDate): Map[String, String] = {
    createDivorceDateInput(divorceDate.getDayOfMonth.toString, divorceDate.getMonthValue.toString, divorceDate.getYear.toString)
  }

  private def createDivorceDateInput(day: String, month: String, year: String): Map[String, String] =
    Map[String, String](
      "dateOfDivorce.day" -> day,
      "dateOfDivorce.month" -> month,
      "dateOfDivorce.year" -> year
    )

  private def extractErrorMessageKey(errors: Seq[FormError]): String = {
    errors match {
      case List(FormError(_, List(messageKey), _)) => messageKey
      case _ => throw new RuntimeException("Unable to extract error message key")
    }
  }
}
