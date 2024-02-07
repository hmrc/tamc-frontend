/*
 * Copyright 2024 HM Revenue & Customs
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

import sbt.*

object AppDependencies {

  private val hmrcMongoFeatureTogglesClientVersion  = "1.1.0"
  private val hmrcScaWrapperVersion                 = "1.2.0"
  private val hmrcBootstrapVersion                  = "8.2.0"

  private val playVersion = "play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% s"emailaddress-$playVersion"                  % "4.0.0",
    "uk.gov.hmrc"                  %% s"http-caching-client-$playVersion"           % "11.2.0",
    "uk.gov.hmrc"                  %% s"mongo-feature-toggles-client-$playVersion"  % hmrcMongoFeatureTogglesClientVersion,
    "uk.gov.hmrc"                  %% s"sca-wrapper-$playVersion"                   % hmrcScaWrapperVersion,
    "uk.gov.hmrc"                  %% s"tax-year"                                   % "4.0.0",
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test-$playVersion" % hmrcBootstrapVersion,
    "org.scalatestplus" %% "scalacheck-1-17"              % "3.2.18.0"
  ).map(_ % Test)

  val all: Seq[ModuleID] = compile ++ test
}
