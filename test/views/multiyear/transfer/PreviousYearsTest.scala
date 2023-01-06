/*
 * Copyright 2023 HM Revenue & Customs
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

//package views.multiyear.transfer
//
//import org.jsoup.Jsoup
//import utils.BaseTest
//import views.html.multiyear.transfer.previous_years
//import forms.RegistrationForm
//import models.TaxYear
//import models.auth.AuthenticatedUserRequest
//import play.api.test.FakeRequest
//import uk.gov.hmrc.domain.Nino
//
//import java.time.LocalDate
//
//class PreviousYearsTest extends BaseTest {
//
//  lazy val previousYears = instanceOf[previous_years]
//  lazy val previousYearForm = instanceOf[RegistrationForm]
//  implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino("AA000000A"))
//
//  "previousYears" should {
//    "return the correct title" in {
//
//      val document = Jsoup.parse(previousYears(previousYearForm.registrationForm(LocalDate.now, Nino("AA000000A")),
//        List(TaxYear(2022)),
//        true).toString())
//
//      val title = document.title()
//      val expected = messages("title.application.pattern", messages("pages.previousyear.header"))
//
//      title shouldBe expected
//    }
//  }
//
//}
