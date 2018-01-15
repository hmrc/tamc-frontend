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

package config

import org.joda.time.LocalDate
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.time.TaxYearResolver

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  private def loadConfig(key: String) = configuration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactHost = configuration.getString("tamc.external-urls.contact-frontend").getOrElse("")
  private val contactFrontendService = baseUrl("contact-frontend")
  val contactFormServiceIdentifier = "TAMC"
  val pageTitle = "Your personal tax account"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  override lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version")

  override lazy val analyticsToken: Option[String] = configuration.getString("google-analytics.token")
  override lazy val analyticsHost: String = configuration.getString("google-analytics.host").getOrElse("auto")

  override lazy val loginUrl = loadConfig("tamc.external-urls.login-url")
  override lazy val logoutUrl = loadConfig("tamc.external-urls.logout-url")
  override lazy val logoutCallbackUrl = loadConfig("tamc.external-urls.logout-callback-url")
  override lazy val callbackUrl = loadConfig("tamc.external-urls.callback-url")
  override lazy val ivNotAuthorisedUrl = loadConfig("tamc.external-urls.not-authorised-url")

  override lazy val marriageAllowanceUrl = baseUrl("marriage-allowance")

  lazy val enableRefresh = configuration.getBoolean("enableRefresh").getOrElse(true)


  val TAMC_BEGINNING_YEAR = 2015
  val TAMC_MIN_DATE = new LocalDate(1900, 1, 1)

  val CACHE_TRANSFEROR_RECORD = "TRANSFEROR_RECORD"
  val CACHE_RECIPIENT_RECORD = "RECIPIENT_RECORD"
  val CACHE_RECIPIENT_DETAILS = "RECIPIENT_DETAILS"
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
  val CACHE_MARRIAGE_DATE = "MARRIAGE_DATE"
  val CACHE_ROLE_RECORD = "ROLE"

  val PERSONAL_ALLOWANCE = configuration.getInt("personal-allowance-" + TaxYearResolver.currentTaxYear).get
  val MAX_LIMIT = configuration.getInt("max-limit-" + TaxYearResolver.currentTaxYear).get
  val MAX_LIMIT_SCOT = configuration.getInt("max-limit-scot-" + TaxYearResolver.currentTaxYear).get
  val MAX_ALLOWED_TRANSFER = PERSONAL_ALLOWANCE / 10
  val MAX_BENEFIT = MAX_ALLOWED_TRANSFER / 5
  val TRANSFEROR_ALLOWANCE = PERSONAL_ALLOWANCE - MAX_ALLOWED_TRANSFER
  val RECIPIENT_ALLOWANCE = PERSONAL_ALLOWANCE + MAX_ALLOWED_TRANSFER
  val TAMC_VALID_JOURNEY = "TAMC_VALID_JOURNEY"

  override val gdsFinishedUrl = loadConfig("tamc.external-urls.finished-gds")
  override val ptaFinishedUrl = loadConfig("tamc.external-urls.finished-pta")

  override val LANG_CODE_ENGLISH = "en-GB"
  override val LANG_CODE_WELSH = "cy-GB"
  override val LANG_LANG_ENGLISH = "en"
  override val LANG_LANG_WELSH = "cy"

  override val isWelshEnabled = configuration.getBoolean("welsh-translation").getOrElse(false)
  lazy val webchatId = loadConfig("webchat.id")
}

trait ApplicationConfig {

  val betaFeedbackUnauthenticatedUrl: String
  val contactFrontendPartialBaseUrl: String
  val assetsPrefix: String

  val contactFormServiceIdentifier: String
  val pageTitle: String
  val loginUrl: String
  val logoutUrl: String
  val logoutCallbackUrl: String
  val callbackUrl: String

  val analyticsToken: Option[String]
  val analyticsHost: String

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

  val LANG_CODE_ENGLISH: String
  val LANG_CODE_WELSH: String
  val LANG_LANG_WELSH: String
  val LANG_LANG_ENGLISH: String

  val isWelshEnabled: Boolean
  val webchatId: String
  /* refreshInterval sets the time in seconds for the session timeout.It is 15 minutes now.*/
  lazy val refreshInterval = 900
}
