/*
 * Copyright 2024 HM Revenue & Customs
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

import models.RelationshipRecord
import org.junit.Assert.assertTrue
import org.scalatest.BeforeAndAfterEach
import utils.BaseTest
import play.api.inject.bind
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class ApplicationServiceTest extends BaseTest with BeforeAndAfterEach {
  val mockTimeService: TimeService = mock[TimeService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TimeService].toInstance(mockTimeService)
    ).build()

  val service: ApplicationService = instanceOf[ApplicationService]
  lazy val relationshipRecord: RelationshipRecord = RelationshipRecord("Recipient", "creationTimestamp", "20220101", None, None, "otherPaticipant", "otherParticipantupdateTimestamp")
  lazy val relationshipRecord1: RelationshipRecord = RelationshipRecord("Recipient", "creationTimestamp", "20220101", None, None, "otherPaticipant", "otherParticipantupdateTimestamp")


  "canApplyForCurrentYears" should {
    "return true" in {
      assertTrue(service.canApplyForCurrentYears(Option(Seq(relationshipRecord)), Option(relationshipRecord)))
    }
  }

  "canApplyForMarriageAllowance" should {
    "return true" when {
      "canApplyForPreviousYears or canApplyForCurrentYears is true" in {
        assertTrue(service.canApplyForMarriageAllowance(Option(Seq(relationshipRecord)), Option(relationshipRecord)))
      }
    }
  }


}
