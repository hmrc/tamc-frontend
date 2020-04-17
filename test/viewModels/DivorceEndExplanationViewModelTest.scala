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
import views.helpers.LanguageUtils

class DivorceEndExplanationViewModelTest extends TamcViewModelTest{

  val maEndDate = LocalDate.now()
  val paEffectiveDate = LocalDate.now()

  val currentTaxYear = TaxYear.current
  val divorceDateCurrentTaxYear = currentTaxYear.starts
  val divorceDatePreviousTaxYear = currentTaxYear.previous.starts

  def createViewModel(role: Role, divorceDate: LocalDate): DivorceEndExplanationViewModel = {
    DivorceEndExplanationViewModel(role, divorceDate, MarriageAllowanceEndingDates(maEndDate, paEffectiveDate))
  }

  def formatDate(date: LocalDate): String = LanguageUtils().ukDateTransformer(date)


  "DivorceEndExplanationViewModel" should {

    "create a view model" when {

      "a Recipient is divorced in the current tax year" in {

        val bulletStatement1 = messagesApi("pages.divorce.explanation.current.ma.bullet", formatDate(maEndDate))
        val bulletStatement2 = messagesApi("pages.divorce.explanation.current.pa.bullet", formatDate(paEffectiveDate))
        val expectedBulletStatements = (bulletStatement1, bulletStatement2)
        val expectedTaxYearStatus = messagesApi("pages.divorce.explanation.current.taxYear")

        val expectedViewModel = DivorceEndExplanationViewModel(formatDate(divorceDateCurrentTaxYear), expectedTaxYearStatus, expectedBulletStatements)

        createViewModel(Recipient, divorceDateCurrentTaxYear) shouldBe expectedViewModel

      }

      "a Recipient is divorced in a previous tax year" in {

        val bulletStatement1 = messagesApi("pages.divorce.explanation.previous.bullet", LanguageUtils().ukDateTransformer(maEndDate))
        val bulletStatement2 = messagesApi("pages.divorce.explanation.adjust.code.bullet")
        val expectedBulletStatements = (bulletStatement1, bulletStatement2)
        val expectedTaxYearStatus = messagesApi("pages.divorce.explanation.previous.taxYear")

        val expectedViewModel = DivorceEndExplanationViewModel(formatDate(divorceDatePreviousTaxYear), expectedTaxYearStatus, expectedBulletStatements)

        createViewModel(Recipient, divorceDatePreviousTaxYear) shouldBe expectedViewModel

      }

      "a transferor is divorced in the current tax year" in {

        val bulletStatement1 = messagesApi("pages.divorce.explanation.previous.bullet", LanguageUtils().ukDateTransformer(maEndDate))
        val bulletStatement2 = messagesApi("pages.divorce.explanation.adjust.code.bullet")
        val expectedBulletStatements = (bulletStatement1, bulletStatement2)
        val expectedTaxYearStatus = messagesApi("pages.divorce.explanation.current.taxYear")

        val expectedViewModel = DivorceEndExplanationViewModel(formatDate(divorceDateCurrentTaxYear), expectedTaxYearStatus, expectedBulletStatements)

        createViewModel(Transferor, divorceDateCurrentTaxYear) shouldBe expectedViewModel

      }

      "a transferor is divorced in a previous tax year" in {

        val bulletStatement1 = messagesApi("pages.divorce.explanation.previous.bullet", LanguageUtils().ukDateTransformer(maEndDate))
        val bulletStatement2 = messagesApi("pages.divorce.explanation.adjust.code.bullet")
        val expectedBulletStatements = (bulletStatement1, bulletStatement2)
        val expectedTaxYearStatus = messagesApi("pages.divorce.explanation.previous.taxYear")

        val expectedViewModel = DivorceEndExplanationViewModel(formatDate(divorceDatePreviousTaxYear), expectedTaxYearStatus, expectedBulletStatements)

        createViewModel(Transferor, divorceDatePreviousTaxYear) shouldBe expectedViewModel

      }
    }
  }
}
