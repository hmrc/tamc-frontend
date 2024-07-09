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

import com.google.inject.Inject
import com.google.inject.name.Named
import config.ApplicationConfig
import models._
import play.api.libs.json.{Format, Reads}
import play.api.mvc.Request
import repositories.SessionCacheNew
import uk.gov.hmrc.http.cache.client.SessionCache
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class CachingService @Inject() (
  val http: HttpClient,
  appConfig: ApplicationConfig,
  sessionCacheNew: SessionCacheNew,
  @Named("appName") appName: String
) extends SessionCache {

  override lazy val defaultSource: String = appName
  override lazy val baseUri: String       = appConfig.cacheUri
  override lazy val domain: String        = appConfig.sessionCacheDomain

  def put[T](key: String, value: T)(implicit request: Request[_], format: Format[T], hc: HeaderCarrier, executionContext: ExecutionContext): Future[T] =
    cache[T](key, value)
      .map(_.getEntry[T](key).getOrElse(throw new RuntimeException(s"Failed to retrieve $key from cache after saving")))
      .andThen(_ => sessionCacheNew.put[T](key, value))

  def clear()(implicit request: Request[_], hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] =
    remove()
      .map(_ => ())
      .andThen(_ => sessionCacheNew.clear())

  def get[T](key: String)(implicit reads: Reads[T], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[T]] =
    fetchAndGetEntry[T](key)

  def getUserAnswersCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserAnswersCacheData]] =
    fetch() map (_ map (cacheMap =>
      UserAnswersCacheData(
        transferor = cacheMap.getEntry[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD),
        recipient = cacheMap.getEntry[RecipientRecord](appConfig.CACHE_RECIPIENT_RECORD),
        notification = cacheMap.getEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD),
        relationshipCreated = cacheMap.getEntry[Boolean](appConfig.CACHE_LOCKED_CREATE),
        selectedYears = cacheMap.getEntry[List[Int]](appConfig.CACHE_SELECTED_YEARS),
        recipientDetailsFormData = cacheMap.getEntry[RecipientDetailsFormInput](appConfig.CACHE_RECIPIENT_DETAILS),
        dateOfMarriage = cacheMap.getEntry[DateOfMarriageFormInput](appConfig.CACHE_MARRIAGE_DATE)
      )))

  def getCachedDataForEligibilityCheck(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[EligibilityCheckCacheData]] =
    fetch() map (_ map (cacheMap =>
      EligibilityCheckCacheData(
        loggedInUserInfo = cacheMap.getEntry[LoggedInUserInfo](appConfig.CACHE_LOGGEDIN_USER_RECORD),
        roleRecord = cacheMap.getEntry[String](appConfig.CACHE_ROLE_RECORD),
        activeRelationshipRecord = cacheMap.getEntry[RelationshipRecord](appConfig.CACHE_ACTIVE_RELATION_RECORD),
        historicRelationships = cacheMap.getEntry[Seq[RelationshipRecord]](appConfig.CACHE_HISTORIC_RELATION_RECORD),
        notification = cacheMap.getEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD),
        relationshipEndReasonRecord =
          cacheMap.getEntry[EndRelationshipReason](appConfig.CACHE_RELATION_END_REASON_RECORD),
        relationshipUpdated = cacheMap.getEntry[Boolean](appConfig.CACHE_LOCKED_UPDATE)
      )))

  def getUpdateRelationshipCachedData(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[UpdateRelationshipCacheData]] =
    fetch() map (_ map (cacheMap =>
      UpdateRelationshipCacheData(
        relationshipRecords = cacheMap.getEntry[RelationshipRecords](appConfig.CACHE_RELATIONSHIP_RECORDS),
        email = cacheMap.getEntry[String](appConfig.CACHE_EMAIL_ADDRESS),
        endMaReason = cacheMap.getEntry[String](appConfig.CACHE_MAKE_CHANGES_DECISION),
        marriageEndDate = cacheMap
          .getEntry[MarriageAllowanceEndingDates](appConfig.CACHE_MA_ENDING_DATES)
          .map(_.marriageAllowanceEndDate)
      )))

  def getConfirmationAnswers(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[ConfirmationUpdateAnswersCacheData]] =
    fetch() map (_ map (cacheMap =>
      ConfirmationUpdateAnswersCacheData(
        relationshipRecords = cacheMap.getEntry[RelationshipRecords](appConfig.CACHE_RELATIONSHIP_RECORDS),
        divorceDate = cacheMap.getEntry[LocalDate](appConfig.CACHE_DIVORCE_DATE),
        email = cacheMap.getEntry[String](appConfig.CACHE_EMAIL_ADDRESS),
        maEndingDates = cacheMap.getEntry[MarriageAllowanceEndingDates](appConfig.CACHE_MA_ENDING_DATES)
      )))
}
