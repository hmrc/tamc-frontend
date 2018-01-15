/*
 * Copyright 2018 HM Revenue & Customs
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

import scala.annotation.implicitNotFound
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import models._
import uk.gov.hmrc.http.cache.client.SessionCache
import config.ApplicationConfig
import uk.gov.hmrc.emailaddress.PlayJsonFormats.emailAddressReads
import uk.gov.hmrc.emailaddress.PlayJsonFormats.emailAddressWrites
import utils.WSHttp
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.config.AppName
import details.PersonDetails
import uk.gov.hmrc.http.HeaderCarrier

object CachingService extends CachingService {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
}

trait CachingService extends SessionCache with AppName with ServicesConfig {

  def saveTransferorRecord(transferorRecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cache[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD, transferorRecord) map
      (_.getEntry[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD).get)

  def saveLoggedInUserInfo(loggedInUserInfo: LoggedInUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LoggedInUserInfo] =
    cache[LoggedInUserInfo](ApplicationConfig.CACHE_LOGGEDIN_USER_RECORD, loggedInUserInfo) map
      (_.getEntry[LoggedInUserInfo](ApplicationConfig.CACHE_LOGGEDIN_USER_RECORD).get)

  def saveRecipientRecord(recipientRecord: UserRecord, recipientData: RegistrationFormInput, availableYears: List[TaxYear])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cache[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD, RecipientRecord(record = recipientRecord, data = recipientData, availableTaxYears = availableYears)) map
      (_.getEntry[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD).get.record)

  def saveRecipientDetails(details: RecipientDetailsFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[RecipientDetailsFormInput] =
    cache[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS, details) map
      (_.getEntry[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS).get)

  def saveDateOfMarriage(details: DateOfMarriageFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext) : Future[DateOfMarriageFormInput] =
    cache[DateOfMarriageFormInput](ApplicationConfig.CACHE_MARRIAGE_DATE, details) map
      (_.getEntry[DateOfMarriageFormInput](ApplicationConfig.CACHE_MARRIAGE_DATE).get)


  def saveNotificationRecord(notificationRecord: NotificationRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[NotificationRecord] =
    cache[NotificationRecord](ApplicationConfig.CACHE_NOTIFICATION_RECORD, notificationRecord) map
      (_.getEntry[NotificationRecord](ApplicationConfig.CACHE_NOTIFICATION_RECORD).get)

  def saveSource(source: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cache[String](ApplicationConfig.CACHE_SOURCE, source) map
      (_.getEntry[String](ApplicationConfig.CACHE_SOURCE).get)

  def saveActiveRelationshipRecord(activeRelationshipRecord: RelationshipRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RelationshipRecord] =
    cache[RelationshipRecord](ApplicationConfig.CACHE_ACTIVE_RELATION_RECORD, activeRelationshipRecord) map
      (_.getEntry[RelationshipRecord](ApplicationConfig.CACHE_ACTIVE_RELATION_RECORD).get)
      
  def saveHistoricRelationships(historic: Option[Seq[RelationshipRecord]])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Seq[RelationshipRecord]]] =
    cache[Seq[RelationshipRecord]](ApplicationConfig.CACHE_HISTORIC_RELATION_RECORD, historic.getOrElse(Seq[RelationshipRecord]())) map
      (_.getEntry[Seq[RelationshipRecord]](ApplicationConfig.CACHE_HISTORIC_RELATION_RECORD))

  def savRelationshipEndReasonRecord(relationshipEndReason: EndRelationshipReason)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EndRelationshipReason] =
    cache[EndRelationshipReason](ApplicationConfig.CACHE_RELATION_END_REASON_RECORD, relationshipEndReason) map
      (_.getEntry[EndRelationshipReason](ApplicationConfig.CACHE_RELATION_END_REASON_RECORD).get)

  def saveRoleRecord(roleRecord: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    cache[String](ApplicationConfig.CACHE_ROLE_RECORD, roleRecord) map
      (_.getEntry[String](ApplicationConfig.CACHE_ROLE_RECORD).get)

  def lockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cache[Boolean](ApplicationConfig.CACHE_LOCKED_CREATE, true) map
      (_.getEntry[Boolean](ApplicationConfig.CACHE_LOCKED_CREATE).get)

  def lockUpdateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cache[Boolean](ApplicationConfig.CACHE_LOCKED_UPDATE, true) map
      (_.getEntry[Boolean](ApplicationConfig.CACHE_LOCKED_UPDATE).get)

  def unlockCreateRelationship()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] =
    cache[Boolean](ApplicationConfig.CACHE_LOCKED_CREATE, false) map
      (_.getEntry[Boolean](ApplicationConfig.CACHE_LOCKED_CREATE).get)

  def getPersonDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PersonDetails]] =
    fetchAndGetEntry[PersonDetails](ApplicationConfig.CACHE_PERSON_DETAILS)

  def getRecipientDetails(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RecipientDetailsFormInput]] =
    fetchAndGetEntry[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS)

  def savePersonDetails(personDetails: PersonDetails)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PersonDetails] =
    cache[PersonDetails](ApplicationConfig.CACHE_PERSON_DETAILS, personDetails) map
      (_.getEntry[PersonDetails](ApplicationConfig.CACHE_PERSON_DETAILS).get)

  def saveSelectedYears(selectedYears: List[Int])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Int]] =
    cache[List[Int]](ApplicationConfig.CACHE_SELECTED_YEARS, selectedYears) map
      (_.getEntry[List[Int]](ApplicationConfig.CACHE_SELECTED_YEARS).get)

  def getCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheData]] =
    fetch() map (
      _ map (
        cacheMap =>
          CacheData(
            transferor = cacheMap.getEntry[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD),
            recipient = cacheMap.getEntry[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD),
            notification = cacheMap.getEntry[NotificationRecord](ApplicationConfig.CACHE_NOTIFICATION_RECORD),
            relationshipCreated = cacheMap.getEntry[Boolean](ApplicationConfig.CACHE_LOCKED_CREATE),
            selectedYears = cacheMap.getEntry[List[Int]](ApplicationConfig.CACHE_SELECTED_YEARS),
            recipientDetailsFormData = cacheMap.getEntry[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS),
            dateOfMarriage= cacheMap.getEntry[DateOfMarriageFormInput](ApplicationConfig.CACHE_MARRIAGE_DATE))))


  def getUpdateRelationshipCachedData(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[UpdateRelationshipCacheData]] =
    fetch() map (
      _ map (
        cacheMap =>
          UpdateRelationshipCacheData(
            loggedInUserInfo = cacheMap.getEntry[LoggedInUserInfo](ApplicationConfig.CACHE_LOGGEDIN_USER_RECORD),
            roleRecord = cacheMap.getEntry[String](ApplicationConfig.CACHE_ROLE_RECORD),
            activeRelationshipRecord = cacheMap.getEntry[RelationshipRecord](ApplicationConfig.CACHE_ACTIVE_RELATION_RECORD),
            historicRelationships = cacheMap.getEntry[Seq[RelationshipRecord]](ApplicationConfig.CACHE_HISTORIC_RELATION_RECORD),
            notification = cacheMap.getEntry[NotificationRecord](ApplicationConfig.CACHE_NOTIFICATION_RECORD),
            relationshipEndReasonRecord = cacheMap.getEntry[EndRelationshipReason](ApplicationConfig.CACHE_RELATION_END_REASON_RECORD),
            relationshipUpdated = cacheMap.getEntry[Boolean](ApplicationConfig.CACHE_LOCKED_UPDATE))))

}
