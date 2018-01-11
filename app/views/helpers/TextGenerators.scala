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

package views.helpers

import play.api.data.Form
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import services.TimeService
import uk.gov.hmrc.time.TaxYearResolver
import views.helpers.WelshDateConverter._

object TextGenerators {
  def formPageDataJourney(prefix: String, form: Form[_]): String =
    form.hasErrors match {
      case true => s"${prefix}-erroneous(${form.errors.map { x => x.key}.sorted.distinct.mkString(",")})"
      case _ => prefix
    }

  def dateTransformer(date: LocalDate): String = date.toString("dd/MM/yyyy")

  def dateTransformer(date: String): LocalDate = {
    LocalDate.parse(date, DateTimeFormat.forPattern("dd-MM-yyyy"))
  }

  def dateTransformerActive(date: String): LocalDate = {
    LocalDate.parse(date, DateTimeFormat.forPattern("yyyyMMdd"))
  }

  def ukDateTransformer(date: Option[LocalDate], isWelsh: Boolean): String =
    isWelsh match {
      case false => date.fold("")(_.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK)))
      case true => welshConverted(date)
    }

  def formPossessive(noun: String, isWelsh: Boolean): String =
    isWelsh match {
      case false => s"${noun}’s"
      case true => noun
    }

  def taxDateInterval(taxYear: Int, isWelsh: Boolean): String =
    isWelsh match {
      case false => ukDateTransformer(Some(TaxYearResolver.startOfTaxYear(taxYear)), isWelsh) + " to " + ukDateTransformer(Some(TaxYearResolver.endOfTaxYear(taxYear)), isWelsh)
      case true => (ukDateTransformer(Some(TaxYearResolver.startOfTaxYear(taxYear)), isWelsh) + " i " + ukDateTransformer(Some(TaxYearResolver.endOfTaxYear(taxYear)), isWelsh))
    }

  def taxDateIntervalMultiYear(taxYear: Int,taxEndYear: Int, isWelsh: Boolean = false): String =
    isWelsh match {
      case false => TaxYearResolver.startOfTaxYear(taxYear).getYear + " to " + TaxYearResolver.endOfTaxYear(taxEndYear).getYear
      case true => (TaxYearResolver.startOfTaxYear(taxYear).getYear + " i " + TaxYearResolver.endOfTaxYear(taxEndYear).getYear)
    }

  def taxDateIntervalShort(taxYear: Int, isWelsh: Boolean = false): String =
    isWelsh match {
      case false => TaxYearResolver.startOfTaxYear(taxYear).getYear + " to " + TaxYearResolver.endOfTaxYear(taxYear).getYear
      case true => (TaxYearResolver.startOfTaxYear(taxYear).getYear + " i " + TaxYearResolver.endOfTaxYear(taxYear).getYear)
    }

  def taxDateIntervalString(taxYear: String, taxAnotherYear: Option[String] = None, isWelsh: Boolean = false): String = {
    isWelsh match
    {
      case false if(!(taxAnotherYear.isDefined)) => TaxYearResolver.startOfTaxYear(dateTransformerActive(taxYear).getYear).getYear + " to Present"
      case false => TaxYearResolver.startOfTaxYear(dateTransformer(taxYear).getYear).getYear + " to " + TaxYearResolver.endOfTaxYear(TimeService.getTaxYearForDate(dateTransformer(taxAnotherYear.get))).getYear
      case true if(!(taxAnotherYear.isDefined)) => TaxYearResolver.startOfTaxYear(dateTransformerActive(taxYear).getYear).getYear + " Presennol"
      case true => (TaxYearResolver.startOfTaxYear(dateTransformer(taxYear).getYear).getYear + " i " + TaxYearResolver.endOfTaxYear(dateTransformer(taxAnotherYear.get).getYear).getYear)
    }
}
}
