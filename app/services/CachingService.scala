/*
 * Copyright 2020 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.MarriageAllowanceConnector
import details.PersonDetails
import errors.TransferorNotFound
import models._
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.{CacheMap, SessionCache}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import utils.WSHttp

import scala.concurrent.{ExecutionContext, Future}

object CachingService extends CachingService {
  override lazy val http = WSHttp
  override lazy val defaultSource = appName
  override lazy val baseUri = baseUrl("cachable.session-cache")
  override lazy val domain = getConfString("cachable.session-cache.domain", throw new Exception(s"Could not find config 'cachable.session-cache.domain'"))
  override def marriageAllowanceConnector: MarriageAllowanceConnector = MarriageAllowanceConnector

}

trait CachingService extends SessionCache with AppName with ServicesConfig {

  override protected def mode: Mode = Play.current.mode
  override protected def runModeConfiguration: Configuration = Play.current.configuration
  override protected def appNameConfiguration: Configuration = runModeConfiguration

  def marriageAllowanceConnector: MarriageAllowanceConnector

  def saveTransferorRecord(transferorRecord: UserRecord)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cache[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD, transferorRecord) map
      (_.getEntry[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD).get)

  def saveLoggedInUserInfo(loggedInUserInfo: LoggedInUserInfo)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[LoggedInUserInfo] =
    cache[LoggedInUserInfo](ApplicationConfig.CACHE_LOGGEDIN_USER_RECORD, loggedInUserInfo) map
      (_.getEntry[LoggedInUserInfo](ApplicationConfig.CACHE_LOGGEDIN_USER_RECORD).get)

  def saveRecipientRecord(recipientRecord: UserRecord, recipientData: RegistrationFormInput, availableYears: List[TaxYear])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[UserRecord] =
    cache[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD, RecipientRecord(record = recipientRecord, data = recipientData, availableTaxYears = availableYears)) map
      (_.getEntry[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD).get.record)

  def saveRecipientDetails(details: RecipientDetailsFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[RecipientDetailsFormInput] =
    cache[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS, details) map
      (_.getEntry[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS).get)

  def saveDateOfMarriage(details: DateOfMarriageFormInput)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[DateOfMarriageFormInput] =
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
            dateOfMarriage = cacheMap.getEntry[DateOfMarriageFormInput](ApplicationConfig.CACHE_MARRIAGE_DATE))))

  def getRecipientRecord(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[RecipientRecord]] =
    fetchAndGetEntry[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD)

  def getCachedData(nino: Nino)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[CacheData]] =
    getCacheData(nino).map {
      _.map {
        cacheMap =>
          CacheData(
            transferor = cacheMap.getEntry[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD),
            recipient = cacheMap.getEntry[RecipientRecord](ApplicationConfig.CACHE_RECIPIENT_RECORD),
            notification = cacheMap.getEntry[NotificationRecord](ApplicationConfig.CACHE_NOTIFICATION_RECORD),
            relationshipCreated = cacheMap.getEntry[Boolean](ApplicationConfig.CACHE_LOCKED_CREATE),
            selectedYears = cacheMap.getEntry[List[Int]](ApplicationConfig.CACHE_SELECTED_YEARS),
            recipientDetailsFormData = cacheMap.getEntry[RecipientDetailsFormInput](ApplicationConfig.CACHE_RECIPIENT_DETAILS),
            dateOfMarriage = cacheMap.getEntry[DateOfMarriageFormInput](ApplicationConfig.CACHE_MARRIAGE_DATE))
      }
    }


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

  private def getCacheData(nino: Nino)(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[Option[CacheMap]] = {
    fetch().flatMap {
      _.map {
        cacheMap =>
          cacheMap.getEntry[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD)
            .map(_ => Future.successful(Some(cacheMap)))
            .getOrElse {
              marriageAllowanceConnector.listRelationship(nino) flatMap {
                data => data.userRecord.map {
                  loggedInUser =>
                    val userRecord: UserRecord = UserRecord(
                      cid = loggedInUser.cid,
                      timestamp = loggedInUser.timestamp,
                      has_allowance = None,
                      name = loggedInUser.name)

                    cache[UserRecord](ApplicationConfig.CACHE_TRANSFEROR_RECORD, userRecord)
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
