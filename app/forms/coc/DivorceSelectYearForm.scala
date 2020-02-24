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

package forms.coc

import config.ApplicationConfig
import org.joda.time.LocalDate
import play.api.data.Forms.{optional, single, text, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Mapping}
import play.api.i18n.Messages
import services.TimeService
import uk.gov.hmrc.play.mappers.DateFields.{day, month, year}
import views.helpers.TextGenerator

import scala.util.Try

//TODO add tests
object DivorceSelectYearForm {

  val DateOfDivorce = "dateOfDivorce"
  val divorceDateInTheFutureError: LocalDate => Boolean = _.isAfter(TimeService.getCurrentDate)
  val divorceDateAfterMinDateError: LocalDate => Boolean = _.isBefore(ApplicationConfig.TAMC_MIN_DATE)

  def isNonNumericDate(date: String): Boolean = !date.forall(_.isDigit)
  def isNonValidDate(date: (String, String, String)): Boolean = Try(new LocalDate(date._1.trim.toInt, date._2.trim.toInt, date._3.trim.toInt)).isFailure

  private def divorceDateMapping(implicit messages: Messages): Mapping[LocalDate] = {
    tuple(
        year -> optional(text),
        month -> optional(text),
        day -> optional(text)
    ).verifying {
      isValidDate
    }.transform[LocalDate](transformToDate(_), transformToTuple(_))
    .verifying(checkDateRange)
  }

  private def isValidDate = Constraint[(Option[String], Option[String], Option[String])]("valid.date"){
    case (Some(year), Some(month), Some(day)) => {
      val completeDate = (year, month, day)
      val nonNumericDate = s"$year$month$day"

      if(isNonNumericDate(nonNumericDate)){
        Invalid(ValidationError("pages.divorce.date.error.non.numeric"))
      } else if(isNonValidDate(completeDate)){
        Invalid(ValidationError("pages.divorce.date.error.invalid"))
      } else {
        Valid
      }
    }
    case _ => Invalid(ValidationError("pages.divorce.date.error.mandatory"))

  }

  private def transformToDate(dateTuple: (Option[String], Option[String], Option[String])): LocalDate = {
    dateTuple match {
      case (Some(year), Some(month), Some(day)) =>  new LocalDate(year.toInt, month.toInt, day.toInt)
    }
  }

  private def transformToTuple(date: LocalDate): (Option[String], Option[String], Option[String]) = {

    val year = Some(date.getYear.toString)
    val month = Some(date.getMonthOfYear.toString)
    val day = Some(date.getDayOfMonth.toString)

    (year, month, day)
  }

  private def checkDateRange(implicit messages: Messages): Constraint[LocalDate] = Constraint[LocalDate]("date.range") {
    case(date) if divorceDateAfterMinDateError(date) => Invalid(ValidationError("pages.divorce.date.error.min.date", TextGenerator().ukDateTransformer(ApplicationConfig.TAMC_MIN_DATE)))
    case(date) if divorceDateInTheFutureError(date) =>
      Invalid(ValidationError("pages.divorce.date.error.max.date",TextGenerator().ukDateTransformer(TimeService.getCurrentDate)))
    case _ => Valid
  }

  def form(implicit messages: Messages): Form[LocalDate] = Form(single(DateOfDivorce -> divorceDateMapping))

}
