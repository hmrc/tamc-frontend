/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.actions.AuthenticatedActionRefiner
import controllers.{ControllerBaseSpec}
import models.RelationshipRecordList
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.redirectLocation
import services.{CachingService, ListRelationshipService, TimeService, TransferService, UpdateRelationshipService}
import test_utils.MockTemporaryAuthenticatedAction
import test_utils.data.RelationshipRecordData
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import play.api.test.Helpers._
import scala.concurrent.duration._

import scala.concurrent.{Await, Future}

class HistoryControllerTest extends ControllerBaseSpec {

  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockListRelationshipService: ListRelationshipService = mock[ListRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  def controller(auth: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner]): HistoryController =
    new HistoryController(
      messagesApi,
      auth,
      mockUpdateRelationshipService,
      mockListRelationshipService,
      mockRegistrationService,
      mockCachingService,
      mockTimeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  "History" should {
    "redirect to transfer" when {
      "has no active record, no historic and temporary authentication" in {
        when(mockListRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful(
              (RelationshipRecordList(None, None, None, activeRecord = false, historicRecord = false, historicActiveRecord = false), true)
            )
          )
        val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).onPageLoad()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.TransferController.transfer().url)
      }
    }

    "redirect to how-it-works" when {
      "has no active record, no historic and permanent authentication" in {
        when(mockListRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful(
              (RelationshipRecordList(None, None, None, activeRecord = false, historicRecord = false, historicActiveRecord = false), true)
            )
          )
        val result: Future[Result] = controller().onPageLoad()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.EligibilityController.howItWorks().url)
      }
    }

    "load change of circumstances page" when {
      "has some active record" in {
        when(mockListRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful((RelationshipRecordData.activeRelationshipRecordList, false))
          )

        val result: Future[Result] = controller().onPageLoad()(request)
        status(result) shouldBe OK
      }

      "has some historic record" in {
        when(mockListRelationshipService.listRelationship(any())(any(), any()))
          .thenReturn(
            Future.successful((RelationshipRecordData.historicRelationshipRecordList, true))
          )

        val result: Future[Result] = controller().onPageLoad()(request)
        status(result) shouldBe OK
      }
    }
  }

  "historyWithCy" should {
    "redirect to history, with a welsh language setting" in {
      val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).onPageLoadWithCy()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.updateRelationship.routes.HistoryController.onPageLoad().url)
      val resolved = Await.result(result, 5 seconds)
      resolved.header.headers.keys should contain("Set-Cookie")
      resolved.header.headers("Set-Cookie") should include("PLAY_LANG=cy")
    }
  }

  "historyWithEn" should {
    "redirect to history, with an english language setting" in {
      val result: Future[Result] = controller(instanceOf[MockTemporaryAuthenticatedAction]).onPageLoadWithEn()(request)
      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(controllers.updateRelationship.routes.HistoryController.onPageLoad().url)
      val resolved = Await.result(result, 5 seconds)
      resolved.header.headers.keys should contain("Set-Cookie")
      resolved.header.headers("Set-Cookie") should include("PLAY_LANG=en")
    }
  }

  "makeChange" should {
    "return successful response" when {
      "a valid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody(
          "role" -> "some role",
          "endReason" -> "some end reason",
          "historicActiveRecord" -> "true"
        )
        val result = controller().onSubmit()(request)
        status(result) shouldBe OK
      }
    }

    "redirect the user" when {
      "an invalid form with errors is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("historicActiveRecord" -> "string")
        val result = controller().onSubmit()(request)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.HistoryController.onPageLoad().url)
      }
    }
  }
}

