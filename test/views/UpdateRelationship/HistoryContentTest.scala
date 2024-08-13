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
import models.{CitizenName, LoggedInUserInfo, Recipient, Role, Transferor}
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.time.TaxYear
import utils.{BaseTest, NinoGenerator}
import viewModels.HistorySummaryViewModelImpl
import views.html.coc.history_summary

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class HistoryContentTest extends BaseTest with Injecting with NinoGenerator{

  val appConfig: ApplicationConfig = inject[ApplicationConfig]
  val view: history_summary = inject[history_summary]
  val historySummaryViewModelImpl: HistorySummaryViewModelImpl = instanceOf[HistorySummaryViewModelImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
  val citizenName: CitizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo: LoggedInUserInfo = LoggedInUserInfo(
    cid = 1122L,
    timestamp = LocalDate.now().toString,
    has_allowance = None,
    name = Some(citizenName))
  val maxBenefitCY: String = NumberFormat.getIntegerInstance().format(appConfig.MAX_BENEFIT(TaxYear.current.startYear))
  val maxAllowanceTransferCY: String = NumberFormat.getIntegerInstance().format(appConfig.MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(TaxYear.current.startYear))

  val userRecord: Seq[(Role, Boolean, String)] = Seq(
    (Transferor, true, "Your Marriage Allowance claim has ended. " +
      s"You will keep the tax-free allowances transferred by you until 5 April ${TaxYear.current.finishes.getYear}."),
    (Transferor, false, "You are currently helping your partner benefit from Marriage Allowance."),
    (Recipient, true, "Your Marriage Allowance claim has ended. " +
      s"You will keep the tax-free allowances transferred to you until 5 April ${TaxYear.current.finishes.getYear}."),
    (Recipient, false, s"Your partner is currently using Marriage Allowance to transfer £" +
      s"$maxAllowanceTransferCY of their Personal Allowance to you. " +
      s"This can reduce the tax you pay by up to £$maxBenefitCY a year.")
    )

  "History page" when {
    userRecord.foreach {
      case (role, mACancelled, content) =>
        s"Page Header & caption & $content - are displayed for $mACancelled records when $role checks summary" in {
          val doc = Jsoup.parse(view(historySummaryViewModelImpl(role, mACancelled, loggedInUserInfo)).toString())

          val pageHeading = doc.getElementById("pageHeading").text()
          val caption = doc.getElementsByClass("govuk-caption-xl hmrc-caption-xl").text()
          val paragraphs = doc.getElementsByClass("govuk-body").text()
          val button = doc.getElementsByClass("govuk-button").text()

         if (mACancelled) {
           button shouldBe "Check your Marriage Allowance claims"
         } else button shouldBe "Check or update your Marriage Allowance"

          pageHeading shouldBe "Test User"
          paragraphs shouldBe content
          caption contains "Your Marriage Allowance summary"
        }
    }
  }

}
