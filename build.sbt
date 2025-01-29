/*
 * Copyright 2025 HM Revenue & Customs
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

val appName = "tamc-frontend"

ThisBuild / majorVersion := 7
ThisBuild / scalaVersion := "2.13.15"
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  //"-Xfatal-warnings" FIXME switch to HMRCStandardPage template instead of deprecated HmrcLayout
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)

val root: Project = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    CodeCoverageSettings(),
    PlayKeys.playDefaultPort := 9900,
    libraryDependencies ++= LibraryDependencies(),
  )

addCommandAlias("runLocal", "run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes")
addCommandAlias("runLocalWithColours", "run -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes -Dlogger.resource=logback-colours.xml")
addCommandAlias("runAllTests", ";test;")
addCommandAlias("runAllTestsWithColours", ";set Test/javaOptions += \"-Dlogger.resource=logback-colours.xml\";runAllTests;")
addCommandAlias("runAllChecks", ";clean;scalastyle;coverageOn;runAllTests;coverageOff;coverageAggregate;")
