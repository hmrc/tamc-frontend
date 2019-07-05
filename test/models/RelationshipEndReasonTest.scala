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

package models

import models.RelationshipEndReason._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec

class RelationshipEndReasonTest extends UnitSpec with GuiceOneAppPerSuite{

  "RelationshipEndReasonHodsReads" should{
    "read the HODS value for correct RelationshipEndReason" in{

      JsString("DEATH").as[RelationshipEndReason] shouldBe Death
      JsString("DIVORCE").as[RelationshipEndReason] shouldBe Divorce
      JsString("INVALID_PARTICIPANT").as[RelationshipEndReason] shouldBe InvalidParticipant
      JsString("CANCELLED").as[RelationshipEndReason] shouldBe Cancelled
      JsString("REJECTED").as[RelationshipEndReason] shouldBe Rejected
      JsString("HMRC").as[RelationshipEndReason] shouldBe Hmrc
      JsString("CLOSED").as[RelationshipEndReason] shouldBe Closed
      JsString("MERGER").as[RelationshipEndReason] shouldBe Merger
      JsString("RETROSPECTIVE").as[RelationshipEndReason] shouldBe Retrospective
      JsString("SYSTEM").as[RelationshipEndReason] shouldBe System
      JsString("Active").as[RelationshipEndReason] shouldBe Active
      JsString("DEFAULT").as[RelationshipEndReason] shouldBe Default
    }
    "read Default if the reason is not recognised" in{
      JsString("dafdasfa").as[RelationshipEndReason] shouldBe Default
      JsString("Some other reason").as[RelationshipEndReason] shouldBe Default
      JsString("Other").as[RelationshipEndReason] shouldBe Default
    }
    "return JsResultException if the value is not string" in{
      a[JsResultException] shouldBe thrownBy(JsNull.as[RelationshipEndReason])
      a[JsResultException] shouldBe thrownBy(JsNumber(21).as[RelationshipEndReason])
    }
  }
  "writes" should{
    "write the value to the Json correctly" in{
      Json.toJson(Death) shouldBe JsString(Death.value)
      Json.toJson(InvalidParticipant) shouldBe JsString(InvalidParticipant.value)
    }
  }
}
