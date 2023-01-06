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

package utils

import com.google.inject.Inject
import config.ApplicationConfig
import models.{Country, England, NorthernIreland, Scotland, TaxBand, Wales}
import uk.gov.hmrc.time.TaxYear

trait TaxBandReader {
  def read(countryOfResidence: Country, taxYear: TaxYear): List[TaxBand]
}

class TaxBandReaderImpl @Inject()(config: ApplicationConfig) extends TaxBandReader {

  override def read(countryOfResidence: Country, taxYear: TaxYear): List[TaxBand] = {
    countryOfResidence match {
      case England => readByCountryAndTaxYear("england", taxYear.startYear)
      case Scotland => readByCountryAndTaxYear("scotland", taxYear.startYear)
      case Wales => readByCountryAndTaxYear("wales", taxYear.startYear)
      case NorthernIreland => readByCountryAndTaxYear("northern-ireland", taxYear.startYear)
    }
  }

  private def readByCountryAndTaxYear(country: String, taxYear: Int): List[TaxBand] = {
    country match {
      case "scotland" => {
        List[TaxBand](
          config.getTaxBand("StarterRate", "starter-rate", country.toLowerCase(), taxYear),
          config.getTaxBand("BasicRate", "basic-rate", country.toLowerCase(), taxYear),
          config.getTaxBand("IntermediateRate", "intermediate-rate", country.toLowerCase(), taxYear)
        )
      }
      case _ => {
        List[TaxBand](
          config.getTaxBand("BasicRate", "basic-rate", country.toLowerCase(), taxYear)
        )
      }
    }
  }
}
