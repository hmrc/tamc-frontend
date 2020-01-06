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

package utils

import akka.actor.ActorSystem
import com.typesafe.config.Config
import connectors.ApplicationAuditConnector
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.http.{HttpDelete, HttpGet, HttpPost, HttpPut}
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, RunMode}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

object WSHttp extends WSGet with HttpGet
  with WSPut with HttpPut
  with WSPost with HttpPost
  with WSDelete with HttpDelete
  with AppName with RunMode
  with HttpAuditing {
  override val hooks = Seq(AuditingHook)
  val auditConnector: AuditConnector = ApplicationAuditConnector

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  override protected def appNameConfiguration: Configuration = runModeConfiguration

  override protected def mode: Mode = Play.current.mode

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some(runModeConfiguration.underlying)
}
