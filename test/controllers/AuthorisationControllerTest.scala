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

import config.ApplicationConfig
import models._
import org.joda.time.LocalDate
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, OK, SEE_OTHER, contentAsString, defaultAwaitTimeout, redirectLocation}
import services.TimeService
import test_utils.TestData.{Cids, Ninos}
import test_utils.{TestConstants, TestUtility}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationControllerTest extends UnitSpec with TestUtility with OneAppPerSuite {

  implicit override lazy val app: Application = fakeApplication

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
