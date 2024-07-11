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
import play.api.Configuration
import play.api.libs.json.{Format, JsResultException}
import play.api.mvc.Request
import repositories.SessionCacheNew.{CacheKeyRead, CacheKeyReadWrite}
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

  def get[T](cacheKey: CacheKeyRead[T])(implicit request: Request[_]): Future[Option[T]] =
    cacheRepo.findById(request).map(_.flatMap(cacheKey))
}

object SessionCacheNew {

  sealed abstract class CacheKeyRead[T] extends Function1[CacheItem, Option[T]]
  sealed abstract class CacheKeyReadWrite[T] extends CacheKeyRead[T] {val dataKey: DataKey[T]}

  object CacheKeyRead {
    def apply[T](key: String)(implicit format: Format[T]): CacheKeyReadWrite[T] = new CacheKeyReadWrite[T] {
      override def apply(cacheItem: CacheItem): Option[T] =
        (cacheItem.data \ key)
          .validateOpt[T]
          .fold(e => throw JsResultException(e), identity)
      override val dataKey: DataKey[T] = DataKey[T](key)
    }

    def apply[T](transformation: CacheItem => T): CacheKeyRead[T] = new CacheKeyRead[T] {
      override def apply(cacheItem: CacheItem): Option[T] = Some(transformation(cacheItem))
    }
  }


}
