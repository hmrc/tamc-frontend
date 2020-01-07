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

package views.html.coc

import models._
import org.joda.time.LocalDate
import org.scalatest.mockito.MockitoSugar
import play.twirl.api.Html
import utils.TamcViewTest
import viewModels.HistorySummaryViewModel

class HistorySummaryTest extends TamcViewTest with MockitoSugar {

  val cid = 1122L
  val timeStamp = new LocalDate().toString
  val hasAllowance = None
  val citizenName = CitizenName(Some("Test"), Some("User"))
  val loggedInUserInfo = Some(LoggedInUserInfo(cid, timeStamp, hasAllowance, Some(citizenName)))
  val viewModel = mock[HistorySummaryViewModel]

  override def view: Html = views.html.coc.history_summary(loggedInUserInfo, viewModel)

  "finished page " should {

    behave like pageWithTitle(messagesApi("title.history"))
    behave like pageWithCombinedHeader(s"${citizenName.firstName.getOrElse("")} ${citizenName.lastName.getOrElse("")}", messagesApi("title.history"))

  }



}
