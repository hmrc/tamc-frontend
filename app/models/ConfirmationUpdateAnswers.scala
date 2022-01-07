/*
 * Copyright 2022 HM Revenue & Customs
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

import errors.{CacheMissingEmail, CacheMissingMAEndingDates, CacheMissingRelationshipRecords}
import java.time.LocalDate

case class ConfirmationUpdateAnswers(loggedInUserInfo: LoggedInUserInfo, divorceDate: Option[LocalDate], email: String, maEndingDates: MarriageAllowanceEndingDates)

object ConfirmationUpdateAnswers {

  def apply(cacheData: ConfirmationUpdateAnswersCacheData): ConfirmationUpdateAnswers = {

    val relationshipRecords = cacheData.relationshipRecords.getOrElse(throw CacheMissingRelationshipRecords())
    val loggedInUserInfo = relationshipRecords.loggedInUserInfo
    val divorceDate = cacheData.divorceDate
    val emailAddress = cacheData.email.getOrElse(throw CacheMissingEmail())
    val maEndingDates = cacheData.maEndingDates.getOrElse(throw CacheMissingMAEndingDates())

    ConfirmationUpdateAnswers(loggedInUserInfo, divorceDate, emailAddress, maEndingDates)
  }
}

case class ConfirmationUpdateAnswersCacheData(relationshipRecords: Option[RelationshipRecords], divorceDate: Option[LocalDate],
                                              email: Option[String], maEndingDates: Option[MarriageAllowanceEndingDates])

