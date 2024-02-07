import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

val appName = "tamc-frontend"

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 7
ThisBuild / scalacOptions ++= Seq(
  "-feature",
  //"-Xfatal-warnings" FIXME switch to HMRCStandardPage template instead of deprecated HmrcLayout
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

val scoverageSettings: Seq[Def.Setting[?]] = {
  val excludedPackages = Seq(
    "<empty>",
    "app.*",
    "config.*",
    "testOnlyDoNotUseInAppConf.*",
    "views.*",
    "uk.gov.hmrc.*",
    "prod.*",
    "forms.*",
    "connectors.ApplicationAuditConnector",
    "connectors.ApplicationAuthConnector",
    "services.CachingService",
    "metrics.Metrics",
    "utils.WSHttp",
    "events"
  )
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 92,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

val microservice: Project = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtGitVersioning, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scalaSettings,
    defaultSettings(),
    scoverageSettings,
    PlayKeys.playDefaultPort := 9900,
    libraryDependencies ++= AppDependencies.all,
  )

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
