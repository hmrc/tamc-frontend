/*
 * Copyright 2015 HM Revenue & Customs
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
  import scala.util.Properties.envOrElse

  val appName = "tamc-frontend"
  val appVersion = envOrElse("TAMC_FRONTEND_VERSION", "999-SNAPSHOT")

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

/*
 * Copyright 2015 HM Revenue & Customs
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

private object AppDependencies {
  import play.PlayImport._
  import play.core.PlayVersion

  val compile = Seq(
    "uk.gov.hmrc" %% "emailaddress" % "1.1.0",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "6.7.0",
    "uk.gov.hmrc" %% "govuk-template" % "4.0.0",
    "uk.gov.hmrc" %% "http-caching-client" % "5.6.0",
    "uk.gov.hmrc" %% "http-verbs" % "3.3.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "5.5.0",
    "uk.gov.hmrc" %% "play-breadcrumb" % "1.0.0",
    "uk.gov.hmrc" %% "play-config" % "2.0.1",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "play-partials" % "4.5.0",
    "uk.gov.hmrc" %% "play-ui" % "4.10.0",
    "uk.gov.hmrc" %% "time" % "2.1.0",
    "uk.gov.hmrc" %% "domain" % "3.3.0",
    "uk.gov.hmrc" %% "url-builder" % "1.0.0",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2",
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.8")

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "1.4.0" % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.jsoup" % "jsoup" % "1.8.3" % scope,
        "org.pegdown" % "pegdown" % "1.6.0" % scope,
        "org.scalacheck" %% "scalacheck" % "1.12.5" % scope,
        "org.scalatest" %% "scalatest" % "2.2.6" % scope)
    }.test
  }

  def apply() = compile ++ Test()
}
