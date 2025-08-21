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
import play.api.data.*
import play.api.data.Forms.*

class ChooseYearForm {
  def apply(): Form[Seq[String]] = Form(
    mapping(
      "value" -> seq(text)
        .verifying("pages.chooseYears.error.required", _.nonEmpty)
        .verifying("pages.chooseYears.error.invalid", answers => answers.forall(valid))
    )(identity)(Some(_))
  )

  def valid(answer: String): Boolean =
    ApplyForEligibleYears.values.map(_.toString).contains(answer)
}
