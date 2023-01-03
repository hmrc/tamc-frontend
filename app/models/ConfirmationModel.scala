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


import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress

case class ConfirmationModel(
                              transferorFullName: Option[CitizenName],
                              transferorEmail: EmailAddress,
                              recipientFirstName: String,
                              recipientLastName: String,
                              recipientNino: Nino,
                              availableYears: List[TaxYear],
                              dateOfMarriage: DateOfMarriageFormInput)

case class UpdateRelationshipConfirmationModel(fullName: Option[CitizenName], email: EmailAddress, endRelationshipReason: EndRelationshipReason, historicRelationships: Option[Seq[RelationshipRecord]] = None, role: Option[String] = None)
