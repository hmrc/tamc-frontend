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
import play.api.libs.json.Format
import play.api.mvc.Request
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{DataKey, SessionCacheRepository}
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
}
