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

package test_utils.data

import models._
import java.time.LocalDate
import test_utils.TestData.Ninos
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

object ConfirmationModelData {
  val citizenName = Some(CitizenName(Some("Test"), Some("User")))
  val emailAddress = EmailAddress("test@test.com")

  val dateOfMarriageFormInput = DateOfMarriageFormInput(LocalDate.now().minusDays(1))

  val confirmationModelData = ConfirmationModel(citizenName, emailAddress, "Test", "User", Nino(Ninos.nino1),
    List(TaxYear(2015, None)), dateOfMarriageFormInput)

  val updateRelationshipConfirmationModel =
    UpdateRelationshipConfirmationModel(citizenName, emailAddress, EndRelationshipReason("DIVORCE_PY"))
}
