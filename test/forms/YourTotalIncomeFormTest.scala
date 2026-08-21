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

import org.scalatest.matchers.must.Matchers.{must, mustBe}
import play.api.data.{Form, FormError}
import utils.BaseTest

class YourTotalIncomeFormTest extends BaseTest {

  val form: Form[Boolean] = new YourTotalIncomeForm().apply()

  "YourTotalIncomeForm" should {
    "bind all valid options" in {
      Seq(true, false).map { value =>
        val res = form.mapping.bind(Map("yourTotalIncome" -> value.toString))
        res shouldBe Right(value)
      }
    }

    "fail to bind, with an empty value" in {
      val res = form.mapping.bind(Map("yourTotalIncome" -> value.toString))
      res shouldBe Left(
        Seq(
          FormError("yourTotalIncome", "yourTotalIncome.error.required", Nil)
        )
      )
    }
  }
}
