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

case class ScottishBandedIncome(incomeAtStarterRate: Int, incomeAtBasicRate: Int = 0, incomeAtIntermediateRate: Int = 0) extends BandedIncome {
  val starterRate = 0.19
  val basicRate = 0.20
  val intermediateRate = 0.21

  val starterIncomeBenefit: Double = incomeAtStarterRate * starterRate
  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate
  val intermediateIncomeBenefit: Double = incomeAtIntermediateRate * intermediateRate
}

case class EnglishBandedIncome(incomeAtBasicRate: Int) extends BandedIncome {
  val basicRate = 0.20

  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate
}

case class WelshBandedIncome(incomeAtBasicRate: Int) extends BandedIncome {
  val basicRate = 0.20

  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate
}

case class NorthernIrelandBandedIncome(incomeAtBasicRate: Int) extends BandedIncome {
  val basicRate = 0.20

  val basicIncomeBenefit: Double = incomeAtBasicRate * basicRate
}

object BandedIncome {

  def incomeChunker(income: Int, country: Country): BandedIncome = {
    val incomeOverPersonalAllowance = income - PERSONAL_ALLOWANCE
    country match {
      case Scotland =>
        val diffBetweenStarterAndBasicRates = 2000
        val diffBetweenBasicAndIntermediateRates = 10150
        val starterRateIncome = Math.min(diffBetweenStarterAndBasicRates, incomeOverPersonalAllowance)

        if(starterRateIncome < diffBetweenStarterAndBasicRates) {
          ScottishBandedIncome(starterRateIncome)
        } else {
          val basicRateIncome = incomeOverPersonalAllowance - diffBetweenStarterAndBasicRates
          if(basicRateIncome > diffBetweenBasicAndIntermediateRates) {
            val intermediateRateIncome = basicRateIncome - diffBetweenBasicAndIntermediateRates
            ScottishBandedIncome(starterRateIncome,basicRateIncome - intermediateRateIncome, intermediateRateIncome)
          } else {
            ScottishBandedIncome(starterRateIncome, basicRateIncome)
          }
        }
      case England => EnglishBandedIncome(income - PERSONAL_ALLOWANCE)
      case Wales => WelshBandedIncome(income - PERSONAL_ALLOWANCE)
      case NorthernIreland => NorthernIrelandBandedIncome(income - PERSONAL_ALLOWANCE)
    }
  }
}