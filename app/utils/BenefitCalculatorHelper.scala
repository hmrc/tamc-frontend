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

package utils

import config.ApplicationConfig._
import models.{Country, TaxBand}

object BenefitCalculatorHelper {

  def calculateTotalBenefitAcrossBands(income: Int, country: Country, countryTaxbands: List[TaxBand]): Int = {
    val incomeOverPersonalAllowance = income - PERSONAL_ALLOWANCE
    val relevantTaxBands = countryTaxbands.filterNot(band => income < band.lowerThreshold)
    val rates = relevantTaxBands.map(band => band.name -> band.rate).toMap

    val benefitsFromBandedIncome = dividedIncome(relevantTaxBands, incomeOverPersonalAllowance).map(
      income => income._2 * rates(income._1)).sum.toInt
    Math.min(benefitsFromBandedIncome, MAX_BENEFIT)
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

}