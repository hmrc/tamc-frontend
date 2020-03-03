/*
 * Copyright 2020 HM Revenue & Customs
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

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.time.TaxYear

object TimeService extends TimeService {
  //TODO can we make other way?
  override val defaultDateFormat: String = "yyyyMMdd"
}

trait TimeService {
  val defaultDateFormat: String = "yyyyMMdd"

  def isFutureDate(date: LocalDate): Boolean =
    date.isAfter(getCurrentDate)

  def getCurrentDate: LocalDate =
    LocalDate.now()

  def getCurrentTaxYear: Int =
    currentTaxYear.startYear

  private def currentTaxYear: TaxYear =
    TaxYear.current

  def getTaxYearForDate(date: LocalDate): Int =
    TaxYear.taxYearFor(date).startYear

  def getStartDateForTaxYear(year: Int): LocalDate =
    TaxYear.firstDayOfTaxYear(year)

  def getPreviousYearDate: LocalDate =
    LocalDate.now().minusYears(1)

  def parseDateWithFormat(date: String, format: String = TimeService.defaultDateFormat): LocalDate =
    LocalDate.parse(date, DateTimeFormat.forPattern(format))
}
