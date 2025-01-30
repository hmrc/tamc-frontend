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

package utils

import config.ApplicationConfig

import java.text.NumberFormat
import java.util.Locale
import models._

import javax.inject.Inject

class BenefitCalculatorHelper@Inject()(applicationConfig: ApplicationConfig) {

  def calculateTotalBenefitAcrossBands(income: BigDecimal, countryTaxBands: List[TaxBand]): Int = {
    val rates = countryTaxBands.map(band => band.name -> band.rate).toMap
    val basicRate = countryTaxBands.find(band => band.name == "BasicRate").head.rate
    val maxBenefit = applicationConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER() * basicRate

    val benefitsFromBandedIncome = dividedIncome(countryTaxBands, income).map(
      i => i._2 * rates(i._1)).filterNot(_ < 0).sum.toInt

    Math.min(benefitsFromBandedIncome, maxBenefit.toInt)
  }

  private def dividedIncome(relevantTaxBands: List[TaxBand], incomeLessPersonalAllowance: BigDecimal): Map[String, BigDecimal] = {
    relevantTaxBands.map {
      band =>
        if (relevantTaxBands.size == 1 || band.name == relevantTaxBands.head.name) {
          band.name -> incomeLessPersonalAllowance.min(band.diffBetweenLowerAndUpperThreshold)
        } else
          band.name -> {
            val valueOfPreviousBands = relevantTaxBands.takeWhile(
              relevantBand => relevantTaxBands.indexOf(relevantBand) < relevantTaxBands.indexOf(band)).map(
              _.diffBetweenLowerAndUpperThreshold).sum
            (incomeLessPersonalAllowance - valueOfPreviousBands).min(band.diffBetweenLowerAndUpperThreshold)
          }
    }.toMap
  }

  def maxLimit(country: Country): Int = country match {
    case England => applicationConfig.MAX_LIMIT()
    case Scotland => applicationConfig.MAX_LIMIT_SCOT()
    case Wales => applicationConfig.MAX_LIMIT_WALES()
    case NorthernIreland => applicationConfig.MAX_LIMIT_NORTHERN_IRELAND()
  }

  def setCurrencyFormat(country: Country, allowanceType: String): String = {
    val limit: Int = if (allowanceType == "PA") {
      applicationConfig.PERSONAL_ALLOWANCE() + 1
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