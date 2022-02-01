import play.core.PlayVersion
import sbt._

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "emailaddress" % "3.5.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.12.0",
    "com.ibm.icu" % "icu4j" % "54.1.1",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-28",
    "uk.gov.hmrc" %% "local-template-renderer" % "2.15.0-play-28",
    "uk.gov.hmrc" %% "time" % "3.25.0",
    "uk.gov.hmrc" %% "domain" % "6.2.0-play-28",
    "uk.gov.hmrc" %% "url-builder" % "3.5.0-play-28",
    "uk.gov.hmrc" %% "play-partials" % "8.2.0-play-28",
    "uk.gov.hmrc" %% "tax-year" % "1.3.0",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-28",
    "uk.gov.hmrc" %% "govuk-template" % "5.70.0-play-28",
    "uk.gov.hmrc" %% "play-ui"        % "9.7.0-play-28",
    "com.typesafe.play"              %%  "play-json-joda"      % "2.9.2",
    "com.fasterxml.jackson.module"   %% "jackson-module-scala" % "2.12.5"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play-test" % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.scalatestplus"      %% "scalacheck-1-14" % "3.1.1.1",
    "org.jsoup"               % "jsoup" % "1.13.1",
    "org.pegdown"             % "pegdown" % "1.6.0",
    "org.scalacheck"         %% "scalacheck" % "1.15.4",
    "org.mockito"             % "mockito-core" % "3.11.1",
    "com.github.tomakehurst"  % "wiremock-jre8" % "2.28.1",
    "com.vladsch.flexmark"    % "flexmark-all" % "0.35.10"
  ).map(_ % "test")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.8" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.8" % Provided cross CrossVersion.full
  )

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies
}
