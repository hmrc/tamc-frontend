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

import models.EligibilityCalculatorInput
import play.api.data.Forms.mapping
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError, Forms}

import java.text.DecimalFormat

object EligibilityCalculatorForm {

  private def incomeStringWithoutSeparator(incomeString: String): String = {
    val moneyFormatSimple = """^\s*£?\s?([0-9]+(?:\.\d{2})?)\s*$""".r //£0.56 or £1234.56
    val moneyFormatBritish = """^\s*£?\s?([1-9]\d{0,2}(?:,\d{3})*(?:\.\d{2})?)\s*$""".r //£1,234.56
    val moneyFormatContinental = """^\s*£?\s?([1-9]\d{0,2}(?:[ .]\d{3})*(?:,\d{2})?)\s*$""".r //£1 234,56 or £1.234,56

    incomeString match {
      case moneyFormatSimple(poundsTotal) => poundsTotal
      case moneyFormatBritish(poundsTotal) => poundsTotal.replaceAll(",", "")
      case moneyFormatContinental(poundsTotal) => poundsTotal.replaceAll(" .", "")
      case _ => throw new NumberFormatException
    }
  }

  private val incomeFormatter: Formatter[BigDecimal] = new Formatter[BigDecimal] {
    private def getErrorMessageFromMessagesFile(fieldKey: String, messageKey: String): String =
      s"pages.form.$messageKey.$fieldKey"

    private def formatDecimalValue (value: BigDecimal) = {
      val decimalFormat = new DecimalFormat("####")
      decimalFormat.format(value)
    }

    override def unbind(key: String, value: BigDecimal): Map[String, String] =
      Map(key -> formatDecimalValue(value))

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
      data.get(key) match {
        case Some(num) if num.trim().isEmpty => Left(Seq(FormError(key, getErrorMessageFromMessagesFile(key, "field-required"))))
        case Some(num) =>
          try {
            val income = BigDecimal(incomeStringWithoutSeparator(num))

            if (income.isValidInt) {
              Right(income.toInt)
            }
            else {
              Left(Seq(FormError(key, getErrorMessageFromMessagesFile(key, "field-invalid"))))
            }

          }
          catch {
            case e: NumberFormatException => Left(Seq(FormError(key, getErrorMessageFromMessagesFile(key, "field-invalid"))))
          }
        case _ => Left(Seq(FormError(key, getErrorMessageFromMessagesFile(key, "field-required"))))
      }
  }

  private val countryFormatter = new Formatter[String] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(c) if c.trim().isEmpty => Left(Seq(FormError(key, "pages.form.field-required.country")))
        case Some(c) if Set("england", "wales", "scotland", "northernireland").contains(c.trim()) => Right(c)
        case Some(_) => Left(Seq(FormError(key, "pages.form.field-required.country")))
        case None => Left(Seq(FormError(key, "pages.form.field-required.country")))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
  }

  private val income: FieldMapping[BigDecimal] = Forms.of[BigDecimal](incomeFormatter)
  val country: FieldMapping[String] = Forms.of[String](countryFormatter)

  val calculatorForm: Form[EligibilityCalculatorInput] = Form[EligibilityCalculatorInput](
    mapping(
      "country" -> country,
      "transferor-income" -> income,
      "recipient-income" -> income
    )(EligibilityCalculatorInput.apply)(EligibilityCalculatorInput.unapply)
  )
}
