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

package config

import com.google.inject.Inject
import models.TaxBand
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.time.TaxYear
import utils.encodeQueryStringValue

import java.time.LocalDate

//TODO[DDCNL-3479] get rid of vals in here that aren't actually config values!!!!!
class ApplicationConfig @Inject()(configuration: Configuration, servicesConfig: ServicesConfig) {

  val authURL: String = servicesConfig.baseUrl("auth")

  def currentTaxYear(): TaxYear = TaxYear(configuration.getOptional[Int]("tamc-effective-tax-year").getOrElse(TaxYear.current.startYear))

  def currentLocalDate(): LocalDate = LocalDate.parse(configuration.getOptional[String]("tamc-effective-local-date").getOrElse(LocalDate.now.toString))

  private def loadConfig(key: String) = configuration.getOptional[String](key).getOrElse(throw new Exception(s"Missing key: $key"))

  val contactHost = configuration.getOptional[String]("tamc.external-urls.contact-frontend").getOrElse("")
  val contactFrontendService = configuration.getOptional[String]("microservice.contact-frontend.host").getOrElse("")
  val contactFormServiceIdentifier = "TAMC"
  val pageTitle = "Your personal tax account"
  lazy val betaFeedbackUnauthenticatedUrl = s"$contactHost/beta-feedback-unauthenticated?service=$contactFormServiceIdentifier"

  lazy val loginUrl: String = loadConfig("tamc.external-urls.login-url")
  lazy val logoutUrl: String = loadConfig("tamc.external-urls.logout-url")
  lazy val callbackUrl: String = loadConfig("tamc.external-urls.callback-url")
  lazy val ivNotAuthorisedUrl: String = loadConfig("tamc.external-urls.not-authorised-url")
  lazy val callChargeUrl: String = loadConfig("tamc.external-urls.govuk-call-charges")
  lazy val contactIncomeTaxHelplineUrl: String = loadConfig("tamc.external-urls.contact-income-tax-helpline")
  lazy val marriageAllowanceGuideUrl: String = loadConfig("tamc.external-urls.marriage-allowance-guide")
  lazy val howItWorksUrl: String = loadConfig("tamc.external-urls.marriage-allowance-how-it-works")
  lazy val ggSignInHost: String = configuration.getOptional[String]("microservice.bas-gateway-frontend.host").getOrElse("")
  lazy val ggSignInUrl: String = s"$ggSignInHost/bas-gateway/sign-in?continue_url=${encodeQueryStringValue(callbackUrl)}"


  lazy val marriageAllowanceUrl: String = servicesConfig.baseUrl("marriage-allowance")
  lazy val taiFrontendUrl: String = s"${configuration.getOptional[String]("microservice.tai-frontend.host").getOrElse("")}/check-income-tax"
  lazy val taxFreeAllowanceUrl = s"$taiFrontendUrl/tax-free-allowance"

  val DEFAULT_VALID_YEARS = 4
  val TAMC_VALID_YEARS: Int = configuration.getOptional[Int]("tamc-valid-years-prior")
    .getOrElse(DEFAULT_VALID_YEARS)

  val TAMC_MIN_DATE: LocalDate = LocalDate.of(1900, 1, 1)
  val marriedCoupleAllowanceLink = "https://www.gov.uk/married-couples-allowance"
  val generalEnquiriesLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/income-tax-enquiries-for-individuals-pensioners-and-employees"

  def actualTaxYear(taxYear: Int): Int = {
    if (taxYear <= 0)
      currentTaxYear().startYear
    else
      taxYear
  }

  def PERSONAL_ALLOWANCE(taxYear: Int = 0): Int = configuration.getOptional[Int]("personal-allowance-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT(taxYear: Int = 0): Int = configuration.getOptional[Int]("max-limit-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT_SCOT(taxYear: Int = 0): Int = configuration.getOptional[Int]("max-limit-scot-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT_WALES(taxYear: Int = 0): Int = configuration.getOptional[Int]("max-limit-wales-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_LIMIT_NORTHERN_IRELAND(taxYear: Int = 0): Int = configuration.getOptional[Int]("max-limit-northern-ireland-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER(taxYear: Int = 0): Int = configuration.getOptional[Int]("max-allowed-personal-allowance-transfer-" + actualTaxYear(taxYear)).getOrElse(0)
  def MAX_BENEFIT(taxYear: Int = 0): Int = configuration.getOptional[Int]("max-benefit-" + actualTaxYear(taxYear)).getOrElse(0)

  def STARTER_RATE_LOWER_TRESHOLD_SCOT(taxYear: Int = 0): Int = configuration.getOptional[Int]("taxbands-starter-rate-lowerthreshold-scotland-" + actualTaxYear(taxYear)).getOrElse(0)

  val TRANSFEROR_ALLOWANCE: Int = PERSONAL_ALLOWANCE() - MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER()
  val RECIPIENT_ALLOWANCE: Int = PERSONAL_ALLOWANCE() + MAX_ALLOWED_PERSONAL_ALLOWANCE_TRANSFER()
  val TAMC_VALID_JOURNEY = "TAMC_VALID_JOURNEY"
  val SCOTTISH_RESIDENT = "scottish_resident"

  val gdsStartUrl: String = loadConfig("tamc.external-urls.start-gds")
  val gdsContinueUrl: String = loadConfig("tamc.external-urls.continue-gds")
  val gdsFinishedUrl: String = loadConfig("tamc.external-urls.finished-gds")
  val ptaFinishedUrl: String = loadConfig("tamc.external-urls.finished-pta")

  val LANG_LANG_WELSH = "cy"

  lazy val webchatId: String = loadConfig("webchat.id")

  val frontendHost: String = loadConfig("tamc-frontend.host")
  val accessibilityStatementHost: String = loadConfig("accessibility-statement.service-path") + "/accessibility-statement"
  def accessibilityStatementUrl(relativeReferrerPath: String): String =
    accessibilityStatementHost + "/marriage-allowance?referrerUrl=" + encodeQueryStringValue(frontendHost + relativeReferrerPath)

  val applyMarriageAllowanceUrl: String = loadConfig("tamc.external-urls.apply-marriage-allowance")

  private def createUrl(action: String) =
    s"$loginUrl/$action?origin=ma&confidenceLevel=200&completionURL=${encodeQueryStringValue(callbackUrl)}&failureURL=${encodeQueryStringValue(ivNotAuthorisedUrl)}"

  def ivUpliftUrl: String = createUrl(action = "uplift")

  def getTaxBand(name: String, taxRate: String, country: String, taxYear: Int): TaxBand = {
    val lowerThreshold = configuration.getOptional[Int](taxBandKey(taxRate, "lowerthreshold", country, taxYear)).getOrElse(0)
    val upperThreshold = configuration.getOptional[Int](taxBandKey(taxRate, "upperthreshold", country, taxYear)).getOrElse(0)
    val rate = configuration.getOptional[Double](taxBandKey(taxRate, "rate", country, taxYear)).getOrElse(0d)
    TaxBand(name, lowerThreshold, upperThreshold, rate)
  }

  private def taxBandKey(taxRate: String, taxBand: String, country: String, taxYear: Int) = {
    s"taxbands-$taxRate-$taxBand-$country-$taxYear"
  }

  val pertaxAuthBaseUrl = servicesConfig.baseUrl("pertax-auth")


}
