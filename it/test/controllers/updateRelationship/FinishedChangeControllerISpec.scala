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
import org.mockito.Mockito.{never, verify, when}
import play.api.test.Helpers.*
import services.CacheService.CACHE_CANCELLATION_CONFIRMATION
import utils.IntegrationSpec

import java.time.LocalDate
import scala.concurrent.Future

class FinishedChangeControllerISpec extends IntegrationSpec {

  "FinishedChangeController" must {
    "render the cancellation submitted page with the saved end date and keep it available" in {
      val confirmation = CancellationConfirmation(
        marriageAllowanceEndDate = LocalDate.of(2027, 4, 5)
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
        "We will cancel your Marriage Allowance from 5 April 2027. Until then your Marriage Allowance will show as ‘Active’."
      )
      doc must include("Before you go")
      doc must include("Take a short survey")

      verify(mockCachingService, never()).clear()(any())
    }
  }
}
