/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import com.google.inject.Inject
import config.ApplicationConfig
import models.{DesRelationshipEndReason, RelationshipRecord}

class ApplicationService @Inject()(
                                    timeService: TimeService,
                                    appConfig: ApplicationConfig
                                  )  {

  def canApplyForMarriageAllowance(
                                    historicRelationships: Option[Seq[RelationshipRecord]],
                                    activeRelationship: Option[RelationshipRecord],
                                    startingFromTaxYear: Int = timeService.getEarliestValidYear()): Boolean =
    canApplyForPreviousYears(historicRelationships, activeRelationship, startingFromTaxYear) ||
      canApplyForCurrentYears(historicRelationships, activeRelationship)

  def canApplyForCurrentYears(
                               historicRelationships: Option[Seq[RelationshipRecord]],
                               activeRelationship: Option[RelationshipRecord]): Boolean =
    !taxYearsThatAreUnavailableForApplication(historicRelationships, activeRelationship).contains(timeService.getCurrentTaxYear)

  def canApplyForPreviousYears(
                                historicRelationships: Option[Seq[RelationshipRecord]],
                                activeRelationship: Option[RelationshipRecord],
                                startingFromTaxYear: Int = timeService.getEarliestValidYear()): Boolean = {
    val startYear = Math.max(startingFromTaxYear, timeService.getEarliestValidYear())
    val availableYears: Set[Int] = (startYear until timeService.getCurrentTaxYear).toSet
    val unavailableYears: Set[Int] = taxYearsThatAreUnavailableForApplication(historicRelationships, activeRelationship)
    (availableYears -- unavailableYears).nonEmpty
  }

  private def taxYearsThatAreUnavailableForApplication(historicRelationships: Option[Seq[RelationshipRecord]],
                                                       activeRelationship: Option[RelationshipRecord]): Set[Int] = {
    val historicYears: Set[Set[Int]] = historicRelationships.getOrElse(Seq[RelationshipRecord]()).toSet.filter {
      relationship =>
        val unavailableReasonCodes = List(
          Some(DesRelationshipEndReason.Divorce),
          Some(DesRelationshipEndReason.Cancelled),
          Some(DesRelationshipEndReason.Merger),
          Some(DesRelationshipEndReason.Retrospective)
        )
        unavailableReasonCodes contains relationship.relationshipEndReason
    }.map {
      _.overlappingTaxYears(appConfig.currentTaxYear().startYear)
    }

    val activeYears: Set[Int] = activeRelationship.map {
      _.overlappingTaxYears(appConfig.currentTaxYear().startYear)
    }.getOrElse(Set[Int]())
    val allYears: Set[Set[Int]] = historicYears + activeYears
    allYears.flatten
  }
}
