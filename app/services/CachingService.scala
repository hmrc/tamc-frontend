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

import com.google.inject.{ImplementedBy, Inject}
import models._
import play.api.Configuration
import play.api.libs.json.{Format, JsResultException}
import play.api.mvc.Request
import services.CacheService.CacheKey.{CacheReadKey, CacheReadWriteKey}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}
import utils.EmailAddress
import utils.emailAddressFormatters.PlayJsonFormats._

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CachingServiceImpl])
trait CachingService {
  def get[T](key: CacheReadKey[T])(implicit request: Request[_]): Future[Option[T]]
  def put[T](key: CacheReadWriteKey[T], value: T)(implicit request: Request[_], format: Format[T]): Future[T]
  def clear()(implicit request: Request[_]): Future[Unit]
}

object CacheService {

  val CACHE_DIVORCE_DATE: CacheReadWriteKey[LocalDate]                           = CacheKey[LocalDate]("DIVORCE_DATE")
  val CACHE_MAKE_CHANGES_DECISION: CacheReadWriteKey[String]                     = CacheKey[String]("MAKE_CHANGES_DECISION")
  val CACHE_CHECK_CLAIM_OR_CANCEL: CacheReadWriteKey[String]                     = CacheKey[String]("CHECK_CLAIM_OR_CANCEL")
  val CACHE_TRANSFEROR_RECORD: CacheReadWriteKey[UserRecord]                     = CacheKey[UserRecord]("TRANSFEROR_RECORD")
  val CACHE_RECIPIENT_RECORD: CacheReadWriteKey[RecipientRecord]                 = CacheKey[RecipientRecord]("RECIPIENT_RECORD")
  val CACHE_RECIPIENT_DETAILS: CacheReadWriteKey[RecipientDetailsFormInput]      = CacheKey[RecipientDetailsFormInput]("RECIPIENT_DETAILS")
  val CACHE_NOTIFICATION_RECORD: CacheReadWriteKey[NotificationRecord]           = CacheKey[NotificationRecord]("NOTIFICATION_RECORD")
  val CACHE_LOCKED_CREATE: CacheReadWriteKey[Boolean]                            = CacheKey[Boolean]("LOCKED_CREATE")
  val CACHE_SELECTED_YEARS: CacheReadWriteKey[List[Int]]                         = CacheKey[List[Int]]("SELECTED_YEARS")
  val CACHE_MARRIAGE_DATE: CacheReadWriteKey[DateOfMarriageFormInput]            = CacheKey[DateOfMarriageFormInput]("MARRIAGE_DATE")
  val CACHE_EMAIL_ADDRESS: CacheReadWriteKey[EmailAddress]                       = CacheKey[EmailAddress]("EMAIL_ADDRESS")
  val CACHE_MA_ENDING_DATES: CacheReadWriteKey[MarriageAllowanceEndingDates]     = CacheKey[MarriageAllowanceEndingDates]("MA_ENDING_DATES")
  val CACHE_RELATIONSHIP_RECORDS: CacheReadWriteKey[RelationshipRecords]         = CacheKey[RelationshipRecords]("RELATIONSHIP_RECORDS")
  val CACHE_LOGGEDIN_USER_RECORD: CacheReadWriteKey[LoggedInUserInfo]            = CacheKey[LoggedInUserInfo]("LOGGEDIN_USER_RECORD")             // TODO is this key required?
  val CACHE_ACTIVE_RELATION_RECORD: CacheReadWriteKey[RelationshipRecord]        = CacheKey[RelationshipRecord]("ACTIVE_RELATION_RECORD")         // TODO is this key required?
  val CACHE_HISTORIC_RELATION_RECORD: CacheReadWriteKey[Seq[RelationshipRecord]] = CacheKey[Seq[RelationshipRecord]]("HISTORIC_RELATION_RECORD")  // TODO is this key required?
  val CACHE_RELATION_END_REASON_RECORD: CacheReadWriteKey[EndRelationshipReason] = CacheKey[EndRelationshipReason]("RELATION_END_REASON_RECORD")  // TODO is this key required?
  val CACHE_LOCKED_UPDATE: CacheReadWriteKey[Boolean]                            = CacheKey[Boolean]("LOCKED_UPDATE")                             // TODO is this key required?
  val CACHE_ROLE_RECORD: CacheReadWriteKey[String]                               = CacheKey[String]("ROLE")                                       // TODO is this key required?
  val CACHE_CHOOSE_YEARS: CacheReadWriteKey[String]                              = CacheKey[String]("CHOOSE_YEARS")                                       // TODO is this key required?

