/*
 * Copyright 2017 HM Revenue & Customs
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

import sbt._

object FrontendBuild extends Build with MicroService {
  val appName = "tamc-frontend"
  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion

  private val scalaTestPlusPlayVersion = "1.5.1"
  private val emailAddressVersion = "1.1.0"
  private val frontendBootstrapVersion = "7.22.0"
  private val govUkTemplateVersion = "5.1.0"
  private val httpCachingClientVersion = "6.2.0"
  private val httpVerbsVersion = "6.3.0"
  private val playAuthorisedFrontendVersion = "6.3.0"
  private val playBreadcrumbVersion = "1.0.0"
  private val playConfigVersion = "4.3.0"
  private val playHealthVersion = "2.1.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val playPartialsVersion = "5.2.0"
  private val playUiVersion = "5.2.0"
  private val timeVersion = "2.1.0"
  private val domainVersion = "4.1.0"
  private val urlBuilderVersion = "2.1.0"
  private val playGraphiteVersion = "3.2.0"
  private val hmrcTestVersion = "2.3.0"
  private val jsoupVerison = "1.8.3"
  private val pegdownVersion = "1.6.0"
  private val scalacheckVersion = "1.12.5"
  private val scalaTestVersion = "2.2.6"
  private val mockitoCoreVerison = "1.9.5"

  val compile = Seq(
    "uk.gov.hmrc" %% "emailaddress" % emailAddressVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "uk.gov.hmrc" %% "govuk-template" % govUkTemplateVersion,
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "http-verbs" % httpVerbsVersion,
    "uk.gov.hmrc" %% "play-authorised-frontend" % playAuthorisedFrontendVersion,
    "uk.gov.hmrc" %% "play-breadcrumb" % playBreadcrumbVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "play-ui" % playUiVersion,
    "uk.gov.hmrc" %% "time" % timeVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "play-graphite" % playGraphiteVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = Seq.empty
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusPlayVersion % scope,
        "org.jsoup" % "jsoup" % jsoupVerison % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.scalacheck" %% "scalacheck" % scalacheckVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.mockito" % "mockito-core" % mockitoCoreVerison % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
