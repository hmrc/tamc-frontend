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
    val bandDifferences = relevantTaxBands.map(band => band.name -> (band.upperThreshold - band.lowerThreshold)).toMap
    val rates = relevantTaxBands.map(band => band.name -> band.rate).toMap

    val benefitsFromBandedIncome = dividedIncome(relevantTaxBands, incomeOverPersonalAllowance,
      bandDifferences).map(income => income._2 * rates(income._1)).sum.toInt

    Math.min(benefitsFromBandedIncome, MAX_BENEFIT)
  }

  def dividedIncome(relevantTaxBands: List[TaxBand], incomeLessPersonalAllowance: Int,
                    bandDifferences: Map[String, Int]): Map[String, Int] = {
    relevantTaxBands.map {
      band =>
        if (relevantTaxBands.size == 1)
          band.name -> Math.min(incomeLessPersonalAllowance, band.upperThreshold - band.lowerThreshold)
        else if (band.name == relevantTaxBands.last.name)
          band.name -> {
          Math.min(bandDifferences.values.take(bandDifferences.values.size).sum - incomeLessPersonalAllowance,
            band.upperThreshold - band.lowerThreshold)
        } else
          band.name -> Math.min(incomeLessPersonalAllowance, band.upperThreshold - band.lowerThreshold)
    }.toMap
  }

}