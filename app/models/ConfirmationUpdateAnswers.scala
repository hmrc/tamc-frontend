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

package models

import org.joda.time.LocalDate
import play.api.mvc.Result


case class ConfirmationUpdateAnswers(fullName: String, divorceDate: Option[LocalDate], email: String, maEndDate: LocalDate, paEffectiveDate: LocalDate) {

  def apply(cacheData: ConfirmationUpdateAnswersCacheData): ConfirmationUpdateAnswers = {

    val name = cacheData.fullName.getOrElse(throw new RuntimeException("fullName value not returned from cache"))
    val divorceDate = cacheData.divorceDate
    val emailAddress = cacheData.email.getOrElse(throw new RuntimeException("email value not returned from cache"))
    val endDate = cacheData.maEndDate.getOrElse(throw new RuntimeException("maEndDate value not returned from cache"))
    val effectiveDate = cacheData.paEffectiveDate.getOrElse(throw new RuntimeException("paEffectiveDate value not returned from cache"))

    ConfirmationUpdateAnswers(name, divorceDate, emailAddress, endDate, effectiveDate)
  }
}

case class ConfirmationUpdateAnswersCacheData(fullName: Option[String], divorceDate: Option[LocalDate], email: Option[String], maEndDate: Option[LocalDate], paEffectiveDate: Option[LocalDate])
