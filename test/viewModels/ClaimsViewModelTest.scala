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
import models._
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.time.TaxYear
import utils.TamcViewModelTest


class ClaimsViewModelTest extends ViewModelBaseSpec {

  lazy val currentOfTaxYear: Int = TaxYear.current.currentYear
  lazy val endOfTaxYear: LocalDate = TaxYear.current.finishes
  lazy val maxPATransfer: Int = PERSONAL_ALLOWANCE(currentOfTaxYear)
  lazy val maxBenefit: Int = MAX_BENEFIT(currentOfTaxYear)
  lazy val maxPaTransferFormatted: Int = MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(currentOfTaxYear)
  lazy val formattedEndOfTaxYear: String = endOfTaxYear.toString(DateTimeFormat.forPattern("d MMMM yyyy").withLocale(Locale.UK))

  private def buildTaxFreeHtml()(implicit messages: Messages): Html = {
    Html(
      s"""${messages("pages.claims.link.tax.free.allowance.part1")} <a href="${ApplicationConfig.taxFreeAllowanceUrl}">
         |${messages("pages.claims.link.tax.free.allowance.link.text")}</a>""".stripMargin)
  }

  private def buildBackURL(recordStatus: RecordStatus): String = {
    recordStatus match {
      case Active =>
        controllers.routes.UpdateRelationshipController.decision().url
      case _ =>
        controllers.routes.UpdateRelationshipController.history().url
    }
  }

  "ClaimsViewModel" should {
    "not active and not historic" when {
      "have no active no historic records but is Active" in {
        val claimsView = ClaimsViewModel(None, None, Active)

        claimsView.activeRow shouldBe None
        claimsView.historicRows shouldBe None
        claimsView.isActiveRecord shouldBe true
        claimsView.backLinkUrl shouldBe buildBackURL(Active)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have no active no historic records but is ActiveHistoric" in {
        val claimsView = ClaimsViewModel(None, None, ActiveHistoric)

        claimsView.activeRow shouldBe None
        claimsView.historicRows shouldBe None
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(ActiveHistoric)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have no active no historic records but is Historic" in {
        val claimsView = ClaimsViewModel(None, None, Historic)

        claimsView.activeRow shouldBe None
        claimsView.historicRows shouldBe None
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(Historic)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

    }

    "not active and is historic" when {
      "have no active is historic records but is Active" in {
        val claimsView = ClaimsViewModel(None, Some(Seq(inactiveRecipientRelationshipRecord1)), Active)

        claimsView.activeRow shouldBe None
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord1)))
        claimsView.isActiveRecord shouldBe true
        claimsView.backLinkUrl shouldBe buildBackURL(Active)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have no active is historic records but is ActiveHistoric" in {
        val claimsView = ClaimsViewModel(None, Some(Seq(inactiveRecipientRelationshipRecord2)), ActiveHistoric)

        claimsView.activeRow shouldBe None
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord2)))
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(ActiveHistoric)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have no active is historic records but is Historic" in {
        val claimsView = ClaimsViewModel(None, Some(Seq(inactiveRecipientRelationshipRecord3)), Historic)

        claimsView.activeRow shouldBe None
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord3)))
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(Historic)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

    }

    "is active(without end date) and is historic" when {
      "have is active and is historic records but is Active" in {
        val claimsView = ClaimsViewModel(Some(activeRecipientRelationshipRecord), Some(Seq(inactiveRecipientRelationshipRecord1)), Active)

        claimsView.activeRow shouldBe Some(ActiveRow(activeRecipientRelationshipRecord))
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord1)))
        claimsView.isActiveRecord shouldBe true
        claimsView.backLinkUrl shouldBe buildBackURL(Active)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have is active and is historic records but is ActiveHistoric" in {
        val claimsView = ClaimsViewModel(Some(activeRecipientRelationshipRecord), Some(Seq(inactiveRecipientRelationshipRecord2)), ActiveHistoric)

        claimsView.activeRow shouldBe Some(ActiveRow(activeRecipientRelationshipRecord))
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord2)))
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(ActiveHistoric)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have is active and is historic records but is Historic" in {
        val claimsView = ClaimsViewModel(Some(activeRecipientRelationshipRecord), Some(Seq(inactiveRecipientRelationshipRecord3)), Historic)

        claimsView.activeRow shouldBe Some(ActiveRow(activeRecipientRelationshipRecord))
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord3)))
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(Historic)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

    }

    "is active(with end date) and is historic" when {
      "have is active and is historic records but is Active" in {
        val claimsView = ClaimsViewModel(Some(activeTransferorRelationshipRecord3), Some(Seq(inactiveRecipientRelationshipRecord1)), Active)

        claimsView.activeRow shouldBe Some(ActiveRow(activeTransferorRelationshipRecord3))
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord1)))
        claimsView.isActiveRecord shouldBe true
        claimsView.backLinkUrl shouldBe buildBackURL(Active)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have is active and is historic records but is ActiveHistoric" in {
        val claimsView = ClaimsViewModel(Some(activeTransferorRelationshipRecord3), Some(Seq(inactiveRecipientRelationshipRecord2)), ActiveHistoric)

        claimsView.activeRow shouldBe Some(ActiveRow(activeTransferorRelationshipRecord3))
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord2)))
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(ActiveHistoric)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

      "have is active and is historic records but is Historic" in {
        val claimsView = ClaimsViewModel(Some(activeTransferorRelationshipRecord3), Some(Seq(inactiveRecipientRelationshipRecord3)), Historic)

        claimsView.activeRow shouldBe Some(ActiveRow(activeTransferorRelationshipRecord3))
        claimsView.historicRows shouldBe Some(Seq(HistoricRow(inactiveRecipientRelationshipRecord3)))
        claimsView.isActiveRecord shouldBe false
        claimsView.backLinkUrl shouldBe buildBackURL(Historic)
        claimsView.taxFreeAllowanceLink shouldBe buildTaxFreeHtml
      }

    }
  }

}
