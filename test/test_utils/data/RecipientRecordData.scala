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

package test_utils.data

import models._
import java.time.LocalDate
import test_utils.TestData.{Cids, Ninos}
import uk.gov.hmrc.domain.Nino

object RecipientRecordData {
  val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
  val userRecord: UserRecord = UserRecord(Cids.cid1, "2015", Some(true), Some(citizenName))
  val registrationFormInput: RegistrationFormInput = RegistrationFormInput("Test", "User", Gender("M"), Nino(Ninos.nino1), LocalDate.now())
  val recipientRecord: RecipientRecord = RecipientRecord(userRecord, registrationFormInput, List(TaxYear(2015, Some(false))))
}
