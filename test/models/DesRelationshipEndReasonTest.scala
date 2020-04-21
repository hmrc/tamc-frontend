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

package models

import models.DesRelationshipEndReason._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import uk.gov.hmrc.play.test.UnitSpec

class DesRelationshipEndReasonTest extends UnitSpec with GuiceOneAppPerSuite{

  "RelationshipEndReasonHodsReads" should{
    "read the HODS value for correct RelationshipEndReason" in{

      JsString("DEATH").as[DesRelationshipEndReason] shouldBe Death
      JsString("DIVORCE").as[DesRelationshipEndReason] shouldBe DesRelationshipEndReason.Divorce
      JsString("INVALID_PARTICIPANT").as[DesRelationshipEndReason] shouldBe InvalidParticipant
      JsString("CANCELLED").as[DesRelationshipEndReason] shouldBe Cancelled
      JsString("REJECTED").as[DesRelationshipEndReason] shouldBe Rejected
      JsString("HMRC").as[DesRelationshipEndReason] shouldBe Hmrc
      JsString("CLOSED").as[DesRelationshipEndReason] shouldBe Closed
      JsString("MERGER").as[DesRelationshipEndReason] shouldBe Merger
      JsString("RETROSPECTIVE").as[DesRelationshipEndReason] shouldBe Retrospective
      JsString("SYSTEM").as[DesRelationshipEndReason] shouldBe System
      JsString("Active").as[DesRelationshipEndReason] shouldBe DesRelationshipEndReason.Active
      JsString("DEFAULT").as[DesRelationshipEndReason] shouldBe Default
    }
    "read Default if the reason is not recognised" in{
      JsString("dafdasfa").as[DesRelationshipEndReason] shouldBe Default
      JsString("Some other reason").as[DesRelationshipEndReason] shouldBe Default
      JsString("Other").as[DesRelationshipEndReason] shouldBe Default
    }
    "return JsResultException if the value is not string" in{
      a[JsResultException] shouldBe thrownBy(JsNull.as[DesRelationshipEndReason])
      a[JsResultException] shouldBe thrownBy(JsNumber(21).as[DesRelationshipEndReason])
    }
  }
  "writes" should{
    "write the value to the Json correctly" in{
      Json.toJson(Death) shouldBe JsString(Death.value)
      Json.toJson(InvalidParticipant) shouldBe JsString(InvalidParticipant.value)
    }
  }
}
