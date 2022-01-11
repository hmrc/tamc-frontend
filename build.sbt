import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "tamc-frontend"

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;app.*;config.*;testOnlyDoNotUseInAppConf.*;views.*;uk.gov.hmrc.*;prod.*;forms.*;connectors.ApplicationAuditConnector;connectors.ApplicationAuthConnector;services.CachingService;metrics.Metrics;utils.WSHttp;events",
    ScoverageKeys.coverageMinimumStmtTotal := 92.48,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice: Project = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(
    scoverageSettings,
    publishingSettings,
    scalaSettings,
    defaultSettings(),
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.13",
    PlayKeys.playDefaultPort := 9900,
    libraryDependencies ++= AppDependencies.all,
    Test / parallelExecution := false,
    Test / fork := false,
    retrieveManaged := true,
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    ),
    majorVersion := 7
  )

scalacOptions ++= Seq(
  "-Xmaxerrs", "1000", // Maximum errors to print
  "-Xmaxwarns", "1000", // Maximum warnings to print
  "-Xfatal-warnings"
)
