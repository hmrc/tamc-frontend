/*
 * Copyright 2021 HM Revenue & Customs
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
import config.ApplicationConfig
import connectors.MarriageAllowanceConnector
import errors.{CacheMapNoFound, TransferorNotFound}
import models._
import java.time.LocalDate
import play.api.Mode.Mode
import play.api.libs.json.{Reads, Writes}
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

import scala.concurrent.{ExecutionContext, Future}

class CachingService @Inject()(
                                marriageAllowanceConnector: MarriageAllowanceConnector,
                                val http: HttpClient,
                                val runModeConfiguration: Configuration,
                                environment: Environment,
                                appConfig: ApplicationConfig
                              ) extends SessionCache with AppName with ServicesConfig {
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))

  override protected def mode: Mode = environment.mode

  override protected def appNameConfiguration: Configuration = runModeConfiguration

  def cacheValue[T](key: String, value: T)(implicit wts: Writes[T], reads: Reads[T], hc: HeaderCarrier, executionContext: ExecutionContext): Future[T] = {
    cache[T](key, value) map (_.getEntry[T](key).getOrElse(throw new RuntimeException(s"Failed to retrieve $key from cache after saving")))
  }

  def saveTransferorRecord(transferorRecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cache[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD, transferorRecord) map
      (_.getEntry[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD).get)

  def saveLoggedInUserInfo(loggedInUserInfo: LoggedInUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LoggedInUserInfo] =
    cache[LoggedInUserInfo](appConfig.CACHE_LOGGEDIN_USER_RECORD, loggedInUserInfo) map
      (_.getEntry[LoggedInUserInfo](appConfig.CACHE_LOGGEDIN_USER_RECORD).get)

  def saveRecipientRecord(recipientRecord: UserRecord, recipientData: RegistrationFormInput, availableYears: List[TaxYear])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cache[RecipientRecord](appConfig.CACHE_RECIPIENT_RECORD, RecipientRecord(record = recipientRecord, data = recipientData, availableTaxYears = availableYears)) map
      (_.getEntry[RecipientRecord](appConfig.CACHE_RECIPIENT_RECORD).get.record)

  def saveRecipientDetails(details: RecipientDetailsFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RecipientDetailsFormInput] =
    cache[RecipientDetailsFormInput](appConfig.CACHE_RECIPIENT_DETAILS, details) map
      (_.getEntry[RecipientDetailsFormInput](appConfig.CACHE_RECIPIENT_DETAILS).get)

  def saveDateOfMarriage(details: DateOfMarriageFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DateOfMarriageFormInput] = {
    cache[DateOfMarriageFormInput](appConfig.CACHE_MARRIAGE_DATE, details) map
      (_.getEntry[DateOfMarriageFormInput](appConfig.CACHE_MARRIAGE_DATE).get)
  }

  def saveNotificationRecord(notificationRecord: NotificationRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    cache[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD, notificationRecord) map
      (_.getEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD).get)

  def saveSource(source: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cache[String](appConfig.CACHE_SOURCE, source) map
      (_.getEntry[String](appConfig.CACHE_SOURCE).get)

  def saveActiveRelationshipRecord(activeRelationshipRecord: RelationshipRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecord] =
    cache[RelationshipRecord](appConfig.CACHE_ACTIVE_RELATION_RECORD, activeRelationshipRecord) map
      (_.getEntry[RelationshipRecord](appConfig.CACHE_ACTIVE_RELATION_RECORD).get)

  def saveHistoricRelationships(historic: Option[Seq[RelationshipRecord]])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[RelationshipRecord]]] =
    cache[Seq[RelationshipRecord]](appConfig.CACHE_HISTORIC_RELATION_RECORD, historic.getOrElse(Seq[RelationshipRecord]())) map
      (_.getEntry[Seq[RelationshipRecord]](appConfig.CACHE_HISTORIC_RELATION_RECORD))

  def savRelationshipEndReasonRecord(relationshipEndReason: EndRelationshipReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndRelationshipReason] =
    cache[EndRelationshipReason](appConfig.CACHE_RELATION_END_REASON_RECORD, relationshipEndReason) map
      (_.getEntry[EndRelationshipReason](appConfig.CACHE_RELATION_END_REASON_RECORD).get)

  def saveRoleRecord(roleRecord: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cache[String](appConfig.CACHE_ROLE_RECORD, roleRecord) map
      (_.getEntry[String](appConfig.CACHE_ROLE_RECORD).get)

  def lockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cache[Boolean](appConfig.CACHE_LOCKED_CREATE, true) map
      (_.getEntry[Boolean](appConfig.CACHE_LOCKED_CREATE).get)

  def lockUpdateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cache[Boolean](appConfig.CACHE_LOCKED_UPDATE, true) map
      (_.getEntry[Boolean](appConfig.CACHE_LOCKED_UPDATE).get)

  def unlockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cache[Boolean](appConfig.CACHE_LOCKED_CREATE, false) map
      (_.getEntry[Boolean](appConfig.CACHE_LOCKED_CREATE).get)

  def getRecipientDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RecipientDetailsFormInput]] =
    fetchAndGetEntry[RecipientDetailsFormInput](appConfig.CACHE_RECIPIENT_DETAILS)

  def saveSelectedYears(selectedYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    cache[List[Int]](appConfig.CACHE_SELECTED_YEARS, selectedYears) map
      (_.getEntry[List[Int]](appConfig.CACHE_SELECTED_YEARS).get)

  def getCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheData]] =
    fetch() map (
      _ map (
        cacheMap =>
          CacheData(
            transferor = cacheMap.getEntry[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD),
            recipient = cacheMap.getEntry[RecipientRecord](appConfig.CACHE_RECIPIENT_RECORD),
            notification = cacheMap.getEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD),
            relationshipCreated = cacheMap.getEntry[Boolean](appConfig.CACHE_LOCKED_CREATE),
            selectedYears = cacheMap.getEntry[List[Int]](appConfig.CACHE_SELECTED_YEARS),
            recipientDetailsFormData = cacheMap.getEntry[RecipientDetailsFormInput](appConfig.CACHE_RECIPIENT_DETAILS),
            dateOfMarriage = cacheMap.getEntry[DateOfMarriageFormInput](appConfig.CACHE_MARRIAGE_DATE))))

  def getRecipientRecord(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RecipientRecord]] =
    fetchAndGetEntry[RecipientRecord](appConfig.CACHE_RECIPIENT_RECORD)


  def getCachedData(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheData]] =
    getCacheData(nino).map {
      _.map {
        cacheMap =>
          CacheData(
            transferor = cacheMap.getEntry[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD),
            recipient = cacheMap.getEntry[RecipientRecord](appConfig.CACHE_RECIPIENT_RECORD),
            notification = cacheMap.getEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD),
            relationshipCreated = cacheMap.getEntry[Boolean](appConfig.CACHE_LOCKED_CREATE),
            selectedYears = cacheMap.getEntry[List[Int]](appConfig.CACHE_SELECTED_YEARS),
            recipientDetailsFormData = cacheMap.getEntry[RecipientDetailsFormInput](appConfig.CACHE_RECIPIENT_DETAILS),
            dateOfMarriage = cacheMap.getEntry[DateOfMarriageFormInput](appConfig.CACHE_MARRIAGE_DATE))
      }
    }

  def getCachedDataForEligibilityCheck(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[EligibilityCheckCacheData]] =
    fetch() map (
      _ map (
        cacheMap =>
          EligibilityCheckCacheData(
            loggedInUserInfo = cacheMap.getEntry[LoggedInUserInfo](appConfig.CACHE_LOGGEDIN_USER_RECORD),
            roleRecord = cacheMap.getEntry[String](appConfig.CACHE_ROLE_RECORD),
            activeRelationshipRecord = cacheMap.getEntry[RelationshipRecord](appConfig.CACHE_ACTIVE_RELATION_RECORD),
            historicRelationships = cacheMap.getEntry[Seq[RelationshipRecord]](appConfig.CACHE_HISTORIC_RELATION_RECORD),
            notification = cacheMap.getEntry[NotificationRecord](appConfig.CACHE_NOTIFICATION_RECORD),
            relationshipEndReasonRecord = cacheMap.getEntry[EndRelationshipReason](appConfig.CACHE_RELATION_END_REASON_RECORD),
            relationshipUpdated = cacheMap.getEntry[Boolean](appConfig.CACHE_LOCKED_UPDATE))))


  def getUpdateRelationshipCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UpdateRelationshipCacheData] = {
    fetch() map { optionalCacheMap =>
      optionalCacheMap.fold(throw CacheMapNoFound()) { cacheMap =>
        val emailAddress = cacheMap.getEntry[String](appConfig.CACHE_EMAIL_ADDRESS)
        val marriageAllowanceEndingDate = cacheMap.getEntry[MarriageAllowanceEndingDates](appConfig.CACHE_MA_ENDING_DATES).map(_.marriageAllowanceEndDate)
        val endReason = cacheMap.getEntry[String](appConfig.CACHE_MAKE_CHANGES_DECISION)
        val relationshipRecord = cacheMap.getEntry[RelationshipRecords](appConfig.CACHE_RELATIONSHIP_RECORDS)

        UpdateRelationshipCacheData(relationshipRecord, emailAddress, endReason, marriageAllowanceEndingDate)
      }
    }
  }

  def getConfirmationAnswers(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ConfirmationUpdateAnswersCacheData] = {
    fetch() map {
      optionalCacheMap =>
        optionalCacheMap.fold(throw CacheMapNoFound()) {
          cacheMap =>
            val emailAddress = cacheMap.getEntry[String](appConfig.CACHE_EMAIL_ADDRESS)
            val marriageAllowanceEndingDate = cacheMap.getEntry[MarriageAllowanceEndingDates](appConfig.CACHE_MA_ENDING_DATES)
            val divorceDate = cacheMap.getEntry[LocalDate](appConfig.CACHE_DIVORCE_DATE)
            val relationshipRecords = cacheMap.getEntry[RelationshipRecords](appConfig.CACHE_RELATIONSHIP_RECORDS)

            ConfirmationUpdateAnswersCacheData(relationshipRecords, divorceDate, emailAddress, marriageAllowanceEndingDate)
        }
    }
  }

  def getRelationshipRecords(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RelationshipRecords]] = {
    fetchAndGetEntry[RelationshipRecords](appConfig.CACHE_RELATIONSHIP_RECORDS)
  }

  private def getCacheData(nino: Nino)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Option[CacheMap]] = {
    fetch().flatMap {
      _.map {
        cacheMap =>
          cacheMap.getEntry[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD)
            .map(_ => Future.successful(Some(cacheMap)))
            .getOrElse {
              marriageAllowanceConnector.listRelationship(nino) flatMap {
                data =>
                  data.userRecord.map {
                    loggedInUser =>
                      val userRecord: UserRecord = UserRecord(
                        cid = loggedInUser.cid,
                        timestamp = loggedInUser.timestamp,
                        has_allowance = None,
                        name = loggedInUser.name)

                      cache[UserRecord](appConfig.CACHE_TRANSFEROR_RECORD, userRecord)
                        .map(Some.apply)
                  }.getOrElse {
                    throw TransferorNotFound()
                  }
              }
            }
      }.getOrElse(Future.successful(None))
    }
  }
}
