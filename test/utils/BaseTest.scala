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

package utils

import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait BaseTest extends UnitSpec with GuiceOneAppPerSuite with BeforeAndAfterEach with SCAWrapperMockHelper {

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false
    )
    .overrides(featureFlagServiceBinding)
    .build()

  override def beforeEach(): Unit = {
    Mockito.reset()
    featureFlagSCAWrapperMock()
  }


  implicit lazy val messages = Helpers.stubMessages()
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def instanceOf[T](implicit evidence: scala.reflect.ClassTag[T]): T = app.injector.instanceOf[T]

}
