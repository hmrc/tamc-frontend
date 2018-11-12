/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import test_utils.TestUtility
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerTest extends UnitSpec with TestUtility with GuiceOneAppPerSuite {

  val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  val fakeCustomAuditConnector = new AuditConnector {
    override lazy val auditingConfig = ???
    var auditEventsToTest: List[DataEvent] = List()

    override def sendEvent(event: DataEvent)(implicit hc: HeaderCarrier = HeaderCarrier(), ec: ExecutionContext): Future[AuditResult] = {
      auditEventsToTest = auditEventsToTest :+ event
      Future {
        AuditResult.Success
      }
    }
  }

  "Calling notAuthorised" should {
    "return OK" in {
      val controller = makeFakeHomeController
      val request = FakeRequest()
      val result = await(controller.notAuthorised.apply(request))
      status(result) shouldBe OK
    }
  }

  "Calling sessionTimeout" should {
    "return OK" in {
      val controller = makeFakeHomeController
      val request = FakeRequest()
      val result = await(controller.sessionTimeout.apply(request))
      status(result) shouldBe OK
    }
  }

}
