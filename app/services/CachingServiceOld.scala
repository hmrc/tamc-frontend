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
trait CachingServiceOld {

  def get[T](cacheKey: CacheKey[T])(implicit request: Request[_]): Future[Option[T]]
  def put[T](cacheKey: CacheKey[T], value: T)(implicit request: Request[_], format: Format[T]): Future[T]
  def clear()(implicit request: Request[_]): Future[Unit]

  def getUserAnswersCachedData(implicit request: Request[_]): Future[Option[UserAnswersCacheData]]
  def getCachedDataForEligibilityCheck(implicit request: Request[_]): Future[Option[EligibilityCheckCacheData]]
  def getUpdateRelationshipCachedData(implicit request: Request[_]): Future[Option[UpdateRelationshipCacheData]]
  def getConfirmationAnswers(implicit request: Request[_]): Future[Option[ConfirmationUpdateAnswersCacheData]]
}

object CacheService {
  val CACHE_DIVORCE_DATE: CacheKey[LocalDate] = CacheKey[LocalDate]("DIVORCE_DATE")
  val CACHE_MAKE_CHANGES_DECISION:CacheKey[String]  = CacheKey[String]("MAKE_CHANGES_DECISION")
  val CACHE_CHECK_CLAIM_OR_CANCEL:CacheKey[String] = CacheKey[String]("CHECK_CLAIM_OR_CANCEL")
  val CACHE_TRANSFEROR_RECORD:CacheKey[UserRecord] = CacheKey[UserRecord]("TRANSFEROR_RECORD")
  val CACHE_RECIPIENT_RECORD:CacheKey[RecipientRecord] = CacheKey[RecipientRecord]("RECIPIENT_RECORD")
  val CACHE_RECIPIENT_DETAILS:CacheKey[RecipientDetailsFormInput] = CacheKey[RecipientDetailsFormInput]("RECIPIENT_DETAILS")
  val CACHE_NOTIFICATION_RECORD:CacheKey[NotificationRecord] = CacheKey[NotificationRecord]("NOTIFICATION_RECORD")
  val CACHE_LOCKED_CREATE:CacheKey[Boolean] = CacheKey[Boolean]("LOCKED_CREATE")
  val CACHE_SELECTED_YEARS:CacheKey[List[Int]] = CacheKey[List[Int]]("SELECTED_YEARS")
  val CACHE_MARRIAGE_DATE:CacheKey[DateOfMarriageFormInput] = CacheKey[DateOfMarriageFormInput]("MARRIAGE_DATE")
  val CACHE_EMAIL_ADDRESS:CacheKey[EmailAddress] = CacheKey[EmailAddress]("EMAIL_ADDRESS")
  val CACHE_MA_ENDING_DATES:CacheKey[MarriageAllowanceEndingDates] = CacheKey[MarriageAllowanceEndingDates]("MA_ENDING_DATES")
  val CACHE_RELATIONSHIP_RECORDS:CacheKey[RelationshipRecords] = CacheKey[RelationshipRecords]("RELATIONSHIP_RECORDS")
  val CACHE_LOGGEDIN_USER_RECORD:CacheKey[LoggedInUserInfo] = CacheKey[LoggedInUserInfo]("LOGGEDIN_USER_RECORD")                          //FIXME is this key used properly?
  val CACHE_ACTIVE_RELATION_RECORD:CacheKey[RelationshipRecord] = CacheKey[RelationshipRecord]("ACTIVE_RELATION_RECORD")                  //FIXME is this key used properly?
  val CACHE_HISTORIC_RELATION_RECORD:CacheKey[Seq[RelationshipRecord]] = CacheKey[Seq[RelationshipRecord]]("HISTORIC_RELATION_RECORD")    //FIXME is this key used properly?
  val CACHE_RELATION_END_REASON_RECORD:CacheKey[EndRelationshipReason] = CacheKey[EndRelationshipReason]("RELATION_END_REASON_RECORD")    //FIXME is this key used properly?
  val CACHE_LOCKED_UPDATE:CacheKey[Boolean] = CacheKey[Boolean]("LOCKED_UPDATE")                                                          //FIXME is this key used properly?
  val CACHE_ROLE_RECORD:CacheKey[String] = CacheKey[String]("ROLE")                                                                       //FIXME is this key used properly?

  val CK_EXTRACT_USER_ANSWERS:CacheKey[UserAnswersCacheData] = CacheKey[UserAnswersCacheData](
    (cacheItem: CacheItem) => Some(UserAnswersCacheData(
        transferor = CACHE_TRANSFEROR_RECORD.extractor(cacheItem),
        recipient = CACHE_RECIPIENT_RECORD.extractor(cacheItem),
        notification = CACHE_NOTIFICATION_RECORD.extractor(cacheItem),
        relationshipCreated = CACHE_LOCKED_CREATE.extractor(cacheItem),
        selectedYears = CACHE_SELECTED_YEARS.extractor(cacheItem),
        recipientDetailsFormData = CACHE_RECIPIENT_DETAILS.extractor(cacheItem),
        dateOfMarriage = CACHE_MARRIAGE_DATE.extractor(cacheItem)
      )))

