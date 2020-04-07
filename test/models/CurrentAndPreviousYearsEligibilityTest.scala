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

package models

import org.scalatest.mockito.MockitoSugar
import services.TimeService
import uk.gov.hmrc.play.test.UnitSpec
import org.mockito.Mockito._

class CurrentAndPreviousYearsEligibilityTest extends UnitSpec with MockitoSugar {

  val currentYear: Int = TimeService.getCurrentTaxYear
  val previousTaxYear: TaxYear = TaxYear(currentYear - 1)
  val mockData: RegistrationFormInput = mock[RegistrationFormInput]
  val mockTimeService: TimeService = mock[TimeService]

  def createRecipientRecord(availableTaxYears: List[TaxYear]): RecipientRecord = {
    RecipientRecord(mock[UserRecord], mockData, availableTaxYears)
  }

  "CurrentAndPreviousYearsEligibility" should {
    when(mockTimeService.getCurrentTaxYear).thenReturn(currentYear)

    "contain a true value if there is a currentTaxYear present" in {
      val availableTaxYears = List(TaxYear(currentYear), previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord)

      result.currentYearAvailable shouldBe true
    }

    "contain a false value if there is no currentTaxYear present" in {
      val availableTaxYears = List(previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord)

      result.currentYearAvailable shouldBe false
    }

    "contain an empty list if no previous tax years are present in availableTaxYears" in {
      val availableTaxYears = List(TaxYear(currentYear))
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord)

      result.previousYears shouldBe List.empty[TaxYear]
    }

    "contain a list of previous tax years if they are present in availableTaxYears" in {
      val previousTaxYear2017 = TaxYear(currentYear - 2)
      val availableTaxYears = List(previousTaxYear, previousTaxYear2017)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord)

      result.previousYears shouldBe List(previousTaxYear, previousTaxYear2017)
    }

    "contain recipientRecord data" in {
      val availableTaxYears = List(previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord)

      result.registrationInput shouldBe mockData
    }

    "contain availableTaxYears data" in {
      val availableTaxYears = List(previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord)

      result.availableTaxYears shouldBe availableTaxYears
    }

  }
}
