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

import forms.coc.MakeChangesDecisionForm
import models.{RelationshipRecords, Transferor}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.test.Helpers.*
import services.CacheService.{CACHE_MAKE_CHANGES_DECISION, CACHE_RELATIONSHIP_RECORDS}
import utils.IntegrationSpec

import scala.concurrent.Future

class MakeChangesControllerISpec extends IntegrationSpec {

  "MakeChangesController" must {

    "render the make changes page" in {
      when(mockCachingService.get[String](eqTo(CACHE_MAKE_CHANGES_DECISION))(any()))
        .thenReturn(Future.successful(Some("Divorce")))

      val result = route(app, request("/make-changes")).value

      status(result) mustBe OK

      val doc = contentAsString(result)

      doc must include("Why do you want to cancel your Marriage Allowance?")
      doc must include("I have divorced, legally separated or my civil partnership has ended")
    }

    "redirect to the divorce page when Divorce is selected" in {
      when(mockCachingService.put[String](eqTo(CACHE_MAKE_CHANGES_DECISION), any())(any(), any()))
        .thenReturn(Future.successful("Divorce"))

      val fakeRequest = request("/make-changes", POST)
        .withFormUrlEncodedBody(MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.Divorce)

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.UpdateRelationship.routes.DivorceController.divorceEnterYear().url
      )
    }
    Seq(MakeChangesDecisionForm.IncomeChange, MakeChangesDecisionForm.WrongSetup, MakeChangesDecisionForm.AnotherReason)
      .foreach { answer =>
        s"redirect to the stop allowance page when $answer is selected by Recipient" in {
          when(mockCachingService.put[String](eqTo(CACHE_MAKE_CHANGES_DECISION), any())(any(), any()))
            .thenReturn(Future.successful("Cancel"))

          val fakeRequest = request("/make-changes", POST)
            .withFormUrlEncodedBody(MakeChangesDecisionForm.StopMAChoice -> answer)

          val result = route(app, fakeRequest).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.UpdateRelationship.routes.StopAllowanceController.stopAllowance().url
          )
        }

        s"redirect to the cancel page when $answer is selected by Transferor" in {
          val transferorRelationshipRecords = relationshipRecords.copy(
            primaryRecord = relationshipRecords.primaryRecord.copy(
              participant = Transferor.value
            )
          )

          when(mockCachingService.get[RelationshipRecords](eqTo(CACHE_RELATIONSHIP_RECORDS))(any()))
            .thenReturn(Future.successful(Some(transferorRelationshipRecords)))

          when(mockCachingService.put[String](eqTo(CACHE_MAKE_CHANGES_DECISION), any())(any(), any()))
            .thenReturn(Future.successful(answer))

          val fakeRequest = request("/make-changes", POST)
            .withFormUrlEncodedBody(MakeChangesDecisionForm.StopMAChoice -> answer)

          val result = route(app, fakeRequest).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.UpdateRelationship.routes.StopAllowanceController.cancel().url
          )
        }
      }

    "redirect to the bereavement page when Bereavement is selected" in {
      when(mockCachingService.put[String](eqTo(CACHE_MAKE_CHANGES_DECISION), any())(any(), any()))
        .thenReturn(Future.successful("Bereavement"))

      val fakeRequest = request("/make-changes", POST)
        .withFormUrlEncodedBody(MakeChangesDecisionForm.StopMAChoice -> MakeChangesDecisionForm.Bereavement)

      val result = route(app, fakeRequest).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.UpdateRelationship.routes.BereavementController.bereavement().url
      )
    }
  }
}
