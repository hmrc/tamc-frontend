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

import com.google.inject.name.Named
import com.google.inject.{ImplementedBy, Inject}
import config.ApplicationConfig
import models._
import play.api.libs.json.Format
import play.api.mvc.Request
import repositories.SessionCacheNew
import services.CacheService._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.emailaddress.PlayJsonFormats.{emailAddressReads, emailAddressWrites}
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CachingServiceImpl])
trait CachingServiceOld {

  def get[T](cacheKey: CacheKey[T])(implicit request: Request[_], format: Format[T], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[T]]
  def put[T](cacheKey: CacheKey[T], value: T)(implicit request: Request[_], format: Format[T], hc: HeaderCarrier, executionContext: ExecutionContext): Future[T]
  def clear()(implicit request: Request[_], hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit]

  def getUserAnswersCachedData(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UserAnswersCacheData]]
  def getCachedDataForEligibilityCheck(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EligibilityCheckCacheData]]
  def getUpdateRelationshipCachedData(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]]
  def getConfirmationAnswers(implicit request: Request[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[ConfirmationUpdateAnswersCacheData]]
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

  val EXTRACT_USER_ANSWERS: CacheMap => Option[UserAnswersCacheData] =
    cacheMap => Some(cacheMap).map (cacheMap =>
      UserAnswersCacheData(
        transferor = cacheMap.getEntry[UserRecord](CACHE_TRANSFEROR_RECORD.dataKey),
        recipient = cacheMap.getEntry[RecipientRecord](CACHE_RECIPIENT_RECORD.dataKey),
        notification = cacheMap.getEntry[NotificationRecord](CACHE_NOTIFICATION_RECORD.dataKey),
        relationshipCreated = cacheMap.getEntry[Boolean](CACHE_LOCKED_CREATE.dataKey),
        selectedYears = cacheMap.getEntry[List[Int]](CACHE_SELECTED_YEARS.dataKey),
        recipientDetailsFormData = cacheMap.getEntry[RecipientDetailsFormInput](CACHE_RECIPIENT_DETAILS.dataKey),
        dateOfMarriage = cacheMap.getEntry[DateOfMarriageFormInput](CACHE_MARRIAGE_DATE.dataKey)
      ))

  val EXTRACT_ELIGIBILITY_CHECK: CacheMap => Option[EligibilityCheckCacheData] =
    cacheMap => Some(cacheMap).map (cacheMap =>
      EligibilityCheckCacheData(
        loggedInUserInfo = cacheMap.getEntry[LoggedInUserInfo](CACHE_LOGGEDIN_USER_RECORD.dataKey),
        roleRecord = cacheMap.getEntry[String](CACHE_ROLE_RECORD.dataKey),
        activeRelationshipRecord = cacheMap.getEntry[RelationshipRecord](CACHE_ACTIVE_RELATION_RECORD.dataKey),
        historicRelationships = cacheMap.getEntry[Seq[RelationshipRecord]](CACHE_HISTORIC_RELATION_RECORD.dataKey),
        notification = cacheMap.getEntry[NotificationRecord](CACHE_NOTIFICATION_RECORD.dataKey),
        relationshipEndReasonRecord = cacheMap.getEntry[EndRelationshipReason](CACHE_RELATION_END_REASON_RECORD.dataKey),
        relationshipUpdated = cacheMap.getEntry[Boolean](CACHE_LOCKED_UPDATE.dataKey)
      ))

  val EXTRACT_UPDATE_RELATIONSHIP: CacheMap => Option[UpdateRelationshipCacheData] =
    cacheMap => Some(cacheMap).map (cacheMap =>
      UpdateRelationshipCacheData(
        relationshipRecords = cacheMap.getEntry[RelationshipRecords](CACHE_RELATIONSHIP_RECORDS.dataKey),
        email = cacheMap.getEntry[EmailAddress](CACHE_EMAIL_ADDRESS.dataKey).map(_.value),
        endMaReason = cacheMap.getEntry[String](CACHE_MAKE_CHANGES_DECISION.dataKey),
        marriageEndDate = cacheMap.getEntry[MarriageAllowanceEndingDates](CACHE_MA_ENDING_DATES.dataKey).map(_.marriageAllowanceEndDate)
      ))

  val EXTRACT_CONFIRMATION: CacheMap => Option[ConfirmationUpdateAnswersCacheData] =
    cacheMap => Some(cacheMap).map (cacheMap =>
      ConfirmationUpdateAnswersCacheData(
        relationshipRecords = cacheMap.getEntry[RelationshipRecords](CACHE_RELATIONSHIP_RECORDS.dataKey),
        divorceDate = cacheMap.getEntry[LocalDate](CACHE_DIVORCE_DATE.dataKey),
        email = cacheMap.getEntry[String](CACHE_EMAIL_ADDRESS.dataKey),
        maEndingDates = cacheMap.getEntry[MarriageAllowanceEndingDates](CACHE_MA_ENDING_DATES.dataKey)
      ))


  sealed abstract class CacheKey[T]{val dataKey: String; val extractor: CacheMap => Option[T]}
  object CacheKey {
    def apply[T](key: String)(implicit format: Format[T]): CacheKey[T] = new CacheKey[T] {
      override val dataKey = key
      override val extractor: CacheMap => Option[T] =
        _.getEntry[T](dataKey)
    }

    def apply[T](extra: CacheMap => Option[T])(implicit format: Format[T]): CacheKey[T] = new CacheKey[T]{
      override  val dataKey = ???
      override val extractor: CacheMap => Option[T] =
        _.getEntry[T](dataKey)
    }
  }
}

