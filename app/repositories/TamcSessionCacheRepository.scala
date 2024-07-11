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

import com.google.inject.{ImplementedBy, Inject}
import models.{RecipientRecord, UserAnswersCacheData, UserRecord}
import play.api.Configuration
import play.api.libs.json.{Format, JsResultException}
import play.api.mvc.Request
import repositories.SessionCacheNew.{CacheKey, CacheKeyReadWrite}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TamcSessionCacheRepository])
trait SessionCacheNew {
  def put[T](key: String, value: T)(implicit format: Format[T], request: Request[_]): Future[T]
  def clear()(implicit request: Request[_]): Future[Unit]
}

class TamcSessionCacheRepository @Inject() (
  mongoComponent: MongoComponent,
  config: Configuration,
  timestampSupport: TimestampSupport
)(implicit ec: ExecutionContext)
    extends SessionCacheRepository(
      mongoComponent = mongoComponent,
      collectionName = "session-cache",
      ttl = Duration(config.get[Int]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS),
      timestampSupport = timestampSupport,
      sessionIdKey = SessionKeys.sessionId
    )
    with SessionCacheNew {

  override def put[T](key: String, value: T)(implicit format: Format[T], request: Request[_]): Future[T] =
    putSession(DataKey[T](key), value).map(_ => value)

  override def clear()(implicit request: Request[_]): Future[Unit] =
    cacheRepo.deleteEntity(request)

  def put[T](cacheKey: CacheKeyReadWrite[T], value: T)(implicit format: Format[T], request: Request[_]): Future[T] =
    putSession(cacheKey.dataKey, value).map(_ => value)

  def get[T](cacheKey: CacheKey[T])(implicit request: Request[_]): Future[Option[T]] =
    cacheRepo.findById(request).map(_.flatMap(cacheKey))
}

object SessionCacheNew {

  sealed abstract class CacheKey[T] extends Function1[CacheItem, Option[T]]
  sealed abstract class CacheKeyReadWrite[T] extends CacheKey[T] {val dataKey: DataKey[T]}

  object CacheKey {
    def apply[T](key: String)(implicit format: Format[T]): CacheKeyReadWrite[T] = new CacheKeyReadWrite[T] {
      override def apply(cacheItem: CacheItem): Option[T] =
        (cacheItem.data \ key)
          .validateOpt[T]
          .fold(e => throw JsResultException(e), identity)
      override val dataKey: DataKey[T] = DataKey[T](key)
    }

    def apply[T](transformation: CacheItem => T): CacheKey[T] = new CacheKey[T] {
      override def apply(cacheItem: CacheItem): Option[T] = Some(transformation(cacheItem))
    }

    val TRANSFEROR_RECORD:CacheKeyReadWrite[UserRecord] = CacheKey[UserRecord]("TRANSFEROR_RECORD")
    val RECIPIENT_RECORD:CacheKeyReadWrite[RecipientRecord] = CacheKey[RecipientRecord]("RECIPIENT_RECORD")

    val CACHED_USER_ANSWERS:CacheKey[UserAnswersCacheData] = CacheKey[UserAnswersCacheData]({
      cacheItem:CacheItem =>
        UserAnswersCacheData(
          transferor = TRANSFEROR_RECORD.apply(cacheItem),
          recipient = RECIPIENT_RECORD.apply(cacheItem),
          None,None
        )
    })
  }

}
