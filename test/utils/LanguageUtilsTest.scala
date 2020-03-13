/*
 * Copyright 2020 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.i18n.{Lang, MessagesApi}
import uk.gov.hmrc.play.test.UnitSpec

class LanguageUtilsTest extends UnitSpec with GuiceOneAppPerTest {

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  "LanguageUtils" should {

    "return true if the langauge is set to Welsh" in {
      val messages = messagesApi.preferred(Seq(Lang("cy")))
      LanguageUtils.isWelsh(messages) shouldBe true

    }

    "return false if the language is not set to Welsh" in {
      val messages = messagesApi.preferred(Seq(Lang("en")))
      LanguageUtils.isWelsh(messages) shouldBe false
    }
  }

}
