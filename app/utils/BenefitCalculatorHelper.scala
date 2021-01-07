/*
 * Copyright 2021 HM Revenue & Customs
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

package utils

import java.text.NumberFormat
import java.util.Locale

import config.ApplicationConfig._
import models._

object BenefitCalculatorHelper {

  def calculateTotalBenefitAcrossBands(income: Int, countryTaxBands: List[TaxBand]): Int = {
    val rates = countryTaxBands.map(band => band.name -> band.rate).toMap
    val basicRate = countryTaxBands.find(band => band.name == "BasicRate").head.rate
    val maxBenefit = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER() * basicRate

    val benefitsFromBandedIncome = dividedIncome(countryTaxBands, income).map(
      i => i._2 * rates(i._1)).filterNot(_ < 0).sum.toInt

    Math.min(benefitsFromBandedIncome, maxBenefit.toInt)
  }

  def dividedIncome(relevantTaxBands: List[TaxBand], incomeLessPersonalAllowance: Int): Map[String, Int] = {
    relevantTaxBands.map {
      band =>
        if (relevantTaxBands.size == 1 || band.name == relevantTaxBands.head.name)
          band.name -> Math.min(incomeLessPersonalAllowance, band.diffBetweenLowerAndUpperThreshold)
        else
          band.name -> {
            val valueOfPreviousBands = relevantTaxBands.takeWhile(
              relevantBand => relevantTaxBands.indexOf(relevantBand) < relevantTaxBands.indexOf(band)).map(
              _.diffBetweenLowerAndUpperThreshold).sum
            Math.min(
              incomeLessPersonalAllowance - valueOfPreviousBands,
              band.diffBetweenLowerAndUpperThreshold)
          }
    }.toMap
  }

  def maxLimit(country: Country): Int = country match {
    case England => MAX_LIMIT()
    case Scotland => MAX_LIMIT_SCOT()
    case Wales => MAX_LIMIT_WALES()
    case NorthernIreland => MAX_LIMIT_NORTHERN_IRELAND()
  }

  def setCurrencyFormat(country: Country, allowanceType: String): String = {
    val limit: Int = if (allowanceType == "PA") {
      PERSONAL_ALLOWANCE() + 1
    } else {
      maxLimit(country)
    }

    currencyFormatter(limit)
  }

  def currencyFormatter(limit: Int): String = {
    val formatter = NumberFormat.getCurrencyInstance(Locale.UK)
    formatter.setMaximumFractionDigits(0)
    formatter.format(limit)
  }

}