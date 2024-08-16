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

object LibraryDependencies {

  private val hmrcScaWrapperVersion                 = "1.11.0"
  private val hmrcMongoVersion      = "2.1.0"

  private val playVersion = "play-30"

  private val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% s"sca-wrapper-$playVersion"       % hmrcScaWrapperVersion,
    "uk.gov.hmrc"                  %% s"emailaddress-$playVersion"      % "4.0.0",
    "uk.gov.hmrc"                  %% s"tax-year"                       % "4.0.0",
    "uk.gov.hmrc.mongo"            %% s"hmrc-mongo-$playVersion"        % hmrcMongoVersion,
    "org.typelevel"                %% "cats-core"                       % "2.12.0"

  )

  private val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% s"sca-wrapper-test-$playVersion"  % hmrcScaWrapperVersion,
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
