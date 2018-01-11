/*
 * Copyright 2018 HM Revenue & Customs
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

import scala.Left
import scala.Right
import play.api.data.Form
import play.api.data.FormError
import play.api.data.Forms.mapping
import play.api.data.Forms.of
import play.api.data.format.Formatter
import models.MultiYearIncomeFormInput

object MultiYearIncomeCheckForm {

  private val PRE_ERROR_KEY = "pages.form.field-required."
  implicit def requiredBooleanFormatter: Formatter[Boolean] = new Formatter[Boolean] {
    override val format = Some(("format.boolean", Nil))
    def bind(key: String, data: Map[String, String]) = {
      Right(data.get(key).getOrElse("")).right.flatMap {
        case "true"  => Right(true)
        case "false" => Right(false)
        case _       => Left(Seq(FormError(key, PRE_ERROR_KEY + key, Nil)))
      }
    }
    def unbind(key: String, value: Boolean) = Map(key -> value.toString)
  }

  val incomeCheckForm = Form[MultiYearIncomeFormInput](
    mapping(
      "multiyear-transferor-income-criteria" -> of(requiredBooleanFormatter),
      "multiyear-recipient-income-criteria" -> of(requiredBooleanFormatter))(MultiYearIncomeFormInput.apply)(MultiYearIncomeFormInput.unapply))
}
