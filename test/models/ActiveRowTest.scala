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
import viewModels.ActiveRow

class ActiveRowTest extends TamcViewModelTest {

  "active row" should {
    Seq(
      (activeRecipientRelationshipRecord, "2012 to Present"),
      (activeTransferorRelationshipRecord3, "2012 to 2020")
    ).foreach { row =>
      s"be active ${row._2}" in {
        val expectedDate = row._2
        val activeRow = ActiveRow(row._1)
        activeRow.activeDateInterval shouldBe expectedDate
        activeRow.activeStatus shouldBe "Active"
      }
    }
  }

}
