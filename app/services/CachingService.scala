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

package services

import com.google.inject.{ImplementedBy, Inject}
import models._
import play.api.Configuration
import play.api.libs.json.{Format, JsResultException, Reads}
import play.api.mvc.Request
import services.CacheService._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayJsonFormats.emailAddressReads
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.cache.{CacheItem, DataKey, SessionCacheRepository}
import uk.gov.hmrc.mongo.{MongoComponent, TimestampSupport}

import java.time.LocalDate
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CachingServiceImpl])
trait CachingService {
  def get[T](key: CacheReadKey[T])(implicit request: Request[_]): Future[Option[T]]
  def put[T](key: CacheWriteKey[T], value: T)(implicit request: Request[_], format: Format[T]): Future[T]
  def clear()(implicit request: Request[_]): Future[Unit]
}

object CacheService {

  val CACHE_DIVORCE_DATE: CacheWriteKey[LocalDate] = CacheKey[LocalDate]("DIVORCE_DATE")
  val CACHE_MAKE_CHANGES_DECISION:CacheWriteKey[String]  = CacheKey[String]("MAKE_CHANGES_DECISION")
  val CACHE_CHECK_CLAIM_OR_CANCEL:CacheWriteKey[String] = CacheKey[String]("CHECK_CLAIM_OR_CANCEL")
  val CACHE_TRANSFEROR_RECORD:CacheWriteKey[UserRecord] = CacheKey[UserRecord]("TRANSFEROR_RECORD")
  val CACHE_RECIPIENT_RECORD:CacheWriteKey[RecipientRecord] = CacheKey[RecipientRecord]("RECIPIENT_RECORD")
  val CACHE_RECIPIENT_DETAILS:CacheWriteKey[RecipientDetailsFormInput] = CacheKey[RecipientDetailsFormInput]("RECIPIENT_DETAILS")
  val CACHE_NOTIFICATION_RECORD:CacheWriteKey[NotificationRecord] = CacheKey[NotificationRecord]("NOTIFICATION_RECORD")
  val CACHE_LOCKED_CREATE:CacheWriteKey[Boolean] = CacheKey[Boolean]("LOCKED_CREATE")
  val CACHE_SELECTED_YEARS:CacheWriteKey[List[Int]] = CacheKey[List[Int]]("SELECTED_YEARS")
  val CACHE_MARRIAGE_DATE:CacheWriteKey[DateOfMarriageFormInput] = CacheKey[DateOfMarriageFormInput]("MARRIAGE_DATE")
  val CACHE_EMAIL_ADDRESS:CacheWriteKey[EmailAddress] = CacheKey[EmailAddress]("EMAIL_ADDRESS")
  val CACHE_MA_ENDING_DATES:CacheWriteKey[MarriageAllowanceEndingDates] = CacheKey[MarriageAllowanceEndingDates]("MA_ENDING_DATES")
  val CACHE_RELATIONSHIP_RECORDS:CacheWriteKey[RelationshipRecords] = CacheKey[RelationshipRecords]("RELATIONSHIP_RECORDS")
  val CACHE_LOGGEDIN_USER_RECORD:CacheWriteKey[LoggedInUserInfo] = CacheKey[LoggedInUserInfo]("LOGGEDIN_USER_RECORD")                          //FIXME is this key used properly?
  val CACHE_ACTIVE_RELATION_RECORD:CacheWriteKey[RelationshipRecord] = CacheKey[RelationshipRecord]("ACTIVE_RELATION_RECORD")                  //FIXME is this key used properly?
  val CACHE_HISTORIC_RELATION_RECORD:CacheWriteKey[Seq[RelationshipRecord]] = CacheKey[Seq[RelationshipRecord]]("HISTORIC_RELATION_RECORD")    //FIXME is this key used properly?
  val CACHE_RELATION_END_REASON_RECORD:CacheWriteKey[EndRelationshipReason] = CacheKey[EndRelationshipReason]("RELATION_END_REASON_RECORD")    //FIXME is this key used properly?
  val CACHE_LOCKED_UPDATE:CacheWriteKey[Boolean] = CacheKey[Boolean]("LOCKED_UPDATE")                                                          //FIXME is this key used properly?
  val CACHE_ROLE_RECORD:CacheWriteKey[String] = CacheKey[String]("ROLE")                                                                       //FIXME is this key used properly?

