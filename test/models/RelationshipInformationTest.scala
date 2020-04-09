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

import errors.DesEnumerationNotFound
import org.joda.time.LocalDate
import uk.gov.hmrc.play.test.UnitSpec

class RelationshipInformationTest extends UnitSpec {

  val creationTimestamp = "20191212105512"
  val endDate = LocalDate.now()

  "RelationshipInformation" should {

    "create a RelationshipInformation object" in {

      val relationshipEndReason = "Divorce"
      val expectedResult = RelationshipInformation(creationTimestamp, "Divorce/Separation", endDate.toString("yyyyMMdd"))

      RelationshipInformation(creationTimestamp, relationshipEndReason, endDate) shouldBe expectedResult

    }


    "return a DESEnumeration value for a given marriage allowance endReason" when {

      val endReasonsWithEnumerations = Seq(("Divorce", "Divorce/Separation"), ("Cancel", "Cancelled by Transferor"))

      endReasonsWithEnumerations foreach { reasonAndEnumeration =>
        s"the reason is ${reasonAndEnumeration._1}" in {
          RelationshipInformation(creationTimestamp, reasonAndEnumeration._1, endDate).relationshipEndReason shouldBe
            reasonAndEnumeration._2
        }
      }
    }

    "return an exception if the value to convert is unsupported" in {
      a[DesEnumerationNotFound] shouldBe thrownBy(RelationshipInformation(creationTimestamp, "Earnings", endDate))
    }

  }
}
