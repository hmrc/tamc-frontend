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

  private val scalaTestPlusPlayVersion = "2.0.1"
  private val emailAddressVersion = "3.2.0"
  private val frontendBootstrapVersion = "12.4.0"
  private val httpCachingClientVersion = "8.1.0"
  private val playBreadcrumbVersion = "1.0.0"
  private val playPartialsVersion = "6.5.0"
  private val timeVersion = "3.2.0"
  private val domainVersion = "5.3.0"
  private val urlBuilderVersion = "3.1.0"
  private val hmrcTestVersion = "3.5.0-play-25"
  private val jsoupVerison = "1.11.3"
  private val pegdownVersion = "1.6.0"
  private val scalacheckVersion = "1.14.0"
  private val scalaTestVersion = "3.0.5"
  private val mockitoCoreVerison = "2.24.5"
  private val localTemplateRendererVersion = "2.3.0"
  private val taxYearVersion = "0.5.0"

  val compile = Seq(
    "uk.gov.hmrc" %% "emailaddress" % emailAddressVersion,
    "uk.gov.hmrc" %% "frontend-bootstrap" % frontendBootstrapVersion,
    "com.ibm.icu" % "icu4j" % "54.1.1",
    "uk.gov.hmrc" %% "http-caching-client" % httpCachingClientVersion,
    "uk.gov.hmrc" %% "play-breadcrumb" % playBreadcrumbVersion,
    "uk.gov.hmrc" %% "local-template-renderer" % localTemplateRendererVersion,
    "uk.gov.hmrc" %% "time" % timeVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "auth-client" % "2.19.0-play-25",
    "uk.gov.hmrc" %% "url-builder" % urlBuilderVersion,
    "uk.gov.hmrc" %% "play-partials" % playPartialsVersion,
    "uk.gov.hmrc" %% "tax-year" % taxYearVersion
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
        "org.mockito" % "mockito-core" % mockitoCoreVerison % scope,
        "com.github.tomakehurst" % "wiremock-standalone" % "2.21.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test()
}
