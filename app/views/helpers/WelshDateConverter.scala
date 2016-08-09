package views.helpers

import java.util.Locale

import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat

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


  def welshConverted(date: Option[LocalDate]): String =
    date.fold("") { localDate =>
      val month = localDate.toString(DateTimeFormat.forPattern("MMMM").withLocale(Locale.UK))
      val enDate = localDate.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))
      enDate.replaceAll(month, welshMonths.get(month).get)
    }
}
