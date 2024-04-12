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

package uk.gov.hmrc.mongoFeatureToggles.internal.repository

import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json.{Format, JsError, JsString, JsSuccess}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import uk.gov.hmrc.mongoFeatureToggles.internal.model.{DeletedToggle}
import uk.gov.hmrc.mongoFeatureToggles.model.{FeatureFlag, FeatureFlagName}
import uk.gov.hmrc.play.http.logging.Mdc

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FeatureFlagRepository @Inject()(
  val mongoComponent: MongoComponent
)(implicit
  ec: ExecutionContext
) extends PlayMongoRepository[FeatureFlagSerialised](
      collectionName = "admin-feature-flags",
      mongoComponent = mongoComponent,
      domainFormat = implicitly[Format[FeatureFlagSerialised]],
      indexes = Seq(
        IndexModel(
          keys = Indexes.ascending("name"),
          indexOptions = IndexOptions()
            .name("name")
            .unique(true)
        )
      )
    )
    with Transactions
    with Logging {

  private implicit val tc: TransactionConfiguration = TransactionConfiguration.strict

  def deleteFeatureFlag(featureFlagName: FeatureFlagName): Future[Boolean] =
    collection
      .deleteOne(Filters.equal("name", featureFlagName.name))
      .map(_.wasAcknowledged())
      .toSingle()
      .toFuture()

  def getFeatureFlag(featureFlagName: FeatureFlagName): Future[Option[FeatureFlag]] =
    Mdc.preservingMdc(
      collection
        .find(Filters.equal("name", featureFlagName.name))
        .headOption()
        .map(
          _.map(flag => FeatureFlag(JsString(flag.name).as[FeatureFlagName], flag.isEnabled, flag.description))
        )
    )

  def getAllFeatureFlags: Future[List[FeatureFlag]] =
    Mdc.preservingMdc(
      collection
        .find()
        .toFuture()
        .map(
          _.toList.map { flag =>
            JsString(flag.name).validate[FeatureFlagName] match {
              case JsSuccess(value, _) => FeatureFlag(value, flag.isEnabled, flag.description)
              case JsError(_)          =>
                logger.warn(s"The feature flag `${flag.name}` does not exist anymore")
                FeatureFlag(DeletedToggle(flag.name), isEnabled = false)
            }
          }
        )
    )

  def setFeatureFlag(featureFlagName: FeatureFlagName, enabled: Boolean): Future[Boolean] =
    Mdc.preservingMdc(
      collection
        .replaceOne(
          filter = equal("name", featureFlagName.name),
          replacement = FeatureFlagSerialised(featureFlagName.name, enabled, featureFlagName.description),
          options = ReplaceOptions().upsert(true)
        )
        .map(_.wasAcknowledged())
        .toSingle()
        .toFuture()
    )

  def setFeatureFlags(flags: Map[FeatureFlagName, Boolean]): Future[Unit] = {
    val featureFlags = flags.map { case (flag, status) =>
      FeatureFlagSerialised(flag.toString, status, flag.description)
    }.toList
    Mdc.preservingMdc(
      withSessionAndTransaction(session =>
        for {
          _ <- collection.deleteMany(session, filter = in("name", flags.keys.toSeq.map(_.name): _*)).toFuture()
          _ <- collection.insertMany(session, featureFlags).toFuture()
        } yield ()
      )
    )
  }
}
