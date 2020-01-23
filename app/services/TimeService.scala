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

import models.{EndReasonCode, EndRelationshipReason}
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import uk.gov.hmrc.time.TaxYear

object TimeService extends TimeService {
  //TODO can we make other way?
  override val defaultDateFormat: String = "yyyyMMdd"
}

trait TimeService {
  val defaultDateFormat: String = "yyyyMMdd"

  def isFutureDate(date: String): Boolean = {
    var res = false
    val format = new java.text.SimpleDateFormat(defaultDateFormat)
    try {
      val time = format.parse(date).getTime
      res = time > System.currentTimeMillis()
    } catch {
      case exp: Throwable => Logger.error(s"Failed to parse date [$date] into format [$defaultDateFormat]", exp)
    }

    res
  }

  def currentTaxYear: TaxYear = TaxYear.current

  def getEffectiveUntilDate(endReason: EndRelationshipReason): Option[LocalDate] =
    endReason.endReason match {
      case EndReasonCode.CANCEL => Some(currentTaxYear.finishes)
      case EndReasonCode.DIVORCE_CY => Some(TaxYear.taxYearFor(endReason.dateOfDivorce.get).finishes)
      case EndReasonCode.DIVORCE_PY => None
    }

  def getEffectiveDate(endReason: EndRelationshipReason): LocalDate =
    endReason.endReason match {
      case EndReasonCode.CANCEL => currentTaxYear.next.starts
      case EndReasonCode.DIVORCE_CY => TaxYear.taxYearFor(endReason.dateOfDivorce.get).finishes.plusDays(1)
      case EndReasonCode.DIVORCE_PY => TaxYear.taxYearFor(endReason.dateOfDivorce.get).starts
    }

  def getCurrentDate: LocalDate = LocalDate.now()

  def getCurrentTaxYear: Int = currentTaxYear.startYear

  def getTaxYearForDate(date: LocalDate): Int = TaxYear.taxYearFor(date).startYear

  def getStartDateForTaxYear(year: Int): LocalDate = TaxYear.firstDayOfTaxYear(year)

  def getPreviousYearDate: LocalDate = LocalDate.now().minusYears(1)

  def parseDateWithFormat(date: String, format: String  = TimeService.defaultDateFormat): LocalDate = LocalDate.parse(date, DateTimeFormat.forPattern(format))
}
