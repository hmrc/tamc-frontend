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

package models

import config.ApplicationConfig._

sealed trait BandedIncome

case class ScottishBandedIncome(incomeAtStarterRate: Int, incomeAtBasicRate: Int = 0,
                                incomeAtIntermediateRate: Int = 0, bands: List[TaxBand]) extends BandedIncome {
  val starterRate = bands.find(x=> x.name=="StarterRate").map(_.rate)
  val basicRate = bands.find(x=> x.name=="BasicRate").map(_.rate)
  val intermediateRate = bands.find(x=> x.name=="IntermediateRate").map(_.rate)

  val starterIncomeBenefit: Double = incomeAtStarterRate * starterRate.get
  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate.get
  val intermediateIncomeBenefit: Double = incomeAtIntermediateRate * intermediateRate.get
}

case class EnglishBandedIncome(incomeAtBasicRate: Int, bands: List[TaxBand]) extends BandedIncome {
  val basicRate = bands.find(x=> x.name=="BasicRate").map(_.rate)

  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate.get
}

case class WelshBandedIncome(incomeAtBasicRate: Int, bands: List[TaxBand]) extends BandedIncome {
  val basicRate = bands.find(x=> x.name=="BasicRate").map(_.rate)

  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate.get
}

case class NorthernIrelandBandedIncome(incomeAtBasicRate: Int, bands: List[TaxBand]) extends BandedIncome {
  val basicRate = bands.find(x=> x.name=="BasicRate").map(_.rate)

  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate.get
}

object BandedIncome {

  def incomeChunker(income: Int, country: Country, countryTaxbands: List[TaxBand]): BandedIncome = {
    val incomeOverPersonalAllowance = income - PERSONAL_ALLOWANCE
    country match {
      case Scotland => {

        // TODO - don't use .get and refactor code duplication
        val starterRateBand = countryTaxbands.find(x=> x.name=="StarterRate").get
        val basicRateBand = countryTaxbands.find(x=> x.name=="BasicRate").get
        val intermediateRateBand = countryTaxbands.find(x=> x.name=="IntermediateRate").get

        val diffBetweenStarterAndBasicRates = basicRateBand.lowerThreshold-starterRateBand.lowerThreshold
        val diffBetweenBasicAndIntermediateRates = intermediateRateBand.lowerThreshold-basicRateBand.lowerThreshold
        val starterRateIncome = Math.min(diffBetweenStarterAndBasicRates, incomeOverPersonalAllowance)

        if (starterRateIncome < diffBetweenStarterAndBasicRates) {
          ScottishBandedIncome(starterRateIncome, bands = countryTaxbands)
        } else {
          val basicRateIncome = incomeOverPersonalAllowance - diffBetweenStarterAndBasicRates
          if (basicRateIncome > diffBetweenBasicAndIntermediateRates) {
            val intermediateRateIncome = basicRateIncome - diffBetweenBasicAndIntermediateRates
            ScottishBandedIncome(starterRateIncome, basicRateIncome - intermediateRateIncome, intermediateRateIncome, countryTaxbands)
          } else {
            ScottishBandedIncome(starterRateIncome, basicRateIncome, bands = countryTaxbands)
          }
        }
      }
      case England => EnglishBandedIncome(income - PERSONAL_ALLOWANCE,
        bands = countryTaxbands)
      case Wales => WelshBandedIncome(income - PERSONAL_ALLOWANCE,
        bands = countryTaxbands)
      case NorthernIreland => NorthernIrelandBandedIncome(income - PERSONAL_ALLOWANCE,
        bands = countryTaxbands)
    }
  }
}

