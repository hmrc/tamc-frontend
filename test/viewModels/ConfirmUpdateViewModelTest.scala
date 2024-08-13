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

package viewModels

import models.{CitizenName, ConfirmationUpdateAnswers, LoggedInUserInfo, MarriageAllowanceEndingDates}
import utils.BaseTest
import views.helpers.{EnglishLangaugeUtils, LanguageUtils}

import java.time.LocalDate

class ConfirmUpdateViewModelTest extends BaseTest {

  val languageUtils: LanguageUtils = EnglishLangaugeUtils

  val confirmUpdateViewModelImpl: ConfirmUpdateViewModelImpl = instanceOf[ConfirmUpdateViewModelImpl]

  val firstName = "Firstname"
  val surname = "Surname"
  val loggedInUser = LoggedInUserInfo(1, "20200304", None, Some(CitizenName(Some(firstName), Some(surname))))
  val email = "email@email.com"
  val marriageAllowanceEndingDates = MarriageAllowanceEndingDates(LocalDate.now(), LocalDate.now())
  val emailRow = SummaryRow(messages("pages.confirm.cancel.email"), "change-link-email","change email", email, Some("/marriage-allowance-application/confirm-email"), 3)
  val nameRow = SummaryRow(messages("pages.confirm.cancel.your-name"), "change-link-name", "change name", s"$firstName $surname", None, 1)

  def createConfirmationUpdateAnswers(loggedInUser: LoggedInUserInfo = loggedInUser,
                                      dateOfDivorce: Option[LocalDate] = Some(LocalDate.now().minusDays(1))
                                     ): ConfirmationUpdateAnswers = {

    ConfirmationUpdateAnswers(loggedInUser, dateOfDivorce, email, marriageAllowanceEndingDates)
  }

  def transformedDate(dateToTransform: LocalDate): String = {
    languageUtils.ukDateTransformer(dateToTransform)
  }

  "ConfirmationUpdateViewModel" should {
    "create a view model" when {
      "a valid confirmationUpdateAnswers object is provided" in {
        val confirmationUpdateAnswers = createConfirmationUpdateAnswers()
        val viewModel = confirmUpdateViewModelImpl(confirmationUpdateAnswers)

        val divorceDate = transformedDate(LocalDate.now().minusDays(1))
        val divorceDateRow = SummaryRow(messages("pages.divorce.title"), "change-link-date", "change date", divorceDate,
          Some(controllers.UpdateRelationship.routes.DivorceController.divorceEnterYear().url), 2)

        val expectedRows = Seq(nameRow, divorceDateRow, emailRow)
        val expectedEndDate = transformedDate(marriageAllowanceEndingDates.marriageAllowanceEndDate)
        val expectedEffectiveDate = transformedDate(marriageAllowanceEndingDates.personalAllowanceEffectiveDate)

        viewModel shouldBe ConfirmUpdateViewModel(expectedEndDate, expectedEffectiveDate, expectedRows)
      }
    }

    "create a view model with no divorce date claim row" when {
      "no divorce date has been provided" in {
        val confirmationUpdateAnswers = createConfirmationUpdateAnswers(dateOfDivorce = None)
        val viewModel = confirmUpdateViewModelImpl(confirmationUpdateAnswers)

        viewModel.rows shouldBe Seq(nameRow, emailRow)
      }
    }

    "display nothing when a users name is unobtainable" in {
      val loggedInUserInfo = LoggedInUserInfo(1, "20200304", None, None)
      val confirmationUpdateAnswers = createConfirmationUpdateAnswers(loggedInUser = loggedInUserInfo)

      val viewModel = confirmUpdateViewModelImpl(confirmationUpdateAnswers)

      val nameRow = viewModel.rows.filter(_.title == messages("pages.confirm.cancel.your-name"))
      nameRow.head.userAnswer shouldBe ""
    }
  }

}
