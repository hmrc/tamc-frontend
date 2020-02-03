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

package viewModels

import models.{Recipient, Role, Transferor}
import org.joda.time.LocalDate
import uk.gov.hmrc.time.TaxYear

class DivorceEndExplanationViewModelTest extends ViewModelBaseSpec {

  "DivorceEndExplanationViewModel" when {
    val roles: Seq[Role] = Seq(Recipient, Transferor)

    for (role <- roles) {
      val className = role.getClass.getSimpleName
      s"$className" when {

        s"current year divorce page($className, current year)" in {
          val targetYear = TaxYear.current.startYear
          val date = new LocalDate(targetYear, 5, 15)
          val actual = DivorceEndExplanationViewModel(role, date)

          val currentTaxYear = TaxYear.current
          val expectedBullets = bulletStatements(role, currentTaxYear, isCurrentYearDivorced = true).toArray
          val bullets = (expectedBullets.head, expectedBullets.last)
          val taxYearStatus = messagesApi("pages.divorce.explanation.current.taxYear")
          val expected = DivorceEndExplanationViewModel(transformDate(date), taxYearStatus, bullets)

          actual shouldBe expected
        }

        s"prev year divorce page($className, prev year)" in {
          val targetYear = LocalDate.now().minusYears(2).getYear
          val date = new LocalDate(targetYear, 5, 15)
          val actual = DivorceEndExplanationViewModel(role, date)

          val currentTaxYear = TaxYear(targetYear + 1)
          val expectedBullets = bulletStatements(role, currentTaxYear, isCurrentYearDivorced = false).toArray
          val bullets = (expectedBullets.head, expectedBullets.last)
          val taxYearStatus = messagesApi("pages.divorce.explanation.previous.taxYear")
          val expected = DivorceEndExplanationViewModel(transformDate(date), taxYearStatus, bullets)

          actual shouldBe expected
        }

      }

    }
  }

}
