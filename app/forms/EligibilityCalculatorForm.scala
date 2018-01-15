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
import scala.util.Success
import scala.util.Try
import models.EligibilityCalculatorInput
import play.api.data.Form
import play.api.data.FormError
import play.api.data.Forms.mapping
import play.api.data.Forms.of
import play.api.data.format.Formatter
import play.api.data.Forms
import uk.gov.hmrc.play.views.helpers.MoneyPounds

object EligibilityCalculatorForm {

  def cleanMoneyString(moneyString: String): String = {
    val moneyFormatSimple = """^(?:\s*)(?:\£?)(?:\s?)(0|[1-9]\d*)(?:\.\d{2})?(?:\s*)$""".r //£0.56 or £1234.56
    val moneyFormatBritish = """^(?:\s*)(?:\£?)(?:\s?)([1-9]\d{0,2}(?:\,\d{3})*)(?:\.\d{2})?(?:\s*)$""".r //£1,234.56
    val moneyFormatContinental = """^(?:\s*)(?:\£?)(?:\s?)([1-9]\d{0,2}(?:[ \.]\d{3})*)(?:\,\d{2})?(?:\s*)$""".r //£1 234,56 or £1.234,56

    moneyString match {
      case moneyFormatSimple(poundsTotal)      => poundsTotal
      case moneyFormatBritish(poundsTotal)     => poundsTotal.replaceAll(""",""", "")
      case moneyFormatContinental(poundsTotal) => poundsTotal.replaceAll("""[ \.]""", "")
      case _                                   => throw new NumberFormatException
    }
  }

  val currencyFormatter = new Formatter[Int] {
    private def messageCustomizer(fieldKey: String, messageKey: String): String = s"pages.form.field.${fieldKey}.error.${messageKey}"
    override def unbind(key: String, value: Int): Map[String, String] =
      Map(key -> MoneyPounds(value, 0).quantity)
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
      data.get(key) match {
        case Some(num) if num.trim().isEmpty => Left(Seq(FormError(key, messageCustomizer(key, "field-required"))))
        case Some(num) =>
          try {
            val bigDecimalMoney = BigDecimal(cleanMoneyString(num.trim()))
            if (bigDecimalMoney.isValidInt) {
              Right(bigDecimalMoney.toInt)
            } else {
              Left(Seq(FormError(key, messageCustomizer(key, "field-invalid"))))
            }
          } catch {
            case e: NumberFormatException => {
              Left(Seq(FormError(key, messageCustomizer(key, "field-invalid"))))
            }
          }
        case _ => Left(Seq(FormError(key, messageCustomizer(key, "field-required"))))
      }
  }

  val currency = Forms.of[Int](currencyFormatter)

  val calculatorForm = Form[EligibilityCalculatorInput](
    mapping(
      "transferor-income" -> currency,
      "recipient-income" -> currency)(EligibilityCalculatorInput.apply)(EligibilityCalculatorInput.unapply))
}
