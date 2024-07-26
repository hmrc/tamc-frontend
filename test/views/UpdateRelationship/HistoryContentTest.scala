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

import models.{CitizenName, LoggedInUserInfo, Recipient, Role, Transferor}
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import viewModels.HistorySummaryViewModelImpl
import views.html.coc.history_summary

import java.time.LocalDate
import java.util.Locale

class HistoryContentTest extends BaseTest with Injecting with NinoGenerator{

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

  val userRecord: Seq[(Role, Boolean, Array[String])] = Seq(
    (Transferor, false, Array("")),
    (Transferor, true, Array("You are currently helping your partner benefit from Marriage Allowance.")),
    (Recipient, false, Array("")),
    (Recipient, true, Array("Your partner is currently using Marriage Allowance to transfer £1,260 of their Personal Allowance to you.",
                      "This can reduce the tax you pay by up to £252 a year.")
    )
  )

  "History page" when {
    userRecord.foreach {
      case (role, mACancelled, Array("")) =>
        s"Page Header & caption - are displayed for $mACancelled records when $role checks summary" in {
          val document = Jsoup.parse(view(historySummaryViewModelImpl(role, mACancelled, loggedInUserInfo)).toString())

          val pageHeading = document.getElementById("pageHeading").text()
          val caption = document.getElementsByClass("govuk-caption-xl hmrc-caption-xl").text()
          val content = document.getElementsByTag("p").text()

          pageHeading shouldBe "Test User"
          content shouldBe "You are currently helping your partner benefit from Marriage Allowance."
          caption contains "Your Marriage Allowance summary"
        }
    }
  }

}
