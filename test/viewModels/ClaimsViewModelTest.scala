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

import java.util.Locale

import config.ApplicationConfig._
import _root_.config.ApplicationConfig
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear


class ClaimsViewModelTest extends ViewModelBaseSpec {

  lazy val currentOfTaxYear: Int = TaxYear.current.currentYear
  lazy val endOfTaxYear: LocalDate = TaxYear.current.finishes
  lazy val maxPATransfer: Int = PERSONAL_ALLOWANCE(currentOfTaxYear)
  lazy val maxBenefit: Int = MAX_BENEFIT(currentOfTaxYear)
  lazy val maxPaTransferFormatted: Int = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(currentOfTaxYear)
  lazy val formattedEndOfTaxYear: String = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

  val taxFreeHtml: Html =
    Html(
      s"""${messages("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messages("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)

  val backURL: String = controllers.routes.UpdateRelationshipController.history().url

  "ClaimsViewModel" should {

    "should not display historic rows" when {
      "recipient has primary (without end date) and does not have non-primary records" in {
        val claimsView = ClaimsViewModel(activeRecipientRelationshipRecord, Seq())

        claimsView.activeRow shouldBe ActiveRow(activeRecipientRelationshipRecord)
        claimsView.historicRows shouldBe None
        claimsView.backLinkUrl shouldBe backURL
        claimsView.taxFreeAllowanceLink shouldBe taxFreeHtml
      }

      "transferor has primary (without end date) and does not have non-primary records" in {
        val claimsView = ClaimsViewModel(activeTransferorRelationshipRecord2, Seq())

        claimsView.activeRow shouldBe ActiveRow(activeTransferorRelationshipRecord2)
        claimsView.historicRows shouldBe None
        claimsView.backLinkUrl shouldBe backURL
        claimsView.taxFreeAllowanceLink shouldBe taxFreeHtml
      }

      "transferor has primary (with end date) and has non-primary records" in {
        val claimsView = ClaimsViewModel(activeTransferorRelationshipRecord3, Seq(inactiveRecipientRelationshipRecord1))

        claimsView.activeRow shouldBe ActiveRow(activeTransferorRelationshipRecord3)
        claimsView.historicRows shouldBe Seq(HistoricRow(inactiveRecipientRelationshipRecord1))
        claimsView.backLinkUrl shouldBe backURL
        claimsView.taxFreeAllowanceLink shouldBe taxFreeHtml
      }

      "recipient has primary (with end Date) and does not have non-primary records" in {
        val claimsView = ClaimsViewModel(activeTransferorRelationshipRecord3, Seq())

        claimsView.activeRow shouldBe ActiveRow(activeTransferorRelationshipRecord3)
        claimsView.historicRows shouldBe None
        claimsView shouldBe backURL
        claimsView.taxFreeAllowanceLink shouldBe taxFreeHtml
      }
    }


    "should display historic rows when non-primary records are present" when {
      "recipient has non-primary records" in {
        val claimsView = ClaimsViewModel(activeRecipientRelationshipRecord, Seq(inactiveRecipientRelationshipRecord1))

        claimsView.activeRow shouldBe ActiveRow(activeRecipientRelationshipRecord)
        claimsView.historicRows shouldBe Seq(HistoricRow(inactiveRecipientRelationshipRecord1))
        claimsView.backLinkUrl shouldBe backURL
        claimsView.taxFreeAllowanceLink shouldBe taxFreeHtml
      }

      "transferor has non-primary records" in {
        val claimsView = ClaimsViewModel(activeTransferorRelationshipRecord2, Seq(inactiveTransferorRelationshipRecord1))

        claimsView.activeRow shouldBe ActiveRow(activeTransferorRelationshipRecord2)
        claimsView.historicRows shouldBe Seq(HistoricRow(inactiveTransferorRelationshipRecord1))
        claimsView.backLinkUrl shouldBe backURL
        claimsView.taxFreeAllowanceLink shouldBe taxFreeHtml
      }
    }
  }
}
