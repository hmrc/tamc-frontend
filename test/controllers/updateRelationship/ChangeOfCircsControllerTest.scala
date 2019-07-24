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

import controllers.{ControllerBaseSpec, UpdateRelationshipController}
import controllers.actions.AuthenticatedActionRefiner
import models.EndRelationshipReason
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContent, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, redirectLocation}
import services._
import uk.gov.hmrc.play.partials.FormPartialRetriever
import uk.gov.hmrc.renderer.TemplateRenderer
import uk.gov.hmrc.time
import play.api.test.Helpers._

import scala.concurrent.Future

class ChangeOfCircsControllerTest extends ControllerBaseSpec {

  val mockRegistrationService: TransferService = mock[TransferService]
  val mockUpdateRelationshipService: UpdateRelationshipService = mock[UpdateRelationshipService]
  val mockListRelationshipService: ListRelationshipService = mock[ListRelationshipService]
  val mockCachingService: CachingService = mock[CachingService]
  val mockTimeService: TimeService = mock[TimeService]

  def controller(auth: AuthenticatedActionRefiner = instanceOf[AuthenticatedActionRefiner]): ChangeOfCircsController =
    new ChangeOfCircsController(
      messagesApi,
      auth,
      mockUpdateRelationshipService,
      mockListRelationshipService,
      mockRegistrationService,
      mockCachingService,
      mockTimeService
    )(instanceOf[TemplateRenderer], instanceOf[FormPartialRetriever])

  class UpdateRelationshipActionTest(endReason: String) {
    val request: Request[AnyContent] = FakeRequest().withFormUrlEncodedBody(
      "role" -> "some role",
      "endReason" -> endReason,
      "historicActiveRecord" -> "true",
      "creationTimestamp" -> "timestamp",
      "dateOfDivorce" -> new LocalDate(time.TaxYear.current.startYear, 6, 12).toString()
    )
    when(mockCachingService.saveRoleRecord(any())(any(), any())).thenReturn("OK")
    when(mockUpdateRelationshipService.saveEndRelationshipReason(any())(any(), any()))
      .thenReturn(Future.successful(EndRelationshipReason("")))
    val result: Future[Result] = controller().updateRelationshipAction()(request)
    lazy val document: Document = Jsoup.parse(contentAsString(result))
  }

  "updateRelationshipAction" should {
    "return a success" when {
      "the end reason code is DIVORCE" in new UpdateRelationshipActionTest("DIVORCE") {
        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("title.divorce")
      }

      "the end reason code is EARNINGS" in new UpdateRelationshipActionTest("EARNINGS") {
        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("change.status.earnings.h1")
      }

      "the end reason code is BEREAVEMENT" in new UpdateRelationshipActionTest("BEREAVEMENT") {
        status(result) shouldBe OK
        document.getElementsByTag("h1").first().text() shouldBe messagesApi("change.status.bereavement.sorry")
      }
    }

    "redirect the user" when {
      "the end reason code is CANCEL" in new UpdateRelationshipActionTest("CANCEL") {
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmCancel().url)
      }

      "the end reason code is REJECT" in new UpdateRelationshipActionTest("REJECT") {
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.UpdateRelationshipController.confirmReject().url)
      }
    }

    "return a bad request" when {
      "an invalid form is submitted" in {
        val request = FakeRequest().withFormUrlEncodedBody("role" -> "ROLE", "historicActiveRecord" -> "string")
        val result: Future[Result] = controller().updateRelationshipAction()(request)
        status(result) shouldBe BAD_REQUEST
      }

      "an unrecognised end reason is submitted" in new UpdateRelationshipActionTest("DIVORCE_PY") {
        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "changeOfIncome" should {
    "return success" in {
      status(controller().changeOfIncome()(request)) shouldBe OK
    }
  }

  "bereavement" should {
    "return success" in {
      status(controller().bereavement()(request)) shouldBe OK
    }
  }
}