  val USER_ANSWERS_CACHE: CacheReadKey[UserAnswersCacheData] = CacheKey[UserAnswersCacheData]((cacheItem: CacheItem) =>
    Some(
      UserAnswersCacheData(
        transferor = CACHE_TRANSFEROR_RECORD.read(cacheItem),
        recipient = CACHE_RECIPIENT_RECORD.read(cacheItem),
        notification = CACHE_NOTIFICATION_RECORD.read(cacheItem),
        relationshipCreated = CACHE_LOCKED_CREATE.read(cacheItem),
        selectedYears = CACHE_SELECTED_YEARS.read(cacheItem),
        recipientDetailsFormData = CACHE_RECIPIENT_DETAILS.read(cacheItem),
        dateOfMarriage = CACHE_MARRIAGE_DATE.read(cacheItem)
      )
    )
  )

  val USER_ANSWERS_ELIGIBILITY_CHECK: CacheReadKey[EligibilityCheckCacheData] =
    CacheKey[EligibilityCheckCacheData]((cacheItem: CacheItem) =>
      Some(
        EligibilityCheckCacheData(
          loggedInUserInfo = CACHE_LOGGEDIN_USER_RECORD.read(cacheItem),
          roleRecord = CACHE_ROLE_RECORD.read(cacheItem),
          activeRelationshipRecord = CACHE_ACTIVE_RELATION_RECORD.read(cacheItem),
          historicRelationships = CACHE_HISTORIC_RELATION_RECORD.read(cacheItem),
          notification = CACHE_NOTIFICATION_RECORD.read(cacheItem),
          relationshipEndReasonRecord = CACHE_RELATION_END_REASON_RECORD.read(cacheItem),
          relationshipUpdated = CACHE_LOCKED_UPDATE.read(cacheItem)
        )
      )
    )

  val USER_ANSWERS_UPDATE_RELATIONSHIP: CacheReadKey[UpdateRelationshipCacheData] =
    CacheKey[UpdateRelationshipCacheData]((cacheItem: CacheItem) =>
      Some(
        UpdateRelationshipCacheData(
          relationshipRecords = CACHE_RELATIONSHIP_RECORDS.read(cacheItem),
          email = CACHE_EMAIL_ADDRESS.read(cacheItem).map(_.value),
          endMaReason = CACHE_MAKE_CHANGES_DECISION.read(cacheItem),
          marriageEndDate = CACHE_MA_ENDING_DATES.read(cacheItem).map(_.marriageAllowanceEndDate)
        )
      )
    )

  val USER_ANSWERS_UPDATE_CONFIRMATION: CacheReadKey[ConfirmationUpdateAnswersCacheData] =
    CacheKey[ConfirmationUpdateAnswersCacheData]((cacheItem: CacheItem) =>
      Some(
        ConfirmationUpdateAnswersCacheData(
          relationshipRecords = CACHE_RELATIONSHIP_RECORDS.read(cacheItem),
          divorceDate = CACHE_DIVORCE_DATE.read(cacheItem),
          email = CACHE_EMAIL_ADDRESS.read(cacheItem).map(_.value),
          maEndingDates = CACHE_MA_ENDING_DATES.read(cacheItem)
        )
      )
    )

  object CacheKey {
    def apply[T](readWriteKey: String)(implicit format: Format[T]): CacheReadWriteKey[T] = new CacheReadWriteKey[T] {
      override val dataKey                      = DataKey[T](readWriteKey)
      override val read: CacheItem => Option[T] =
        cache =>
          (cache.data \ readWriteKey)
            .validateOpt[T]
            .fold(e => throw JsResultException(e), identity)
    }

    def apply[T](reader: CacheItem => Option[T]): CacheReadKey[T] = new CacheReadKey[T] {
      override val read: CacheItem => Option[T] = reader
    }

    sealed abstract class CacheReadKey[T] { val read: CacheItem => Option[T] }
    sealed abstract class CacheReadWriteKey[T] extends CacheReadKey[T] { val dataKey: DataKey[T] }
  }
}

class CachingServiceImpl @Inject() (
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
    with CachingService {

  def get[T](key: CacheReadKey[T])(implicit request: Request[_]): Future[Option[T]] =
    cacheRepo
      .findById(request)
      .map(_.flatMap(key.read))

  def put[T](key: CacheReadWriteKey[T], value: T)(implicit request: Request[_], format: Format[T]): Future[T] =
    cacheRepo
      .put[T](request)(key.dataKey, value)
      .map(key.read)
      .map(_.getOrElse(throw new RuntimeException(s"Failed to retrieve ${key.dataKey} from cache after saving")))

  def clear()(implicit request: Request[_]): Future[Unit] =
    cacheRepo
      .deleteEntity(request)
}
