/*
 * Copyright 2025 HM Revenue & Customs
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

import models.ApplyForEligibleYears
import play.api.data.Form
import utils.BaseTest
import org.scalatest.matchers.must.Matchers.{must, mustBe}

class ChooseYearFormTest extends BaseTest {

  val form: Form[Seq[String]] = new ChooseYearForm().apply()

  "ChooseYearForm" should {
    "bind all valid options" in {
      val allOptions = ApplyForEligibleYears.values.map(_.toString)
      val data: Map[String, String] = allOptions.zipWithIndex.map {
        case (value, index) => s"value[$index]" -> value
      }.toMap

      val boundForm = form.bind(data)
      boundForm.errors.mustBe(empty)
      boundForm.value.mustBe(Some(allOptions))
    }

    "fail to bind, with an empty value" in {
      val data = Map("value" -> "")

      val boundForm = form.bind(data)
      boundForm.errors must have length 1
      boundForm.errors.head.message mustBe "pages.chooseYears.error.required"
    }

    "fail to bind, with an invalid value" in {
      val data = Map("value[0]" -> "invalidOption")

      val boundForm = form.bind(data)
      boundForm.errors must have length 1
      boundForm.errors.head.message mustBe "pages.chooseYears.error.invalid"
    }
  }
}
