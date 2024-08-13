/*
 * Copyright 2024 HM Revenue & Customs
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

import config.ApplicationConfig
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

//TODO[DDCNL-3479] we may be able to delete this

class LanguageUtilsImpl@Inject()(applicationConfig: ApplicationConfig) {

  def isWelsh(messages: Messages): Boolean = messages.lang.language == applicationConfig.LANG_LANG_WELSH

  def apply()(implicit messages: Messages): LanguageUtils = {
    if(isWelsh(messages)) WelshLanguageUtils else EnglishLangaugeUtils
  }
}

sealed trait LanguageUtils {

  val taxDateIntervalString: (String, Option[String]) => String

  def separator: String
  def ukDateTransformer(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String
  def formPossessive(noun: String): String

  def formPageDataJourney(prefix: String, form: Form[_]): String =
    form.hasErrors match {
      case true => s"${prefix}-erroneous(${form.errors.map { x => x.key }.sorted.distinct.mkString(",")})"
      case _ => prefix
    }

  def dateTransformer(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

  def dateTransformer(date: String): LocalDate = {
    LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"))
  }

  def nonBreakingSpace(text: String): String = text.replace(" ", "\u00A0")

  def taxDateInterval(taxYear: Int): String = {
    ukDateTransformer(TaxYear(taxYear).starts) + separator + ukDateTransformer(TaxYear(taxYear).finishes)
  }

  def taxDateIntervalMultiYear(taxYear: Int, taxEndYear: Int): String = {
    s"$taxYear$separator${(taxEndYear + 1)}"
  }

  def taxDateIntervalShort(taxYear: Int): String = {
    s"$taxYear$separator${(taxYear + 1)}"
  }

  def taxDateIntervalGenerator(taxYear: String, taxAnotherYear: Option[String] = None, presentText: String): String = {
    taxAnotherYear.fold(
      s"${TaxYear.taxYearFor(dateTransformer(taxYear)).startYear}$presentText"
    )(syear =>
      s"${TaxYear.taxYearFor(dateTransformer(taxYear)).startYear}$separator${TaxYear.taxYearFor(dateTransformer(syear)).finishYear}"
    )
  }
}

object EnglishLangaugeUtils extends LanguageUtils {

  override def separator: String = " to "

  override def ukDateTransformer(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String = {
   val formattedDate = date.format(DateTimeFormatter.ofPattern(transformPattern).withLocale(Locale.UK))

    nonBreakingSpace(formattedDate)
  }

  override def formPossessive(noun: String): String = s"${noun}’s"

  override val taxDateIntervalString: (String, Option[String]) => String  = taxDateIntervalGenerator(_:String, _:Option[String], " to Present")
}

object WelshLanguageUtils extends LanguageUtils {

  override def separator: String = " i "

  override def ukDateTransformer(date: LocalDate, transformPattern: String = "d MMMM yyyy"): String = {

    val fetchMonthName = (localDate: LocalDate) => localDate.format(DateTimeFormatter.ofPattern("MMMM").withLocale(Locale.UK))
    val month = fetchMonthName(date)
    val endDate = date.format(DateTimeFormatter.ofPattern(transformPattern).withLocale(Locale.UK))

    nonBreakingSpace(endDate.replace(month, welshMonths(month)))
  }

  override def formPossessive(noun: String): String = noun

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

  override val taxDateIntervalString: (String, Option[String]) => String  = taxDateIntervalGenerator(_:String, _:Option[String], " i’r Presennol")

}
