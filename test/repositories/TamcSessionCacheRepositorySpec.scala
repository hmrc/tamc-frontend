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

package repositories

import models.{Gender, RecipientRecord, RegistrationFormInput, UserAnswersCacheData, UserRecord}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.Configuration
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.SessionCacheNew.CacheKey.{CACHED_USER_ANSWERS, RECIPIENT_RECORD, TRANSFEROR_RECORD}
import test_utils.TestData.Cids
import test_utils.data.RecipientRecordData
import test_utils.data.RecipientRecordData.citizenName
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.CurrentTimestampSupport
import uk.gov.hmrc.mongo.test.MongoSupport
import utils.UnitSpec

import java.time.LocalDate
import java.util.UUID
import scala.concurrent.ExecutionContext

class TamcSessionCacheRepositorySpec  extends UnitSpec with MongoSupport {


  private val configuration = Configuration(
    "mongodb.timeToLiveInSeconds" -> "60",
  )

  private val sut = new TamcSessionCacheRepository(
    mongoComponent,
    configuration,
    timestampSupport = new CurrentTimestampSupport()
  )(ExecutionContext.Implicits.global)

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  "The RepaymentsSecureSessionCacheRepository" must {

    "support cache operations for Bank Details" in {

      val userRecord: UserRecord = UserRecord(Cids.cid1, "2015", None, Some(citizenName))
      val recipientData: RegistrationFormInput = RegistrationFormInput("First", "Last", Gender("F"), Nino("AA000000A"), LocalDate.of(1,2,3))
      val recipientRecord = RecipientRecord(RecipientRecordData.userRecord, recipientData, Nil)

      implicit val request: Request[_] = FakeRequest().withSession(SessionKeys.sessionId -> UUID.randomUUID().toString)

      eventually(await(sut.improvedGet(CACHED_USER_ANSWERS)) shouldBe None)
      eventually(await(sut.improvedPut(TRANSFEROR_RECORD, userRecord)) shouldBe userRecord)
      eventually(await(sut.improvedGet(CACHED_USER_ANSWERS)) shouldBe Some(UserAnswersCacheData(transferor = Some(userRecord), recipient = None, notification = None)))
      eventually(await(sut.improvedPut(RECIPIENT_RECORD, recipientRecord)) shouldBe recipientRecord)
      eventually(await(sut.improvedGet(CACHED_USER_ANSWERS)) shouldBe Some(UserAnswersCacheData(transferor = Some(userRecord), recipient = Some(recipientRecord), notification = None)))
      eventually(await(sut.clear()) shouldBe ())
      eventually(await(sut.improvedGet(CACHED_USER_ANSWERS)) shouldBe None)
    }}
}
