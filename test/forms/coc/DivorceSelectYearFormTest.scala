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
import play.api.test.FakeRequest

import scala.collection.mutable

class DivorceSelectYearFormTest extends FormsBaseSpec {

  "DivorceSelectYearForm" should {
    val decisions = Seq(
      MakeChangesDecisionForm.Divorce,
      MakeChangesDecisionForm.IncomeChanges,
      MakeChangesDecisionForm.NoLongerRequired,
      MakeChangesDecisionForm.Bereavement
    )

    "bind date in future" in {
      val input = Some(LocalDate.now().plusYears(10))

      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> input.get.getDayOfMonth.toString,
        "dateOfDivorce.month" -> input.get.getMonthOfYear.toString,
        "dateOfDivorce.year" -> input.get.getYear.toString
      )

      val form = DivorceSelectYearForm.form.bindFromRequest()(request)
      val errors = form.errors
      val value = form.value

      errors shouldBe Seq(
        FormError(
          DivorceSelectYearForm.DateOfDivorce,
          Seq("pages.form.field.dom.error.max-date"),
          Seq(input.get.toString("dd/MM/yyyy")).toArray
        )
      )
      value shouldBe None
    }

    "bind valid date" in {
      val input = Some(LocalDate.now())

      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> input.get.getDayOfMonth.toString,
        "dateOfDivorce.month" -> input.get.getMonthOfYear.toString,
        "dateOfDivorce.year" -> input.get.getYear.toString
      )

      val form = DivorceSelectYearForm.form.bindFromRequest()(request)
      val errors = form.errors
      val value = form.value

      errors shouldBe Seq()
      value shouldBe Some(input)
    }

    "bind date in past" in {
      val input = Some(ApplicationConfig.TAMC_MIN_DATE.minusYears(1))

      val request = FakeRequest().withFormUrlEncodedBody(
        "dateOfDivorce.day" -> input.get.getDayOfMonth.toString,
        "dateOfDivorce.month" -> input.get.getMonthOfYear.toString,
        "dateOfDivorce.year" -> input.get.getYear.toString
      )

      val form = DivorceSelectYearForm.form.bindFromRequest()(request)
      val errors = form.errors
      val value = form.value

      errors shouldBe Seq(
        FormError(
          DivorceSelectYearForm.DateOfDivorce,
          Seq("pages.form.field.dom.error.min-date"),
          mutable.WrappedArray.empty
        )
      )
      value shouldBe None
    }

    //TODO add more
    val invalidDecisions = Seq(
      ""
    )
    for (decision <- invalidDecisions) {
      s"bind a invalid decision <- '$decision'" in {
        val formInput = Map[String, String](
          DivorceSelectYearForm.DateOfDivorce -> decision
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe Seq(
          FormError(
            DivorceSelectYearForm.DateOfDivorce,
            //TODO update after John will fix
            Seq("pages.form.field.dod.error.required"),
            mutable.WrappedArray.empty
          )
        )
        value shouldBe formInput
      }
    }

    //TODO add more
    for (decision <- decisions) {
      s"bind a invalid key decision <- '$decision'" in {
        val formInput = Map[String, String](
          "tralala" -> decision
        )

        val form = DivorceSelectYearForm.form.bind(formInput)
        val errors = form.errors
        val value = form.data

        errors shouldBe List(FormError("dateOfDivorce", List("pages.form.field.dod.error.required"), mutable.WrappedArray.empty))
        value shouldBe formInput
        form.value shouldBe None
      }
    }

  }
}
