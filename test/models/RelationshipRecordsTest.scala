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

import utils.TamcViewModelTest

class RelationshipRecordsTest extends TamcViewModelTest {

  "recordStatus" should {
    "be active" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeRecipientRelationshipRecord,
        activeTransferorRelationshipRecord2
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.recordStatus shouldBe Active
    }

    "be active historic" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeTransferorRelationshipRecord3,
        inactiveRecipientRelationshipRecord1,
        inactiveRecipientRelationshipRecord2,
        inactiveRecipientRelationshipRecord3
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.recordStatus shouldBe ActiveHistoric
    }

    "be historic" in {
      val relationshipRecordList = RelationshipRecordList(Seq())
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.recordStatus shouldBe Historic
    }
  }

  "role" should {
    "be active Recipient" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeRecipientRelationshipRecord
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role shouldBe Recipient
    }
    "be active Transferor" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        activeTransferorRelationshipRecord2
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role shouldBe Transferor
    }

    "be historic Recipient" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        inactiveRecipientRelationshipRecord1,
        inactiveRecipientRelationshipRecord2,
        inactiveRecipientRelationshipRecord3
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role shouldBe Recipient
    }

    "be historic Transferor" in {
      val relationshipRecordList = RelationshipRecordList(Seq(
        inactiveTransferorRelationshipRecord1,
        inactiveTransferorRelationshipRecord2,
        inactiveTransferorRelationshipRecord3
      ))
      val relationship = RelationshipRecords(relationshipRecordList)

      relationship.role shouldBe Transferor
    }

    //TODO to test properly
    "failed to get role no active and no historic records and no user info" in {
      intercept[Exception] {
        new RelationshipRecords(None, None, None).role
      }.getMessage shouldBe "IDK?!"
    }

  }


}
