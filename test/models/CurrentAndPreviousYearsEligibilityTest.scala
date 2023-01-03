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

package models

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import services.TimeService
import utils.{BaseTest, SystemTaxYear}

class CurrentAndPreviousYearsEligibilityTest extends BaseTest with BeforeAndAfterEach {

  lazy val timeService: TimeService = instanceOf[TimeService]
  lazy val currentYear: Int = timeService.getCurrentTaxYear
  lazy val previousTaxYear: TaxYear = TaxYear(currentYear - 1)
  lazy val taxYear: SystemTaxYear = instanceOf[SystemTaxYear]
  val mockData: RegistrationFormInput = mock[RegistrationFormInput]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[RegistrationFormInput].toInstance(mockData)
    ).build()

  def createRecipientRecord(availableTaxYears: List[TaxYear]): RecipientRecord = {
    RecipientRecord(mock[UserRecord], mockData, availableTaxYears)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockData)
  }

  "CurrentAndPreviousYearsEligibility" should {
    "contain a true value if there is a currentTaxYear present" in {
      val availableTaxYears = List(TaxYear(currentYear), previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord, taxYear)

      result.currentYearAvailable shouldBe true
    }

    "contain a false value if there is no currentTaxYear present" in {
      val availableTaxYears = List(previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord, taxYear)

      result.currentYearAvailable shouldBe false
    }

    "contain an empty list if no previous tax years are present in availableTaxYears" in {
      val availableTaxYears = List(TaxYear(currentYear))
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord, taxYear)

      result.previousYears shouldBe List.empty[TaxYear]
    }

    "contain a list of previous tax years if they are present in availableTaxYears" in {
      val previousTaxYear2017 = TaxYear(currentYear - 2)
      val availableTaxYears = List(previousTaxYear, previousTaxYear2017)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord, taxYear)

      result.previousYears shouldBe List(previousTaxYear, previousTaxYear2017)
    }

    "contain recipientRecord data" in {
      val availableTaxYears = List(previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord, taxYear)

      result.registrationInput shouldBe mockData
    }

    "contain availableTaxYears data" in {
      val availableTaxYears = List(previousTaxYear)
      val recipientRecord = createRecipientRecord(availableTaxYears)
      val result = CurrentAndPreviousYearsEligibility(recipientRecord, taxYear)

      result.availableTaxYears shouldBe availableTaxYears
    }

  }
}
