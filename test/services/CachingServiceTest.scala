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

package services

import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.mustBe
import org.mockito.ArgumentMatchers.{any, eq as meq}
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.Request
import play.api.test.{FakeHeaders, FakeRequest}
import services.CacheService.CacheKey
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import uk.gov.hmrc.mongo.cache.{DataKey, SessionCacheRepository}
import utils.BaseTest

import scala.concurrent.Future
import scala.language.postfixOps


class CachingServiceTest extends BaseTest {
  val mockMongoComponent: MongoComponent = mock[MongoComponent]
  val mockTimestampSupport: TimestampSupport = mock[TimestampSupport]
  val mockSessionCacheRepo: SessionCacheRepository = mock[SessionCacheRepository]
  val fakeSessionId = "test-session-id"
  val fakeRequest: Request[?] = FakeRequest()
    .withHeaders(FakeHeaders())
    .withSession(SessionKeys.sessionId -> fakeSessionId)


  val cachingService = app.injector.instanceOf[CachingService]

  implicit val mockRequest: Request[?] = mock[Request[?]]
  implicit val mockWrites: Writes[String] = mock[Writes[String]]
  implicit val mockReads: Reads[String] = mock[Reads[String]]

  "CachingServiceImpl" should {
    "retrieve a value from the cache when it exists" in {
      val cacheKey = CacheKey("testKey")
      val dataKey = DataKey[String](cacheKey.dataKey.unwrap)
      val cachedValue = "cachedValue"

      when(mockSessionCacheRepo.getFromSession(dataKey))
        .thenReturn(Future.successful(Some(cachedValue)))

      cachingService.get[String](cacheKey)(fakeRequest).map { result =>
        result mustBe Some(cachedValue)
      }
    }

    "return None when the cache does not contain a value" in {
      val cacheKey = CacheKey("missingKey")
      val dataKey = DataKey[String](cacheKey.dataKey.unwrap)

      when(mockSessionCacheRepo.getFromSession(dataKey))
        .thenReturn(Future.successful(None))

      cachingService.get[String](cacheKey)(fakeRequest).map { result =>
        result mustBe None
      }
    }

    "delete a value from the cache" in {
      val dataKey = DataKey[String]("someKey")

      when(mockSessionCacheRepo.deleteFromSession(dataKey))
        .thenReturn(Future.successful((): Unit))

      cachingService.clear()(fakeRequest).map { _ =>
        succeed
      }
    }
  }
}
