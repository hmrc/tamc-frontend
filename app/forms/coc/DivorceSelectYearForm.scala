/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.data.Forms.{optional, single, tuple}
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.data.{Form, Forms, Mapping}
import play.api.i18n.Messages
import services.TimeService
import views.helpers.LanguageUtilsImpl

import java.time.LocalDate
import javax.inject.Inject
import scala.util.Try

class DivorceSelectYearForm@Inject()(applicationConfig: ApplicationConfig, timeService: TimeService, languageUtilsImpl: LanguageUtilsImpl) {

  val DateOfDivorce = "dateOfDivorce"
  val divorceDateInTheFutureError: LocalDate => Boolean = _.isAfter(timeService.getCurrentDate)
  val divorceDateAfterMinDateError: LocalDate => Boolean = _.isBefore(applicationConfig.TAMC_MIN_DATE)

  def isNonNumericDate(year: String, month: String, day: String): Boolean = !s"$year$month$day".forall(_.isDigit)
  def isNonValidDate(year: String, month: String, day: String): Boolean = {
    if(year.length != 4) true else Try(LocalDate.of(year.toInt, month.toInt, day.toInt)).isFailure
  }

  private def divorceDateMapping(implicit messages: Messages): Mapping[LocalDate] = {
    tuple(
      "year" -> optional(Forms.text),
      "month" -> optional(Forms.text),
      "day" -> optional(Forms.text)
    ).verifying {
      isValidDate
    }.transform[LocalDate](transformToDate(_), transformToTuple(_))
      .verifying(checkDateRange)
  }

  private def isValidDate = Constraint[(Option[String], Option[String], Option[String])]("valid.date"){
    case (Some(year), Some(month), Some(day)) => {

      if(isNonNumericDate(year, month, day)){
        Invalid(ValidationError("pages.divorce.date.error.non.numeric"))
      } else if(isNonValidDate(year, month, day)){
        Invalid(ValidationError("pages.divorce.date.error.invalid"))
      } else {
        Valid
      }
    }
    case _ => Invalid(ValidationError("pages.divorce.date.error.mandatory"))

  }

  private def transformToDate(dateTuple: (Option[String], Option[String], Option[String])): LocalDate = {
    val year = dateTuple._1.get
    val month = dateTuple._2.get
    val day = dateTuple._3.get

    LocalDate.of(year.toInt, month.toInt, day.toInt)
  }

  private def transformToTuple(date: LocalDate): (Option[String], Option[String], Option[String]) = {

    val year = Some(date.getYear.toString)
    val month = Some(date.getMonthValue.toString)
    val day = Some(date.getDayOfMonth.toString)

    (year, month, day)
  }

  private def checkDateRange(implicit messages: Messages): Constraint[LocalDate] = Constraint[LocalDate]("date.range") {
    case(date) if divorceDateAfterMinDateError(date) => Invalid(ValidationError("pages.divorce.date.error.min.date",
      languageUtilsImpl().ukDateTransformer(applicationConfig.TAMC_MIN_DATE)))
    case(date) if divorceDateInTheFutureError(date) =>
      Invalid(ValidationError("pages.divorce.date.error.max.date",
        languageUtilsImpl().ukDateTransformer(timeService.getCurrentDate.plusDays(1))))
    case _ => Valid
  }

  def form(implicit messages: Messages): Form[LocalDate] = Form(single(DateOfDivorce -> divorceDateMapping))

}
