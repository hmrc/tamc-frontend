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

package views.helpers

import java.util.Locale

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.time.TaxYear
import utils.LanguageUtils.isWelsh

object TextGenerator {

  def apply()(implicit messages: Messages): TextGenerator = {
    if(isWelsh(messages)) WelshTextGenerator else EnglishTextGenerator
  }
}

sealed trait TextGenerator {


  def separator: String
  def ukDateTransformer(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String

  def formPageDataJourney(prefix: String, form: Form[_]): String =
    form.hasErrors match {
      case true => s"${prefix}-erroneous(${form.errors.map { x => x.key }.sorted.distinct.mkString(",")})"
      case _ => prefix
    }

  def dateTransformer(date: LocalDate): String = date.toString("dd/MM/yyyy")

  def dateTransformer(date: String): LocalDate = {
    LocalDate.parse(date, DateTimeFormat.forPattern("yyyyMMdd"))
  }

  def nonBreakingSpace(text: String): String = text.replace(" ", "\u00A0")

  def taxDateInterval(taxYear: Int)(implicit messages: Messages): String = {
    ukDateTransformer(TaxYear(taxYear).starts) + separator + ukDateTransformer(TaxYear(taxYear).finishes)
  }

  def taxDateIntervalMultiYear(taxYear: Int, taxEndYear: Int): String = {
    taxYear + separator + (taxEndYear + 1)
  }

  def taxDateIntervalShort(taxYear: Int): String = {
    taxYear + separator + (taxYear + 1)
  }

  def taxDateIntervalGenerator(taxYear: String, taxAnotherYear: Option[String] = None, presentText: String): String = {
    taxAnotherYear.fold(
      TaxYear.taxYearFor(dateTransformer(taxYear)).startYear + presentText
    )(syear =>
      TaxYear.taxYearFor(dateTransformer(taxYear)).startYear + separator + TaxYear.taxYearFor(dateTransformer(syear)).finishYear
    )
  }
}

object EnglishTextGenerator extends TextGenerator {

  override def separator: String = " to "

  override def ukDateTransformer(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String = {
   val formattedDate = date.toString(DateTimeFormat.forPattern(transformPattern).withLocale(Locale.UK))

    nonBreakingSpace(formattedDate)
  }

  val taxDateIntervalString: (String, Option[String]) => String  = taxDateIntervalGenerator(_:String, _:Option[String], " to Present")
}

object WelshTextGenerator extends TextGenerator {

  override def separator: String = " i "

  override def ukDateTransformer(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String = {
    nonBreakingSpace(welshConverted(date, transformPattern))
  }

  val welshMonths = Map(
    "January" -> "Ionawr",
    "February" -> "Chwefror",
    "March" -> "Mawrth",
    "April" -> "Ebrill",
    "May" -> "Mai",
    "June" -> "Mehefin",
    "July" -> "Gorffennaf",
    "August" -> "Awst",
    "September" -> "Medi",
    "October" -> "Hydref",
    "November" -> "Tachwedd",
    "December" -> "Rhagfyr"
  )

  def welshConverted(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String = {
    val fetchMonthName = (localDate: LocalDate) => localDate.toString(DateTimeFormat.forPattern("MMMM").withLocale(Locale.UK))
    val month = fetchMonthName(date)
    val enDate = date.toString(DateTimeFormat.forPattern(transformPattern).withLocale(Locale.UK))

    date.toString.replace(month, welshMonths.get(month).get)
  }

  val taxDateIntervalString: (String, Option[String]) => String  = taxDateIntervalGenerator(_:String, _:Option[String], " i’r Presennol")

}