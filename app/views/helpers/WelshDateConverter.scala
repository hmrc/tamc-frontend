package views.helpers

import java.util.Locale

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.play.views.formatting.Dates._
import org.joda.time.DateTime

object WelshDateConverter {

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

  val welshWeekDays = Map(
    "Monday" -> "Dydd Llun",
    "Tuesday" -> "Dydd Mawrth",
    "Wednesday" -> "Dydd Mercher",
    "Thursday" -> "Dydd Iau",
    "Friday" -> "Dydd Gwener",
    "Saturday" -> "Dydd Sadwrn",
    "Sunday" -> "Dydd Sul"
  )


  val fetchMonthName = (localDate: LocalDate) => localDate.toString(DateTimeFormat.forPattern("MMMM").withLocale(Locale.UK))
  val fetchDayName = (localDate: LocalDate) => localDate.toString(DateTimeFormat.forPattern("EEEE").withLocale(Locale.UK))

  def welshConverted(date: Option[LocalDate]): String =
    date.fold("") { localDate =>
      val month = fetchMonthName(localDate)
      val enDate = localDate.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))
      enDate.replaceAll(month, welshMonths.get(month).get)
    }

  def welshConvertedDateTime(date: Option[DateTime]): String =
    date.fold("") { localDate =>
      val month = fetchMonthName(localDate.toLocalDate)
      val day = fetchDayName(localDate.toLocalDate)
      val enDate = formatEasyReadingTimestamp(date, "")
      enDate.replaceAll(month, welshMonths.get(month).get).replaceAll(day, welshWeekDays.get(day).get)
    }
}
