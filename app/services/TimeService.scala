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

package services

import config.ApplicationConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import uk.gov.hmrc.time.TaxYear

//TODO[DDCNL-3479] look into making this a util or deleting. It Is Not a Service!
object TimeService extends TimeService

trait TimeService {

  def isFutureDate(date: LocalDate): Boolean =
    date.isAfter(getCurrentDate)

  def getCurrentDate: LocalDate =
    LocalDate.now()

  def getCurrentTaxYear: Int =
    TaxYear.current.startYear

  def getTaxYearForDate(date: LocalDate): Int =
    TaxYear.taxYearFor(date).startYear

  def getStartDateForTaxYear(year: Int): LocalDate =
    TaxYear.firstDayOfTaxYear(year)

  def getPreviousYearDate: LocalDate =
    LocalDate.now().minusYears(1)

  def parseDateWithFormat(date: String, format: String = "yyyyMMdd"): LocalDate =
    LocalDate.parse(date, DateTimeFormatter.ofPattern(format))

  /**
    * TODO Need to change and call this method right before send list of years
    * TODO in cache itself and return only valid years from cache and use these years after
    * @param years - cached years
    * @return valid years to apply for MA
    */
  def getValidYearsApplyMAPreviousYears(years: Option[List[models.TaxYear]]): List[models.TaxYear] = {
    years.fold(List[models.TaxYear]()) {
      actualYears =>
        actualYears.filter(year => year.year >= ApplicationConfig.appConfig.TAMC_BEGINNING_YEAR)
    }
  }

}