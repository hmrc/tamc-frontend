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

import utils.{LanguageUtils, TamcViewModelTest}
import viewModels.ActiveRow
import views.helpers.TextGenerators

class ActiveRowTest extends TamcViewModelTest {

  "active row" should {
    val rows = Seq(
      activeRecipientRelationshipRecord,
      activeTransferorRelationshipRecord2,
      activeTransferorRelationshipRecord3
    )
    var i = 0

    for (row <- rows) {
      i = i + 1
      s"be active row[$i] from rows" in {
        val expectedDate = TextGenerators.taxDateIntervalString(row.participant1StartDate)
        val activeRow = ActiveRow(row)
        activeRow.activeDateInterval shouldBe expectedDate
        activeRow.activeStatus shouldBe Active.asString()
      }
    }
  }

}
