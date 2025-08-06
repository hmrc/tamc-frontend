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

package views.multiyear.transfer

import config.ApplicationConfig
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.i18n.MessagesApi
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import views.helpers.LanguageUtilsImpl
import views.html.multiyear.transfer.eligible_years

import java.time.LocalDate

class EligibleYearsTest extends BaseTest with NinoGenerator {

  val appConfig: ApplicationConfig                                       = instanceOf[ApplicationConfig]
  val nino: String                                                       = generateNino().nino
  lazy val recipient: String                                             = "firstName"
  val eligibleYears: eligible_years                                      = instanceOf[eligible_years]
  implicit val request: AuthenticatedUserRequest[AnyContentAsEmpty.type] =
    AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))
  override implicit lazy val messages: Messages                          =
    instanceOf[MessagesApi].asScala.preferred(FakeRequest(): Request[AnyContent])
  val languageUtilsImpl: LanguageUtilsImpl                               = instanceOf[LanguageUtilsImpl]

  implicit val doc: Document = Jsoup.parse(eligibleYears(true, recipient, Some(LocalDate.now)).toString())

  val currentTaxYearWithNBSP: String = languageUtilsImpl.apply().ukDateTransformer(LocalDate.now())
  val currentTaxYearDate: String     = currentTaxYearWithNBSP.replace("\u00A0", " ")

  val currentTaxYear: Int            = TaxYear.current.currentYear
  val maxBenefit: Int                = appConfig.MAX_BENEFIT(currentTaxYear)

  "EligibleYears" should {
    "return correct title" in {
      val title    = doc.title()
      val expected =
        s"You are applying for the current tax year onwards, from $currentTaxYearDate - Marriage Allowance application - GOV.UK"

      title should include(expected)
    }

    "display correct heading" in {
      doc
        .getElementsByTag("h1")
        .text() shouldBe s"You are applying for the current tax year onwards, from $currentTaxYearDate"
    }

    "display correct text" in {
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        s"$recipient will pay up to £$maxBenefit less tax each year",
        "Marriage Allowance renews each year unless:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "display correct bullet list content" in {
      val bulletList = doc.select("#renewal-conditions > li")
      val listItem1  = s"you or $recipient cancel it"
      val listItem2  = "you are no longer eligible"

      bulletList.get(0).text() should include(listItem1)
      bulletList.get(1).text() should include(listItem2)
    }

    "display continue button" in {
      doc.getElementsByClass("govuk-button").text() shouldBe "Continue"
    }

  }

}