class CachingServiceImpl @Inject() (
  val http: HttpClient,
  appConfig: ApplicationConfig,
  sessionCacheNew: SessionCacheNew,
  @Named("appName") appName: String
) extends SessionCache with CachingServiceOld {

  override lazy val defaultSource: String = appName
  override lazy val baseUri: String       = appConfig.cacheUri
  override lazy val domain: String        = appConfig.sessionCacheDomain

  def get[T](cacheKey: CacheKey[T])(implicit request: Request[_], format: Format[T], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[T]] =
    fetch() map (_ flatMap (cacheKey.extractor))

  def put[T](cacheKey: CacheKey[T], value: T)(implicit request: Request[_], format: Format[T], hc: HeaderCarrier, executionContext: ExecutionContext): Future[T] =
    cache[T](cacheKey.dataKey, value)
      .map(_.getEntry[T](cacheKey.dataKey).getOrElse(throw new RuntimeException(s"Failed to retrieve ${cacheKey.dataKey} from cache after saving")))
      .andThen(_ => sessionCacheNew.put[T](cacheKey.dataKey, value))

  def clear()(implicit request: Request[_], hc: HeaderCarrier, executionContext: ExecutionContext): Future[Unit] =
    remove()
      .map(_ => ())
      .andThen(_ => sessionCacheNew.clear())

  def getUserAnswersCachedData(implicit
    request: Request[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[UserAnswersCacheData]] =
    fetch() map (_ flatMap  (EXTRACT_USER_ANSWERS))

  def getCachedDataForEligibilityCheck(implicit
    request: Request[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[EligibilityCheckCacheData]] =
    fetch() map (_ flatMap (EXTRACT_ELIGIBILITY_CHECK))

  def getUpdateRelationshipCachedData(implicit
    request: Request[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[UpdateRelationshipCacheData]] =
    fetch() map (_ flatMap (EXTRACT_UPDATE_RELATIONSHIP))

  def getConfirmationAnswers(implicit
    request: Request[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[ConfirmationUpdateAnswersCacheData]] =
    fetch() map (_ flatMap (EXTRACT_CONFIRMATION))
}
