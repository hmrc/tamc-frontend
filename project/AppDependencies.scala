
import play.core.PlayVersion
import sbt._

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "emailaddress"               % "3.5.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % "5.12.0",
    "com.ibm.icu"                   % "icu4j"                      % "71.1",
    "uk.gov.hmrc"                  %% "http-caching-client"        % "9.6.0-play-28",
    "uk.gov.hmrc"                  %% "local-template-renderer"    % "2.17.0-play-28",
    "uk.gov.hmrc"                  %% "time"                       % "3.25.0",
    "uk.gov.hmrc"                  %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"                  %% "url-builder"                % "3.6.0-play-28",
    "uk.gov.hmrc"                  %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"                  %% "tax-year"                   % "1.3.0",
    "uk.gov.hmrc"                  %% "play-language"              % "5.3.0-play-28",
    "uk.gov.hmrc"                  %% "govuk-template"             % "5.77.0-play-28",
    "uk.gov.hmrc"                  %% "play-ui"                    % "9.10.0-play-28",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.13.3",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "3.23.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0",
    "org.scalatestplus"      %% "scalacheck-1-14"    % "3.2.2.0",
    "org.jsoup"               % "jsoup"              % "1.14.3",
    "org.pegdown"             % "pegdown"            % "1.6.0",
    "org.scalacheck"         %% "scalacheck"         % "1.16.0",
    "org.mockito"             % "mockito-core"       % "4.6.1",
    "com.github.tomakehurst"  % "wiremock-jre8"      % "2.33.2",
    "com.vladsch.flexmark"    % "flexmark-all"       % "0.62.2",
    "org.scalatest"           %% "scalatest"         % "3.2.12"
  ).map(_ % "test")

  private val silencerDependencies: Seq[ModuleID] = Seq(
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.8" cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % "1.7.8" % Provided cross CrossVersion.full
  )

  val all: Seq[ModuleID] = compile ++ test ++ silencerDependencies
}
