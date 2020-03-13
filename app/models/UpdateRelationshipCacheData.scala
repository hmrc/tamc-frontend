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

import errors.{CacheMissingEmail, CacheMissingEndReason, CacheMissingMAEndingDates, CacheMissingRelationshipRecords}
import org.joda.time.LocalDate

case class UpdateRelationshipCacheData(relationshipRecords: RelationshipRecords, email: String, endMaReason: String, marriageEndDate: LocalDate)

//TODO test
object UpdateRelationshipCacheData {

  def apply(relationshipRecords: Option[RelationshipRecords], email: Option[String], endReason: Option[String],
            marriageAllowanceEndingDate: Option[MarriageAllowanceEndingDates]): UpdateRelationshipCacheData = {

    (relationshipRecords, email, endReason, marriageAllowanceEndingDate) match {

      case(Some(relationshipRecords), Some(email), Some(endReason), Some(endDates)) => {
        val desEndReason = EndMarriageAllowanceReason.asDesEnumeration(endReason)
        UpdateRelationshipCacheData(relationshipRecords, email, desEndReason, endDates.marriageAllowanceEndDate)
      }
      case(None, _, _, _) => throw CacheMissingRelationshipRecords()
      case(_, None, _, _) => throw CacheMissingEmail()
      case(_, _, None, _) => throw CacheMissingEndReason()
      case(_, _, _, None) => throw CacheMissingMAEndingDates()
    }
  }

}
