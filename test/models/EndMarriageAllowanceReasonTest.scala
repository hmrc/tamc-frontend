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
import uk.gov.hmrc.play.test.UnitSpec

class EndMarriageAllowanceReasonTest extends UnitSpec {

  "EndMarriageAllowanceReason" should {

    "return a DESEnumeration value for a given marriage allowance endReason" when {

      val endReasonsWithEnumerations = Seq(("Divorce", "Divorce/Separation"), ("Cancel", "Cancelled by Transferor"))

      endReasonsWithEnumerations foreach { reasonAndEnumeration =>
        s"the reason is $reasonAndEnumeration" in {
          EndMarriageAllowanceReason.asDesEnumeration(reasonAndEnumeration._1) shouldBe reasonAndEnumeration._2
        }
      }
    }

    "return an exception if the value to convert is unsupported" in {
      a[DesEnumerationNotFound] shouldBe thrownBy(EndMarriageAllowanceReason.asDesEnumeration("Earnings"))
    }
  }
}
