/*
 * Copyright 2016 HM Revenue & Customs
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

package views.helpers

import play.api.data.Form
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import uk.gov.hmrc.time.TaxYearResolver

object TextGenerators {
  def formPageDataJourney(prefix: String, form: Form[_]): String =
    form.hasErrors match {
      case true => s"${prefix}-erroneous(${form.errors.map { x => x.key }.sorted.distinct.mkString(",")})"
      case _    => prefix
    }

  def dateTransformer(date: LocalDate): String = date.toString("dd/MM/yyyy")

  def ukDateTransformer(date: Option[LocalDate]): String = 
    date.fold("")(_.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK)))
    
  def formPossessive(noun: String): String =
    s"${noun}'s"
  
  def taxDateInterval(taxYear: Int): String =
    ukDateTransformer(Some(TaxYearResolver.startOfTaxYear(taxYear))) + " to " + ukDateTransformer(Some(TaxYearResolver.endOfTaxYear(taxYear)))
    
  def taxDateIntervalShort(taxYear: Int): String =
    TaxYearResolver.startOfTaxYear(taxYear).getYear + " to " + TaxYearResolver.endOfTaxYear(taxYear).getYear
}
