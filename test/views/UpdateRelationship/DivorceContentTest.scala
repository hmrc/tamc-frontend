/*
 * Copyright 2024 HM Revenue & Customs
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
import helpers.NbspString
import models.MarriageAllowanceEndingDates
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import services.TimeService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import viewModels.DivorceEndExplanationViewModel
import views.helpers.LanguageUtilsImpl
import views.html.coc.{divorce_end_explanation, divorce_select_year}

import java.time.LocalDate
import java.util.Locale
import scala.concurrent.Future

class DivorceContentTest extends BaseTest with Injecting with NinoGenerator {

  val divorceView: divorce_select_year = inject[divorce_select_year]
  val explanationView: divorce_end_explanation = inject[divorce_end_explanation]
  val appConfig: ApplicationConfig = inject[ApplicationConfig]
  val timeService: TimeService = inject[TimeService]
  val languageUtilsImpl: LanguageUtilsImpl = inject[LanguageUtilsImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  "Divorce Enter Year Page" when {
    "divorceEnterYear" in {
      val document = Jsoup.parse(divorceView(
        new DivorceSelectYearForm(appConfig, timeService, languageUtilsImpl).form
      ).toString()).getElementById("main-content")

      val expectedHeading = "Date of divorce, end of civil partnership or legal separation"
      val heading = document.getElementsByTag("h1").text

      val expectedParas = Array(
        "You need to go to court to be legally separated. You can still get Marriage Allowance if you are separated, but not legally separated.",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      val paras = document.getElementsByTag("p").eachText().toArray()

      val expectedLabel = Array("Day", "Month", "Year")
      val formLabel = document.getElementsByTag("label").eachText.toArray
      val formInput = document.getElementsByTag("input").eachAttr("type")

      val expectedHint = "For example, 30 6 2019"
      val hint = document.getElementsByClass("govuk-hint").text

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      formLabel shouldBe expectedLabel
      formInput.size shouldBe 3
      formInput contains "text"
      hint shouldBe expectedHint
    }
  }

  "Divorce End Explanation Page" when {
    "Transferor and DivorceDate is in Current Year" in {
      val document = Jsoup.parse(explanationView(DivorceEndExplanationViewModel(
        languageUtilsImpl().ukDateTransformer(LocalDate.of(TaxYear.current.startYear, 7, 23)).replaceNbspString,
        taxYearStatus = messages("pages.divorce.explanation.current.taxYear"),
        (messages("pages.divorce.explanation.previous.bullet", languageUtilsImpl().ukDateTransformer(TaxYear.current.previous.finishes)),
          messages("pages.divorce.explanation.adjust.code.bullet"))))
        .toString()).getElementById("main-content")

      val expectedHeading = "Cancelling Marriage Allowance"
      val expectedParas = Array(
        s"You have told us you divorced, ended your civil partnership or were legally separated on 23 July ${TaxYear.current.startYear}.",
        "As this date falls within the current tax year:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      val expectedBullets = Array(
        s"your Marriage Allowance will be cancelled from 5 April ${TaxYear.current.previous.finishYear}",
        "if you have not paid enough tax, we will usually collect it by adjusting your tax code"
      )

      val heading = document.getElementsByTag("h1").text()
      val paras = document.getElementsByTag("p").eachText().toArray
      val bullets = document.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }

    "Transferor and DivorceDate is in PreviousYear" in {
      val document = Jsoup.parse(explanationView(DivorceEndExplanationViewModel(
        languageUtilsImpl().ukDateTransformer(LocalDate.of(TaxYear.current.previous.startYear, 7, 23)).replaceNbspString,
        taxYearStatus = messages("pages.divorce.explanation.previous.taxYear"),
        (messages("pages.divorce.explanation.previous.bullet", languageUtilsImpl().ukDateTransformer(TaxYear.current.previous.previous.finishes)),
          messages("pages.divorce.explanation.adjust.code.bullet"))))
        .toString()).getElementById("main-content")

      val expectedHeading = "Cancelling Marriage Allowance"
      val expectedParas = Array(
        s"You have told us you divorced, ended your civil partnership or were legally separated on 23 July ${TaxYear.current.previous.startYear}.",
        "As this date falls within a previous tax year:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
      val expectedBullets = Array(
        messages("pages.divorce.explanation.previous.bullet", s"5 April ${LocalDate.of(TaxYear.current.previous.startYear, 4, 5).getYear}"),
        messages("pages.divorce.explanation.adjust.code.bullet")
      )

      val heading = document.getElementsByTag("h1").text
      val paras = document.getElementsByTag("p").eachText().toArray
      val bullets = document.getElementsByTag("li").eachText().toArray

      heading shouldBe expectedHeading
      paras shouldBe expectedParas
      bullets shouldBe expectedBullets
    }
//
//    "Recipient and DivorceDate is in current year" in {
//      val taxYear: TaxYear = config.currentTaxYear()
//
//      val endingDates = MarriageAllowanceEndingDates(taxYear.finishes, taxYear.next.starts)
//      val divorceDate  = taxYear.starts
//
//      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
//        .thenReturn(Future.successful((Recipient, divorceDate)))
//
//      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
//        .thenReturn(endingDates)
//
//      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
//        .thenReturn(endingDates)
//
//      val result =  controller.divorceEndExplanation(request)
//
//      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
//      val expectedHeading = messages("pages.divorce.explanation.title")
//      val expectedParas = Seq(
//        messages("pages.divorce.explanation.paragraph1", s"6 April ${divorceDate.getYear}"),
//        messages("pages.divorce.explanation.paragraph2", messages("pages.divorce.explanation.current.taxYear")), "Beta phase.banner.before phase.banner.link phase.banner.after"
//      ).toArray
//
//      val expectedBullets = Seq(
//        messages("pages.divorce.explanation.current.ma.bullet", s"5 April ${taxYear.finishYear}"),
//        messages("pages.divorce.explanation.current.pa.bullet", s"6 April ${taxYear.next.startYear}")
//      ).toArray
//
//      val heading = view.getElementsByTag("h1").text
//      val paras = view.getElementsByTag("p").eachText().toArray
//      val bullets = view.getElementsByTag("li").eachText().toArray
//
//      heading shouldBe expectedHeading
//      paras shouldBe expectedParas
//      bullets shouldBe expectedBullets
//    }
//
//    "Recipient and DivorceDate is in previous year" in {
//      val (year, month, day) = (2017, 4, 5)
//      val endingDates = MarriageAllowanceEndingDates(TaxYear.current.previous.finishes, TaxYear.current.starts)
//      val divorceDate = LocalDate.of(year, month, day)
//
//      when(mockUpdateRelationshipService.getDataForDivorceExplanation(any(), any()))
//        .thenReturn(Future.successful((Recipient, divorceDate)))
//      when(mockUpdateRelationshipService.getMAEndingDatesForDivorce(any(), any()))
//        .thenReturn(endingDates)
//      when(mockUpdateRelationshipService.saveMarriageAllowanceEndingDates(any())(any()))
//        .thenReturn(endingDates)
//
//      val result =  controller.divorceEndExplanation(request)
//
//      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
//      val expectedHeading = messages("pages.divorce.explanation.title")
//      val expectedParas = Seq(
//        messages("pages.divorce.explanation.paragraph1", s"5 April ${divorceDate.getYear}"),
//        messages("pages.divorce.explanation.paragraph2", messages("pages.divorce.explanation.previous.taxYear")), "Beta phase.banner.before phase.banner.link phase.banner.after"
//      ).toArray
//
//      val expectedBullets = Seq(
//        messages("pages.divorce.explanation.previous.bullet", s"5 April ${TaxYear.current.previous.finishYear}"),
//        messages("pages.divorce.explanation.adjust.code.bullet")
//      ).toArray
//
//      val heading = view.getElementsByTag("h1").text
//      val paras = view.getElementsByTag("p").eachText().toArray
//      val bullets = view.getElementsByTag("li").eachText().toArray
//
//      heading shouldBe expectedHeading
//      paras shouldBe expectedParas
//      bullets shouldBe expectedBullets
//    }
  }
}