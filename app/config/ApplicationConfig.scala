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

package config

import config.ApplicationConfig.loadConfig
import org.joda.time.LocalDate
import play.api.Mode.Mode
import play.api.{Configuration, Play}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.time.TaxYear
import utils.encodeQueryStringValue
import uk.gov.hmrc.play.frontend.binders.SafeRedirectUrl

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
  val callChargeUrl: String
  val contactIncomeTaxHelplineUrl: String
  val marriageAllowanceGuideUrl: String
  val howItWorksUrl: String

  val analyticsToken: Option[String]
  val analyticsHost: String
  val isGTMEnabled: Boolean
  val gtmId: String
  val frontendTemplatePath: String

  val marriageAllowanceUrl: String
  val taiFrontendUrl: String
  val taxFreeAllowanceUrl: String

  def ivNotAuthorisedUrl: String

  private def createUrl(action: String) =
    s"$loginUrl/$action?origin=ma&confidenceLevel=200&completionURL=${encodeQueryStringValue(callbackUrl)}&failureURL=${encodeQueryStringValue(ivNotAuthorisedUrl)}"

  def ivLoginUrl = createUrl(action = "registration")

  def ivUpliftUrl = createUrl(action = "uplift")

  val ggSignInHost: String
  val ggSignInUrl: String

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
  lazy val applyMarriageAllowanceUrl: String = loadConfig("tamc.external-urls.apply-marriage-allowance")
  def accessibilityStatementUrl(relativeReferrerPath: String): String
}

object ApplicationConfig extends ApplicationConfig with ServicesConfig {

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration

  private lazy val currentTaxYear: TaxYear = TaxYear.current

  private def loadConfig(key: String) = runModeConfiguration.getString(key).getOrElse(throw new Exception(s"Missing key: $key"))

  private val contactHost = runModeConfiguration.getString("tamc.external-urls.contact-frontend").getOrElse("")
  private val contactFrontendService = baseUrl("contact-frontend")
  val contactFormServiceIdentifier = "TAMC"
  val pageTitle = "Your personal tax account"
  override lazy val contactFrontendPartialBaseUrl = s"$contactFrontendService"
  override lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  override lazy val assetsPrefix = loadConfig("assets.url") + loadConfig("assets.version") + '/'

  override lazy val analyticsToken: Option[String] = runModeConfiguration.getString("google-analytics.token")
  override lazy val analyticsHost: String = runModeConfiguration.getString("google-analytics.host").getOrElse("auto")

  override lazy val isGTMEnabled: Boolean = loadConfig("google-tag-manager.enabled").toBoolean
  override lazy val gtmId: String = loadConfig("google-tag-manager.id")

  override lazy val loginUrl = loadConfig("tamc.external-urls.login-url")
  override lazy val logoutUrl = loadConfig("tamc.external-urls.logout-url")
  override lazy val logoutCallbackUrl = loadConfig("tamc.external-urls.logout-callback-url")
  override lazy val callbackUrl: String = loadConfig("tamc.external-urls.callback-url")
  override lazy val ivNotAuthorisedUrl = loadConfig("tamc.external-urls.not-authorised-url")
  override lazy val callChargeUrl: String = loadConfig("tamc.external-urls.govuk-call-charges")
  override lazy val contactIncomeTaxHelplineUrl: String = loadConfig("tamc.external-urls.contact-income-tax-helpline")
  override lazy val marriageAllowanceGuideUrl: String = loadConfig("tamc.external-urls.marriage-allowance-guide")
  override lazy val howItWorksUrl: String = loadConfig("tamc.external-urls.marriage-allowance-how-it-works")

  override lazy val ggSignInHost: String = runModeConfiguration.getString("microservice.bas-gateway.host").getOrElse("")
  override lazy val ggSignInUrl: String = s"$ggSignInHost/gg/sign-in?continue=${encodeQueryStringValue(callbackUrl)}"

