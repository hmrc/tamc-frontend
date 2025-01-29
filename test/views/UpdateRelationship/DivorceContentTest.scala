/*
 * Copyright 2025 HM Revenue & Customs
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

package views.UpdateRelationship

import config.ApplicationConfig
import forms.coc._
import models.auth.AuthenticatedUserRequest
import models.{MarriageAllowanceEndingDates, Recipient, Transferor}
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import services.TimeService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import viewModels.DivorceEndExplanationViewModelImpl
import views.helpers.LanguageUtilsImpl
import views.html.coc.{divorce_end_explanation, divorce_select_year}

import java.time.LocalDate
import java.util.Locale

class DivorceContentTest extends BaseTest with Injecting with NinoGenerator {

  val divorceView: divorce_select_year = inject[divorce_select_year]
  val explanationView: divorce_end_explanation = inject[divorce_end_explanation]
  val divorceEndExplanationViewModelImpl: DivorceEndExplanationViewModelImpl = instanceOf[DivorceEndExplanationViewModelImpl]
  val appConfig: ApplicationConfig = inject[ApplicationConfig]
  val timeService: TimeService = inject[TimeService]
  val languageUtilsImpl: LanguageUtilsImpl = inject[LanguageUtilsImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "Divorce Enter Year Page" when {
    "divorceEnterYear" in {
      val doc = Jsoup.parse(divorceView(new DivorceSelectYearForm(appConfig, timeService, languageUtilsImpl).form
      ).toString()).getElementById("main-content")

      val heading = doc.getElementsByTag("h1").text
      val paras = doc.getElementsByTag("p").eachText().toArray()
      val formLabel = doc.getElementsByTag("label").eachText.toArray
      val formInput = doc.getElementsByTag("input").eachAttr("type")
      val hint = doc.getElementsByClass("govuk-hint").text

      heading shouldBe "Date of divorce, end of civil partnership or legal separation"
      formLabel shouldBe Array("Day", "Month", "Year")
      formInput contains "text"
      hint shouldBe "For example, 30 6 2019"
      paras shouldBe Array(
        "You need to go to court to be legally separated. You can still get Marriage Allowance if you are separated, but not legally separated.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }
  }

  "Display correct content for Divorce End Explanation Page" when {
    "Transferor and DivorceDate is in Current Year" in {
      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val divorceDate = LocalDate.of(TaxYear.current.startYear, 7, 23)
      val doc = Jsoup.parse(explanationView(divorceEndExplanationViewModelImpl(Transferor, divorceDate, endingDates))
        .toString()).getElementById("main-content")

      val heading = doc.getElementsByTag("h1").text()
      val paras = doc.getElementsByTag("p").eachText().toArray
      val bullets = doc.getElementsByTag("li").eachText().toArray

      heading shouldBe "Cancelling Marriage Allowance"
      paras shouldBe Array(
        s"You have told us you divorced, ended your civil partnership or were legally separated on 23 July ${TaxYear.current.startYear}.",
        "As this date falls within the current tax year:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      bullets shouldBe Array(
        s"your Marriage Allowance will be cancelled from 5 April ${TaxYear.current.previous.finishYear}",
        "if you have not paid enough tax, we will usually collect it by adjusting your tax code"
      )
    }

    "Transferor and DivorceDate is in PreviousYear" in {
      val previousTaxStartYear = TaxYear.current.previous.startYear
      val endingDates = MarriageAllowanceEndingDates(LocalDate.of(TaxYear.current.previous.previous.finishYear, 4, 5), LocalDate.of(previousTaxStartYear, 4, 6))
      val divorceDate  = LocalDate.of(TaxYear.current.previous.startYear, 7, 23)
      val doc = Jsoup.parse(explanationView(divorceEndExplanationViewModelImpl(Transferor, divorceDate, endingDates))
        .toString()).getElementById("main-content")

      val heading = doc.getElementsByTag("h1").text
      val paras = doc.getElementsByTag("p").eachText().toArray
      val bullets = doc.getElementsByTag("li").eachText().toArray

      heading shouldBe "Cancelling Marriage Allowance"
      paras shouldBe Array(
        s"You have told us you divorced, ended your civil partnership or were legally separated on 23 July $previousTaxStartYear.",
        "As this date falls within a previous tax year:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      bullets shouldBe Array(
        s"your Marriage Allowance will be cancelled from 5 April ${LocalDate.of(previousTaxStartYear, 4, 5).getYear}",
        "if you have not paid enough tax, we will usually collect it by adjusting your tax code"
      )
    }

    "Recipient and DivorceDate is in current year" in {
      val taxYear: TaxYear = appConfig.currentTaxYear()
      val divorceDate  = taxYear.starts
      val endingDates = MarriageAllowanceEndingDates(taxYear.finishes, taxYear.next.starts)
      val doc = Jsoup.parse(explanationView(divorceEndExplanationViewModelImpl(Recipient, divorceDate, endingDates))
        .toString()).getElementById("main-content")

      val heading = doc.getElementsByTag("h1").text
      val paras = doc.getElementsByTag("p").eachText().toArray
      val bullets = doc.getElementsByTag("li").eachText().toArray

      heading shouldBe "Cancelling Marriage Allowance"
      paras shouldBe Array(
        s"You have told us you divorced, ended your civil partnership or were legally separated on 6 April ${divorceDate.getYear}.",
        "As this date falls within the current tax year:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      bullets shouldBe Array(
        s"your Marriage Allowance will be cancelled from 5 April ${taxYear.finishYear}, the end of the current tax year",
        s"your Personal Allowance will go back to the normal amount from 6 April ${taxYear.next.startYear}, the start of the new tax year"
      )
    }

    "Recipient and DivorceDate is in previous year" in {
      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
      val divorceDate = LocalDate.of(TaxYear.current.previous.startYear, 7, 23)
      val doc = Jsoup.parse(explanationView(divorceEndExplanationViewModelImpl(Recipient, divorceDate, endingDates))
        .toString()).getElementById("main-content")

      val heading = doc.getElementsByTag("h1").text
      val paras = doc.getElementsByTag("p").eachText().toArray
      val bullets = doc.getElementsByTag("li").eachText().toArray

      heading shouldBe "Cancelling Marriage Allowance"
      paras shouldBe Array(
        s"You have told us you divorced, ended your civil partnership or were legally separated on 23 July ${divorceDate.getYear}.",
        "As this date falls within a previous tax year:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      bullets shouldBe Array(
        s"your Marriage Allowance will be cancelled from 5 April ${TaxYear.current.previous.finishYear}",
        "if you have not paid enough tax, we will usually collect it by adjusting your tax code"
      )
    }
  }

}