  val CK_EXTRACT_ELIGIBILITY_CHECK:CacheKey[EligibilityCheckCacheData] = CacheKey[EligibilityCheckCacheData](
    (cacheItem: CacheItem) => Some(EligibilityCheckCacheData(
        loggedInUserInfo = CACHE_LOGGEDIN_USER_RECORD.extractor(cacheItem),
        roleRecord = CACHE_ROLE_RECORD.extractor(cacheItem),
        activeRelationshipRecord = CACHE_ACTIVE_RELATION_RECORD.extractor(cacheItem),
        historicRelationships = CACHE_HISTORIC_RELATION_RECORD.extractor(cacheItem),
        notification = CACHE_NOTIFICATION_RECORD.extractor(cacheItem),
        relationshipEndReasonRecord = CACHE_RELATION_END_REASON_RECORD.extractor(cacheItem),
        relationshipUpdated = CACHE_LOCKED_UPDATE.extractor(cacheItem)
      )))

  val CK_EXTRACT_UPDATE_RELATIONSHIP:CacheKey[UpdateRelationshipCacheData] = CacheKey[UpdateRelationshipCacheData](
    (cacheItem: CacheItem) => Some(UpdateRelationshipCacheData(
        relationshipRecords = CACHE_RELATIONSHIP_RECORDS.extractor(cacheItem),
        email = CACHE_EMAIL_ADDRESS.extractor(cacheItem).map(_.value),
        endMaReason = CACHE_MAKE_CHANGES_DECISION.extractor(cacheItem),
        marriageEndDate = CACHE_MA_ENDING_DATES.extractor(cacheItem).map(_.marriageAllowanceEndDate)
      )))

  val CK_EXTRACT_CONFIRMATION:CacheKey[ConfirmationUpdateAnswersCacheData] = CacheKey[ConfirmationUpdateAnswersCacheData](
    (cacheItem: CacheItem) => Some(ConfirmationUpdateAnswersCacheData(
        relationshipRecords = CACHE_RELATIONSHIP_RECORDS.extractor(cacheItem),
        divorceDate = CACHE_DIVORCE_DATE.extractor(cacheItem),
        email = CACHE_EMAIL_ADDRESS.extractor(cacheItem).map(_.value),
        maEndingDates = CACHE_MA_ENDING_DATES.extractor(cacheItem)
      )))

  sealed abstract class CacheKey[T]{val dataKey: String; val extractor: CacheItem => Option[T]}
  object CacheKey {
    def apply[T](key: String)(implicit format: Reads[T]): CacheKey[T] = new CacheKey[T] {
      override val dataKey = key
      override val extractor: CacheItem => Option[T] =
        cache =>
          (cache.data \ dataKey)
            .validateOpt[T]
            .fold(e => throw JsResultException(e), identity)
    }

    def apply[T](extra: CacheItem => Option[T]): CacheKey[T] = new CacheKey[T]{
      override  val dataKey = "???"
      override val extractor: CacheItem => Option[T] = extra
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
  ) with CachingServiceOld {

  def get[T](cacheKey: CacheKey[T])(implicit request: Request[_]): Future[Option[T]] =
    cacheRepo
      .findById(request)
      .map(_.flatMap(ck => cacheKey.extractor.apply(ck)))

  def put[T](cacheKey: CacheKey[T], value: T)(implicit request: Request[_], format: Format[T]): Future[T] =
    putSession(DataKey[T](cacheKey.dataKey), value).map(_ => value)

  def clear()(implicit request: Request[_]): Future[Unit] =
    cacheRepo
      .deleteEntity(request)

  def getUserAnswersCachedData(implicit
    request: Request[_]
  ): Future[Option[UserAnswersCacheData]] =
    get[UserAnswersCacheData](CK_EXTRACT_USER_ANSWERS)

  def getCachedDataForEligibilityCheck(implicit
    request: Request[_]
  ): Future[Option[EligibilityCheckCacheData]] =
    get[EligibilityCheckCacheData](CK_EXTRACT_ELIGIBILITY_CHECK)

  def getUpdateRelationshipCachedData(implicit
    request: Request[_]
  ): Future[Option[UpdateRelationshipCacheData]] =
    get[UpdateRelationshipCacheData](CK_EXTRACT_UPDATE_RELATIONSHIP)

  def getConfirmationAnswers(implicit
    request: Request[_]
  ): Future[Option[ConfirmationUpdateAnswersCacheData]] =
    get[ConfirmationUpdateAnswersCacheData](CK_EXTRACT_CONFIRMATION)
}
