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

  val maEndDate = TaxYear.current.finishes
  val paEffectiveDate = TaxYear.current.starts
  val currentTaxYearStart = TaxYear.current.startYear

  "DivorceEndExplanationViewModel" should {

    "create a view model" when {

      "the divorce date is in the current tax year" in {

        val divorceDateCurrentTaxYear = TaxYear.current.starts
        val viewModel = DivorceEndExplanationViewModel(divorceDateCurrentTaxYear, MarriageAllowanceEndingDates(maEndDate, paEffectiveDate))

        val bulletStatement1 = messagesApi("pages.divorce.explanation.current.bullet1", TextGenerator().ukDateTransformer(maEndDate))
        val bulletStatement2 = messagesApi("pages.divorce.explanation.current.bullet2", TextGenerator().ukDateTransformer(paEffectiveDate))
        val expectedBulletStatements = (bulletStatement1, bulletStatement2)
        val expectedTaxYearStatus = messagesApi("pages.divorce.explanation.current.taxYear")

        viewModel.divorceDate shouldBe TextGenerator().ukDateTransformer(divorceDateCurrentTaxYear)
        viewModel.taxYearStatus shouldBe expectedTaxYearStatus
        viewModel.bulletStatement shouldBe expectedBulletStatements

      }

      "the divorce date is in a previous tax year" in {

        val divorceDatePreviousTaxYear = TaxYear.current.previous.finishes
        val viewModel = DivorceEndExplanationViewModel(divorceDatePreviousTaxYear, MarriageAllowanceEndingDates(maEndDate, paEffectiveDate))

        val bulletStatement1 = messagesApi("pages.divorce.explanation.previous.bullet1", TextGenerator().ukDateTransformer(maEndDate))
        val bulletStatement2 = messagesApi("pages.divorce.explanation.previous.bullet2")
        val expectedBulletStatements = (bulletStatement1, bulletStatement2)
        val expectedTaxYearStatus = messagesApi("pages.divorce.explanation.previous.taxYear")

        viewModel.divorceDate shouldBe TextGenerator().ukDateTransformer(divorceDatePreviousTaxYear)
        viewModel.taxYearStatus shouldBe expectedTaxYearStatus
        viewModel.bulletStatement shouldBe expectedBulletStatements

      }
    }
  }
}
