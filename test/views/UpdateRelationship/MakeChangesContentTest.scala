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

package views.UpdateRelationship

import controllers.UpdateRelationship.MakeChangesController
import controllers.actions.AuthRetrievals
import controllers.auth.PertaxAuthAction
import forms.coc._
import helpers.FakePertaxAuthAction
import models.auth.AuthenticatedUserRequest
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{FakeRequest, Injecting}
import services._
import uk.gov.hmrc.domain.Nino
import utils.{BaseTest, MockAuthenticatedAction, NinoGenerator}
import views.html.coc.reason_for_change

import java.util.Locale
import scala.concurrent.Future

class MakeChangesContentTest extends BaseTest with Injecting with NinoGenerator {

  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[UpdateRelationshipService].toInstance(mockUpdateRelationshipService),
      bind[AuthRetrievals].to[MockAuthenticatedAction],
      bind[PertaxAuthAction].to[FakePertaxAuthAction]
    ).build()

  lazy val controller: MakeChangesController = app.injector.instanceOf[MakeChangesController]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpdateRelationshipService)
  }

  "Update relationship cause - get view" should {
    "show all appropriate radio buttons" in {
      implicit val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])
      val view = inject[reason_for_change]

      implicit val request: AuthenticatedUserRequest[_] = AuthenticatedUserRequest(FakeRequest(), None, true, None, Nino(nino))
      lazy val nino = generateNino().nino

      val expectedRadioButtons = Seq(
        "Do not need Marriage Allowance any more",
        "Divorce, end of civil partnership or legally separated",
        "Bereavement"
      ).toArray
      when(mockUpdateRelationshipService.getMakeChangesDecision(any()))
        .thenReturn(Future.successful(None))
      val document = Jsoup.parse(view(MakeChangesDecisionForm.form()).toString())
      val radioButtons = document.getElementsByClass("govuk-label govuk-radios__label").eachText().toArray()

      radioButtons.length shouldBe expectedRadioButtons.length
      radioButtons shouldBe expectedRadioButtons
    }
  }
}
