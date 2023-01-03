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

package models

import errors.UnknownParticipant
import utils.UnitSpec

class RoleTest extends UnitSpec {

  val participantsAndRoles = Seq(("Recipient", Recipient), ("Transferor", Transferor))

  "Role" should {
    "return the correct role" when {
      participantsAndRoles foreach { participantAndRole =>
        s"the participant is ${participantAndRole._1}" in {
          Role(participantAndRole._1) shouldBe participantAndRole._2
        }
      }
    }

    "return an error if the participant is unknown" in {
      a[UnknownParticipant] shouldBe thrownBy(Role("Receiver"))
    }

    "return a String representation" when {
      participantsAndRoles foreach { participantAndRole =>
        s"the role is ${participantAndRole._2}" in {
          participantAndRole._2.value shouldBe participantAndRole._1
         }
      }
    }
  }
}
