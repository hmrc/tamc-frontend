/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.updateRelationship

import models.CancellationConfirmation
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.CACHE_CANCELLATION_CONFIRMATION
import uk.gov.hmrc.time.TaxYear
import utils.IntegrationSpec

import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.Future

class FinishedChangeControllerISpec extends IntegrationSpec {

  "FinishedChangeController" must {
    "render the cancellation submission page with Active status message when the end date is in the future" in {
      val endDate          = TaxYear.current.finishes
      val formattedEndDate = endDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK))
      val confirmation     = CancellationConfirmation(
        marriageAllowanceEndDate = endDate
      )

      when(
        mockCachingService.get[CancellationConfirmation](ArgumentMatchers.eq(CACHE_CANCELLATION_CONFIRMATION))(any())
      ).thenReturn(Future.successful(Some(confirmation)))

      val result = route(app, request("/finished-change")).value

      status(result) mustBe OK

      val doc = contentAsString(result).replace('\u00A0', ' ')
      doc must include("Marriage Allowance cancellation submitted")
      doc must include("We will send you a confirmation email within 24 hours.")
      doc must include(
        s"We will cancel your Marriage Allowance from $formattedEndDate. Until then your Marriage Allowance will show as ‘Active’."
      )
      doc must include("Before you go")
      doc must include("Take a short survey")
    }
  }
}
