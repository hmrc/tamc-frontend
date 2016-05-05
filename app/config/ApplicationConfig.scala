/*
 * Copyright 2016 HM Revenue & Customs
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

package config

import play.api.Play.configuration
import play.api.Play.current
import uk.gov.hmrc.play.config.ServicesConfig
import org.joda.time.LocalDate

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactFrontend = loadConfig("tamc.external-urls.contact-frontend")
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactFrontend/beta-feedback-unauthenticated?service=TAMC"
  override lazy val reportAProblemPartialUrl = s"$contactFrontend/problem_reports_ajax?service=ma"
  override lazy val reportAProblemNonJSUrl = s"$contactFrontend/problem_reports_nonjs?service=ma"

  override lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version")

  override lazy val analyticsToken: String = loadConfig("google-analytics.token")
  override lazy val analyticsHost: String = loadConfig("google-analytics.host")

  override lazy val loginUrl = loadConfig("tamc.external-urls.login-url")
  override lazy val logoutUrl = loadConfig("tamc.external-urls.logout-url")
  override lazy val callbackUrl = loadConfig("tamc.external-urls.callback-url")
  override lazy val ivNotAuthorisedUrl = loadConfig("tamc.external-urls.not-authorised-url")

  override lazy val marriageAllowanceUrl = baseUrl("marriage-allowance")

  val TAMC_BEGINNING_YEAR = configuration.getInt("ma-start-tax-year").getOrElse(2015)
  val TAMC_MIN_DATE = new LocalDate(1900, 1, 1)

  val CACHE_TRANSFEROR_RECORD = "TRANSFEROR_RECORD"
  val CACHE_RECIPIENT_RECORD = "RECIPIENT_RECORD"
  val CACHE_NOTIFICATION_RECORD = "NOTIFICATION_RECORD"
  val CACHE_LOCKED_CREATE = "LOCKED_CREATE"
  val CACHE_LOGGEDIN_USER_RECORD = "LOGGEDIN_USER_RECORD"
  val CACHE_ACTIVE_RELATION_RECORD = "ACTIVE_RELATION_RECORD"
  val CACHE_HISTORIC_RELATION_RECORD = "HISTORIC_RELATION_RECORD"
  val CACHE_RELATION_END_REASON_RECORD = "RELATION_END_REASON_RECORD"
  val CACHE_PERSON_DETAILS = "PERSON_DETAILS"
  val CACHE_SELECTED_YEARS = "SELECTED_YEARS"
  val CACHE_SOURCE = "SOURCE"
  val CACHE_LOCKED_UPDATE = "LOCKED_UPDATE"

  val PERSONAL_ALLOWANCE = 11000
  val MAX_ALLOWED_TRANSFER = 1100
  val MAX_BENEFIT = MAX_ALLOWED_TRANSFER / 5
  val MAX_LIMIT = 43000
  val TRANSFEROR_ALLOWANCE = PERSONAL_ALLOWANCE - MAX_ALLOWED_TRANSFER
  val RECIPIENT_ALLOWANCE = PERSONAL_ALLOWANCE + MAX_ALLOWED_TRANSFER
  val TAMC_VALID_JOURNEY = "TAMC_VALID_JOURNEY"

  override val gdsFinishedUrl = loadConfig("tamc.external-urls.finished-gds")
  override val ptaFinishedUrl = loadConfig("tamc.external-urls.finished-pta")
}

trait ApplicationConfig {

  val betaFeedbackUnauthenticatedUrl: String
  val reportAProblemPartialUrl: String
  val reportAProblemNonJSUrl: String

  val assetsPrefix: String

  val analyticsToken: String
  val analyticsHost: String

  val loginUrl: String
  val logoutUrl: String
  val callbackUrl: String

  def ivNotAuthorisedUrl: String
  private def createUrl(action: String) = s"${loginUrl}/${action}?origin=ma&confidenceLevel=100&completionURL=${utils.encodeQueryStringValue(callbackUrl)}&failureURL=${utils.encodeQueryStringValue(ivNotAuthorisedUrl)}"
  def ivLoginUrl = createUrl(action = "registration")
  def ivUpliftUrl = createUrl(action = "uplift")

  val marriageAllowanceUrl: String

  val TAMC_JOURNEY = "TAMC_JOURNEY"
  val TAMC_JOURNEY_PTA = "PTA"
  val TAMC_JOURNEY_GDS = "GDS"

  val gdsFinishedUrl: String
  val ptaFinishedUrl: String
}