  override lazy val marriageAllowanceUrl = baseUrl("marriage-allowance")
  override lazy val taiFrontendUrl: String = s"${runModeConfiguration.getString("microservice.tai-frontend.host").getOrElse("")}/check-income-tax"
  override lazy val taxFreeAllowanceUrl = s"$taiFrontendUrl/tax-free-allowance"

  lazy val enableRefresh = runModeConfiguration.getBoolean("enableRefresh").getOrElse(true)
  lazy val frontendTemplatePath: String = runModeConfiguration.getString("microservice.services.frontend-template-provider.path")
    .getOrElse("/template/mustache")

  val TAMC_BEGINNING_YEAR: Int = runModeConfiguration.getInt("tamc-earliest-valid-year")
    .getOrElse(throw new RuntimeException("Cannot find 'tamc-earliest-valid-year' in 'data/tax-rates.conf'!"))

  val TAMC_MIN_DATE: LocalDate = new LocalDate(1900,1 , 1)
  val marriedCoupleAllowanceLink = "https://www.gov.uk/married-couples-allowance"
  val generalEnquiriesLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"

  val CACHE_DIVORCE_DATE = "DIVORCE_DATE"
  val CACHE_MAKE_CHANGES_DECISION = "MAKE_CHANGES_DECISION"
  val CACHE_CHECK_CLAIM_OR_CANCEL = "CHECK_CLAIM_OR_CANCEL"
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
  val CACHE_EMAIL_ADDRESS = "EMAIL_ADDRESS"
  val CACHE_MA_ENDING_DATES = "MA_ENDING_DATES"
  val CACHE_RELATIONSHIP_RECORDS = "RELATIONSHIP_RECORDS"

  def actualTaxYear(taxYear: Int): Int = {
    if (taxYear <= 0)
      currentTaxYear.startYear
    else
      taxYear
  }

  def PERSONAL_ALLOWANCE(taxYear: Int = 0): Int = runModeConfiguration.getInt("personal-allowance-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT(taxYear: Int = 0): Int = runModeConfiguration.getInt("max-limit-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT_SCOT(taxYear: Int = 0): Int = runModeConfiguration.getInt("max-limit-scot-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT_WALES(taxYear: Int = 0): Int = runModeConfiguration.getInt("max-limit-wales-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT_NORTHERN_IRELAND(taxYear: Int = 0): Int = runModeConfiguration.getInt("max-limit-northern-ireland-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(taxYear: Int = 0): Int = runModeConfiguration.getInt("max-allowed-personal-allowance-transfer-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_BENEFIT(taxYear: Int = 0): Int = runModeConfiguration.getInt("max-benefit-" + actualTaxYear(taxYear)).getOrElse(0)

  val TRANSFEROR_ALLOWANCE: Int = PERSONAL_ALLOWANCE() - MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER()
  val RECIPIENT_ALLOWANCE: Int = PERSONAL_ALLOWANCE() + MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER()
  val TAMC_VALID_JOURNEY = "TAMC_VALID_JOURNEY"
  val SCOTTISH_RESIDENT = "scottish_resident"

  override val gdsFinishedUrl = loadConfig("tamc.external-urls.finished-gds")
  override val ptaFinishedUrl = loadConfig("tamc.external-urls.finished-pta")

  lazy val urBannerEnabled = runModeConfiguration.getString("feature.ur-banner.enabled").getOrElse("true").toBoolean

  override val LANG_CODE_ENGLISH = "en-GB"
  override val LANG_CODE_WELSH = "cy-GB"
  override val LANG_LANG_ENGLISH = "en"
  override val LANG_LANG_WELSH = "cy"

  override val isWelshEnabled = runModeConfiguration.getBoolean("microservice.services.features.welsh-translation").getOrElse(false)
  lazy val webchatId = loadConfig("webchat.id")

  val frontendHost = loadConfig("tamc-frontend.host")
  val accessibilityStatementHost: String = loadConfig("accessibility-statement.url") + "/accessibility-statement"
  override def accessibilityStatementUrl(relativeReferrerPath: String): String =
    accessibilityStatementHost + "/marriage-allowance?referrerUrl=" + SafeRedirectUrl(frontendHost + relativeReferrerPath).encodedUrl
}
