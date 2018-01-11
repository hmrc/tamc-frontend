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

package services

import org.joda.time.LocalDate
import uk.gov.hmrc.time.TaxYearResolver
import models.EndRelationshipReason
import models.EndReasonCode
import org.joda.time.format.DateTimeFormat

object TimeService extends TimeService {
  
  override val taxYearResolver = TaxYearResolver
   
}

trait TimeService {
  
  val taxYearResolver: TaxYearResolver
  
  def getEffectiveUntilDate(endReason: EndRelationshipReason): Option[LocalDate] = 
    endReason.endReason match {
      case EndReasonCode.CANCEL => Some(taxYearResolver.endOfCurrentTaxYear)
      case EndReasonCode.DIVORCE_CY => Some(taxYearResolver.endOfTaxYear(taxYearResolver.taxYearFor(endReason.dateOfDivorce.get)))
      case EndReasonCode.DIVORCE_PY => None
    }

  def getEffectiveDate(endReason: EndRelationshipReason): LocalDate =
    endReason.endReason match {
      case EndReasonCode.CANCEL => taxYearResolver.startOfNextTaxYear
      case EndReasonCode.DIVORCE_CY => taxYearResolver.endOfTaxYear(taxYearResolver.taxYearFor(endReason.dateOfDivorce.get)).plusDays(1)
      case EndReasonCode.DIVORCE_PY => taxYearResolver.startOfTaxYear(taxYearResolver.taxYearFor(endReason.dateOfDivorce.get))
    }
  
  def getCurrentDate = LocalDate.now()
    
  def getCurrentTaxYear = taxYearResolver.currentTaxYear
    
  def getTaxYearForDate(date: LocalDate) = taxYearResolver.taxYearFor(date)
    
  def getStartDateForTaxYear(year: Int) = taxYearResolver.startOfTaxYear(year)

  def getPreviousYearDate() = LocalDate.now().minusYears(1)

  def parseDateWtihFormat(date: String, format: String) = LocalDate.parse(date, DateTimeFormat.forPattern(format))
}
