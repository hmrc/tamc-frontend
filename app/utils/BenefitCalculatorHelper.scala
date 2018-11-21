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
import models.TaxBand

object BenefitCalculatorHelper {

  def calculateTotalBenefitAcrossBands(income: Int, countryTaxbands: List[TaxBand]): Int = {
    val rates = countryTaxbands.map(band => band.name -> band.rate).toMap
    val basicRate = countryTaxbands.find(band => band.name == "BasicRate").head.rate
    val maxBenefit = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER * basicRate

    val benefitsFromBandedIncome = dividedIncome(countryTaxbands, income).map(
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

}