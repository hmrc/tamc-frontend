import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, targetJvm}

val appName = "tamc-frontend"

lazy val excludedPackages = Seq(
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

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 92,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice: Project = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtGitVersioning, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    scoverageSettings,
    scalaSettings,
    defaultSettings(),
    scalaVersion := "2.13.8",
    PlayKeys.playDefaultPort := 9900,
    libraryDependencies ++= AppDependencies.all,
    Test / parallelExecution := false,
    Test / fork := false,
    retrieveManaged := true,
    resolvers ++= Seq(
      Resolver.jcenterRepo
    ),
    majorVersion := 7
  )

scalacOptions ++= Seq(
  "-feature",
  "-Xmaxerrs", "1000", // Maximum errors to print
  "-Xmaxwarns", "1000", // Maximum warnings to print
  "-Xfatal-warnings",
  // Suggested here https://github.com/playframework/twirl/issues/105#issuecomment-782985171
  "-Wconf:src=routes/.*:is,src=twirl/.*:is"
)

Compile / doc / scalacOptions ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)

TwirlKeys.templateImports ++= Seq(
  "uk.gov.hmrc.govukfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.components._",
  "uk.gov.hmrc.hmrcfrontend.views.html.helpers._"
)
