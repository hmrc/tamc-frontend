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

import models.{CitizenName, ConfirmationUpdateAnswers, LoggedInUserInfo, MarriageAllowanceEndingDates}
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, NinoGenerator}
import viewModels.ConfirmUpdateViewModelImpl
import views.html.coc.confirmUpdate

import java.util.Locale

class ConfirmChangeContentTest extends BaseTest with Injecting with NinoGenerator {

  val view: confirmUpdate = inject[confirmUpdate]
  val confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl = instanceOf[ConfirmUpdateViewModelImpl]
  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, isSA = true, None, Nino(nino))
  lazy val nino: String = generateNino().nino
  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  val firstName = "Firstname"
  val surname = "Surname"
  val loggedInUser: LoggedInUserInfo = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some(firstName), Some(surname))))
  //val maEndingDates: MarriageAllowanceEndingDates = MarriageAllowanceEndingDates()
  val email = "email@email.com"

  "Confirm change page" should {
//    "Display correct page heading in" {
//      //val document = Jsoup.parse(view(confirmUpdateViewModelImpl(ConfirmationUpdateAnswers(loggedInUser, None, email,  ))))
//    }
  }

//  "Confirmation Update Page" when {
//    "End reason divorce display divorce date row" in {
//
//      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
//        .thenReturn(Future.successful(
//          ConfirmationUpdateAnswers(loggedInUser, Some(LocalDate.now()), "email@email.com", MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))))
//
//      val expectedHeader = messages("pages.confirm.cancel.heading")
//      val expectedParas = Seq(messages("pages.confirm.cancel.message"), "Beta phase.banner.before phase.banner.link phase.banner.after")
//      val expectedList = Seq(
//        messages("pages.confirm.cancel.message1", s"5 April ${TaxYear.current.finishYear}"),
//        messages("pages.confirm.cancel.message2", s"6 April ${TaxYear.current.next.startYear}")
//      ).toArray
//      val expectedTableHeadings = Seq(
//        messages("pages.confirm.cancel.your-name"),
//        messages("pages.divorce.title"),
//        messages("pages.confirm.cancel.email")
//      ).toArray
//
//      val result = controller.confirmUpdate(request)
//
//      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
//
//      val header = view.getElementsByTag("h1").text
//      val para = view.getElementsByTag("p").eachText.toArray
//      val list = view.getElementsByTag("li").eachText.toArray
//      val tableHeadings = view.getElementsByTag("dt").eachText.toArray
//
//      header shouldBe expectedHeader
//      para shouldBe expectedParas
//      list shouldBe expectedList
//      tableHeadings.size shouldBe 3
//      tableHeadings shouldBe expectedTableHeadings
//    }
//
//    "display two rows when no DivorceDate is present" in {
//      when(mockUpdateRelationshipService.getConfirmationUpdateAnswers(any(), any()))
//        .thenReturn(Future.successful(
//          ConfirmationUpdateAnswers(loggedInUser, None, "email@email.com", MarriageAllowanceEndingDates(TaxYear.current.finishes, TaxYear.current.next.starts))))
//
//      val expectedHeader = messages("pages.confirm.cancel.heading")
//      val expectedParas = Seq(messages("pages.confirm.cancel.message"), "Beta phase.banner.before phase.banner.link phase.banner.after")
//      val expectedList = Seq(
//        messages("pages.confirm.cancel.message1", s"5 April ${TaxYear.current.finishYear}"),
//        messages("pages.confirm.cancel.message2", s"6 April ${TaxYear.current.next.startYear}")
//      ).toArray
//      val expectedTableHeadings = Seq(
//        messages("pages.confirm.cancel.your-name"),
//        messages("pages.confirm.cancel.email")
//      ).toArray
//
//      val result = controller.confirmUpdate(request)
//
//      val view = Jsoup.parse(contentAsString(result)).getElementById("main-content")
//
//      val header = view.getElementsByTag("h1").text
//      val para = view.getElementsByTag("p").eachText.toArray
//      val list = view.getElementsByTag("li").eachText.toArray
//      val tableHeadings = view.getElementsByTag("dt").eachText.toArray
//
//      header shouldBe expectedHeader
//      para shouldBe expectedParas
//      para shouldBe expectedParas
//      list shouldBe expectedList
//      tableHeadings.size shouldBe 2
//      tableHeadings shouldBe expectedTableHeadings
//    }
//  }

}
