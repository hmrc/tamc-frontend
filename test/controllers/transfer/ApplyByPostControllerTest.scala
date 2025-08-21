/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.transfer

import controllers.ControllerViewTestHelper
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import helpers.FakePertaxAuthAction
import models.{CurrentAndPreviousYearsEligibility, TaxYear}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import play.api.test.Injecting
import services.{CachingService, TransferService}
import test_utils.data.RecipientRecordData
import utils.{ControllerBaseTest, MockAuthenticatedAction}
import views.html.multiyear.transfer.apply_by_post

import scala.concurrent.Future

class ApplyByPostControllerTest extends ControllerBaseTest with ControllerViewTestHelper with Injecting {

  val mockTransferService: TransferService = mock[TransferService]
  val mockCachingService: CachingService = mock[CachingService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[TransferService].toInstance(mockTransferService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[MessagesApi].toInstance(stubMessagesApi()),
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: ApplyByPostController = app.injector.instanceOf[ApplyByPostController]

  val applyByPostView: apply_by_post = inject[views.html.multiyear.transfer.apply_by_post]

  "applyByPost" should {
    "display the Apply By Post page" in {
      val currentYearAvailable = true
      when(mockTransferService.getCurrentAndPreviousYearsEligibility(any(), any()))
        .thenReturn(Future.successful(
          CurrentAndPreviousYearsEligibility(
            currentYearAvailable = true,
            List(TaxYear(2014)),
            RecipientRecordData.recipientRecord.data,
            RecipientRecordData.recipientRecord.availableTaxYears
          )))

      val cachedData = Seq("previousTaxYears")
      when(mockCachingService.get[String](any())(any()))
        .thenReturn(Future.successful(cachedData))

      val result = controller.applyByPost(request)

      status(result).shouldBe(OK)

      result `rendersTheSameViewAs` applyByPostView(cachedData, currentYearAvailable)
    }
  }

}
