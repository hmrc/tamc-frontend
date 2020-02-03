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
import play.api.data.Form
import play.api.data.Forms.single
import play.api.data.validation.{Constraint, Invalid, Valid, ValidationError}
import play.api.i18n.Messages
import services.TimeService
import uk.gov.hmrc.play.mappers.DateTuple.dateTuple
import utils.Constants.forms.coc.DivorceSelectYearFormConstants

//TODO add tests
object DivorceSelectYearForm {

  def form(implicit messages: Messages): Form[Option[LocalDate]] = Form[Option[LocalDate]](
    //TODO error message
    single(DivorceSelectYearFormConstants.DateOfDivorce -> dateTuple().verifying(checkDateRange()))
  )

  //TODO does this need tidying
  private def checkDateRange(): Constraint[Option[LocalDate]] = Constraint[Option[LocalDate]]("date.range") {
    case None =>
      Invalid(ValidationError("pages.form.field.dod.error.required"))
    case Some(date) if date.isAfter(TimeService.getCurrentDate) =>
      Invalid(ValidationError("pages.form.field.dom.error.max-date", date.toString("dd/MM/yyyy")))
    case Some(date) if date.isBefore(ApplicationConfig.TAMC_MIN_DATE.plusDays(-1)) =>
      Invalid(ValidationError("pages.form.field.dom.error.min-date"))
    case _ =>
      Valid
  }
}
