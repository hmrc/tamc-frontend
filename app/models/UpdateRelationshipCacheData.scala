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

package models

import errors.{CacheMissingEmail, CacheMissingEndReason, CacheMissingMAEndingDates, CacheMissingRelationshipRecords}
import java.time.LocalDate

case class UpdateRelationshipCacheData(relationshipRecords: Option[RelationshipRecords], email: Option[String], endMaReason: Option[String], marriageEndDate: Option[LocalDate])

case class UpdateRelationshipData(relationshipRecords: RelationshipRecords, email: String, endMaReason: String, marriageEndDate: LocalDate)

object UpdateRelationshipData {

  def apply(updateRelationshipCacheData: UpdateRelationshipCacheData): UpdateRelationshipData = {

    updateRelationshipCacheData match {

      case UpdateRelationshipCacheData(Some(relationshipRecords), Some(email), Some(endReason), Some(endDates)) => {
        UpdateRelationshipData(relationshipRecords, email, endReason, endDates)
      }
      case UpdateRelationshipCacheData(None, _, _, _) => throw CacheMissingRelationshipRecords()
      case UpdateRelationshipCacheData(_, None, _, _) => throw CacheMissingEmail()
      case UpdateRelationshipCacheData(_, _, None, _) => throw CacheMissingEndReason()
      case UpdateRelationshipCacheData(_, _, _, None) => throw CacheMissingMAEndingDates()
    }
  }
}