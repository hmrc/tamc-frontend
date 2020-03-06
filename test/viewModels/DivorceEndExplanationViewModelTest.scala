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

import models.{MarriageAllowanceEndingDates, Recipient, Role, Transferor}
import org.joda.time.LocalDate
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest
import views.helpers.TextGenerator

class DivorceEndExplanationViewModelTest extends TamcViewModelTest {


  lazy val date: Int => LocalDate = year => new LocalDate(year, 5, 15)
  lazy val maEndDate = TaxYear.current.finishes
  lazy val paEffectiveDate = TaxYear.current.starts


  "DivorceEndExplanationViewModel" when {
    val roles: Seq[Role] = Seq(Recipient, Transferor)

    for (role <- roles) {
      val className = role.getClass.getSimpleName
      s"$className" when {

        s"current year divorce page($className, current year)" in {
          val targetYear = TaxYear.current.startYear
          val actual = DivorceEndExplanationViewModel(role, date(targetYear), MarriageAllowanceEndingDates(maEndDate, paEffectiveDate))

          val string1 = messagesApi("pages.divorce.explanation.current.bullet1", TextGenerator().ukDateTransformer(maEndDate))
          val string2 = messagesApi("pages.divorce.explanation.current.bullet2", TextGenerator().ukDateTransformer(paEffectiveDate))
          val bullets = (string1, string2)
          val taxYearStatus = messagesApi("pages.divorce.explanation.current.taxYear")
          val expected = DivorceEndExplanationViewModel(TextGenerator().ukDateTransformer(date(targetYear)), taxYearStatus, bullets)

          actual shouldBe expected
        }

        s"prev year divorce page($className, prev year)" in {
          val targetYear = LocalDate.now().minusYears(2).getYear
          val actual = DivorceEndExplanationViewModel(role, date(targetYear), MarriageAllowanceEndingDates(maEndDate, paEffectiveDate))

          val string1 = messagesApi("pages.divorce.explanation.previous.bullet1", TextGenerator().ukDateTransformer(maEndDate))
          val string2 = messagesApi("pages.divorce.explanation.previous.bullet2")
          val bullets = (string1, string2)
          val taxYearStatus = messagesApi("pages.divorce.explanation.previous.taxYear")
          val expected = DivorceEndExplanationViewModel(TextGenerator().ukDateTransformer(date(targetYear)), taxYearStatus, bullets)

          actual shouldBe expected
        }

      }

    }
  }

}