  val USER_ANSWERS_CACHE:CacheReadKey[UserAnswersCacheData] = CacheKey[UserAnswersCacheData](
    (cacheItem: CacheItem) => Some(UserAnswersCacheData(
        transferor = CACHE_TRANSFEROR_RECORD.reader(cacheItem),
        recipient = CACHE_RECIPIENT_RECORD.reader(cacheItem),
        notification = CACHE_NOTIFICATION_RECORD.reader(cacheItem),
        relationshipCreated = CACHE_LOCKED_CREATE.reader(cacheItem),
        selectedYears = CACHE_SELECTED_YEARS.reader(cacheItem),
        recipientDetailsFormData = CACHE_RECIPIENT_DETAILS.reader(cacheItem),
        dateOfMarriage = CACHE_MARRIAGE_DATE.reader(cacheItem)
      )))

  val USER_ANSWERS_ELIGIBILITY_CHECK:CacheReadKey[EligibilityCheckCacheData] = CacheKey[EligibilityCheckCacheData](
    (cacheItem: CacheItem) => Some(EligibilityCheckCacheData(
        loggedInUserInfo = CACHE_LOGGEDIN_USER_RECORD.reader(cacheItem),
        roleRecord = CACHE_ROLE_RECORD.reader(cacheItem),
        activeRelationshipRecord = CACHE_ACTIVE_RELATION_RECORD.reader(cacheItem),
        historicRelationships = CACHE_HISTORIC_RELATION_RECORD.reader(cacheItem),
        notification = CACHE_NOTIFICATION_RECORD.reader(cacheItem),
        relationshipEndReasonRecord = CACHE_RELATION_END_REASON_RECORD.reader(cacheItem),
        relationshipUpdated = CACHE_LOCKED_UPDATE.reader(cacheItem)
      )))

  val USER_ANSWERS_UPDATE_RELATIONSHIP:CacheReadKey[UpdateRelationshipCacheData] = CacheKey[UpdateRelationshipCacheData](
    (cacheItem: CacheItem) => Some(UpdateRelationshipCacheData(
        relationshipRecords = CACHE_RELATIONSHIP_RECORDS.reader(cacheItem),
        email = CACHE_EMAIL_ADDRESS.reader(cacheItem).map(_.value),
        endMaReason = CACHE_MAKE_CHANGES_DECISION.reader(cacheItem),
        marriageEndDate = CACHE_MA_ENDING_DATES.reader(cacheItem).map(_.marriageAllowanceEndDate)
      )))

  val USER_ANSWERS_UPDATE_CONFIRMATION:CacheReadKey[ConfirmationUpdateAnswersCacheData] = CacheKey[ConfirmationUpdateAnswersCacheData](
    (cacheItem: CacheItem) => Some(ConfirmationUpdateAnswersCacheData(
        relationshipRecords = CACHE_RELATIONSHIP_RECORDS.reader(cacheItem),
        divorceDate = CACHE_DIVORCE_DATE.reader(cacheItem),
        email = CACHE_EMAIL_ADDRESS.reader(cacheItem).map(_.value),
        maEndingDates = CACHE_MA_ENDING_DATES.reader(cacheItem)
      )))

  sealed abstract class CacheReadKey[T]{val reader: CacheItem => Option[T]}
  sealed abstract class CacheWriteKey[T] extends CacheReadKey[T] {val dataKey: DataKey[T]}

  object CacheKey {
    def apply[T](writableKey: String)(implicit format: Format[T]): CacheWriteKey[T] = new CacheWriteKey[T] {
      override val dataKey = DataKey[T](writableKey)
      override val reader: CacheItem => Option[T] =
        cache =>
          (cache.data \ writableKey)
            .validateOpt[T]
            .fold(e => throw JsResultException(e), identity)
    }

    def apply[T](read: CacheItem => Option[T]): CacheReadKey[T] = new CacheReadKey[T]{
      override val reader: CacheItem => Option[T] = read
    }
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
  ) with CachingService {

  def get[T](key: CacheReadKey[T])(implicit request: Request[_]): Future[Option[T]] =
    cacheRepo
      .findById(request)
      .map(_.flatMap(key.reader))

  def put[T](key: CacheWriteKey[T], value: T)(implicit request: Request[_], format: Format[T]): Future[T] =
    cacheRepo
      .put[T](request)(key.dataKey, value)
      .map(key.reader)
      .map(_.getOrElse(throw new RuntimeException(s"Failed to retrieve ${key.dataKey} from cache after saving")))

  def clear()(implicit request: Request[_]): Future[Unit] =
    cacheRepo
      .deleteEntity(request)
}
