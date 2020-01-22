import sbt.Tests.{Group, SubProcess}
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings, targetJvm}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

  val appName = "tamc-frontend"
  lazy val scoverageSettings = {

    Seq(
      ScoverageKeys.coverageExcludedPackages := "<empty>;app.*;config.*;testOnlyDoNotUseInAppConf.*;views.*;uk.gov.hmrc.*;prod.*;forms.*;connectors.ApplicationAuditConnector;connectors.ApplicationAuthConnector;services.CachingService;metrics.Metrics;utils.WSHttp;events",
      ScoverageKeys.coverageMinimum := 80,
      ScoverageKeys.coverageFailOnMinimum := true,
      ScoverageKeys.coverageHighlighting := true,
      ScoverageKeys.coverageEnabled := true
    )
  }

  lazy val microservice: Project = Project(appName, file("."))
    .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
    .settings(scoverageSettings,
      publishingSettings,
      scalaSettings,
      defaultSettings(),
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.11",
      PlayKeys.playDefaultPort := 9900,
      libraryDependencies ++= AppDependencies.all,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      routesGenerator := StaticRoutesGenerator,
      resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo,
      "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
    ),
    majorVersion := 7)




