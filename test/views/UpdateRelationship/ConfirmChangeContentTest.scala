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

import models.auth.AuthenticatedUserRequest
import models.{CitizenName, ConfirmationUpdateAnswers, LoggedInUserInfo, MarriageAllowanceEndingDates}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import viewModels.ConfirmUpdateViewModelImpl
import views.helpers.{EnglishLangaugeUtils, LanguageUtils}
import views.html.coc.confirmUpdate

import java.time.LocalDate
import java.util.Locale

class ConfirmChangeContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: confirmUpdate = inject[confirmUpdate]
  val confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl = instanceOf[ConfirmUpdateViewModelImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  val languageUtils: LanguageUtils = EnglishLangaugeUtils

  val firstName = "Firstname"
  val surname = "Surname"
  val loggedInUser: LoggedInUserInfo = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some(firstName), Some(surname))))
  val taxYearStartCY: Int = TaxYear.current.startYear
  val maEndingDates: MarriageAllowanceEndingDates = MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts)
  val email = "email@email.com"
  val doc: Document = Jsoup.parse(view(confirmUpdateViewModelImpl(ConfirmationUpdateAnswers(
    loggedInUser, Some(LocalDate.now), email, maEndingDates))).toString())

  "Confirm change page" should {
    "Display correct page heading" in {
      doc.getElementsByTag("h1").text() shouldBe "Confirm cancellation of Marriage Allowance"
    }

    "Display correct paragraph text" in {
      doc.getElementsByTag("p").eachText().toArray shouldBe Array(
        "You have asked us to cancel your Marriage Allowance. This means:",
        "Beta This is a new service – your feedback will help us to improve it."
      )
    }

    "Display correct summary list rows & text" in {
      doc.getElementById("marriageAllowanceEndDates").text() shouldBe
        (s"your Marriage Allowance will remain in place until 5 April ${TaxYear.current.finishYear} " +
          s"your Personal Allowance will go back to the normal amount from 6 April ${TaxYear.current.next.startYear}")

      doc.getElementsByTag("dt").eachText().toArray shouldBe Array(
        "Your name",
        "Date of divorce, end of civil partnership or legal separation",
        "Email"
      )

      doc.getElementsByClass("govuk-summary-list_value")
        .eachText().toArray contains Array("Firstname Surname", languageUtils.ukDateTransformer(LocalDate.now()), "email@email.com")
    }

    "Display confirm button" in {
      doc.getElementById("submit").text() shouldBe "Confirm cancellation"
    }
  }

}
