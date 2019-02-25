/*
 * Copyright 2019 HM Revenue & Customs
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

import java.util.Locale

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import uk.gov.hmrc.time.TaxYear
import views.helpers.WelshDateConverter._

object TextGenerators {

  def separator(isWelsh: Boolean): String = if (!isWelsh) " to " else " i "

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

  def taxDateInterval(taxYear: Int, isWelsh: Boolean): String = {
    ukDateTransformer(Some(TaxYear(taxYear).starts), isWelsh) + separator(isWelsh) + ukDateTransformer(Some(TaxYear(taxYear).finishes), isWelsh)
  }

  def taxDateIntervalMultiYear(taxYear: Int,taxEndYear: Int, isWelsh: Boolean = false): String = {
    taxYear + separator(isWelsh) + (taxEndYear + 1)
  }

  def taxDateIntervalShort(taxYear: Int, isWelsh: Boolean = false): String = {
    taxYear + separator(isWelsh) + (taxYear + 1)
  }

  def taxDateIntervalString(taxYear: String, taxAnotherYear: Option[String] = None, isWelsh: Boolean = false): String = {
    val presentText = if(isWelsh) " i’r Presennol" else " to Present"
    taxAnotherYear.fold(
      TaxYear(dateTransformerActive(taxYear).getYear).startYear + presentText
    )(syear =>
      TaxYear(dateTransformer(taxYear).getYear).startYear + separator(isWelsh) + TaxYear.taxYearFor(dateTransformer(syear)).finishYear
    )
  }
}
