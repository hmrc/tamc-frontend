
import play.core.PlayVersion
import sbt._

object AppDependencies {

  val bootstrapVersion = "7.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "emailaddress"               % "3.6.0",
    "uk.gov.hmrc"                  %% "bootstrap-frontend-play-28" % bootstrapVersion,
    "com.ibm.icu"                   % "icu4j"                      % "71.1",
    "uk.gov.hmrc"                  %% "http-caching-client"        % "9.6.0-play-28",
    "uk.gov.hmrc"                  %% "local-template-renderer"    % "2.17.0-play-28",
    "uk.gov.hmrc"                  %% "time"                       % "3.25.0",
    "uk.gov.hmrc"                  %% "domain"                     % "8.1.0-play-28",
    "uk.gov.hmrc"                  %% "url-builder"                % "3.6.0-play-28",
    "uk.gov.hmrc"                  %% "play-partials"              % "8.3.0-play-28",
    "uk.gov.hmrc"                  %% "tax-year"                   % "3.0.0",
    "uk.gov.hmrc"                  %% "play-language"              % "5.3.0-play-28",
    "uk.gov.hmrc"                  %% "govuk-template"             % "5.78.0-play-28",
    "uk.gov.hmrc"                  %% "play-ui"                    % "9.11.0-play-28",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"       % "2.13.4",
    "uk.gov.hmrc"                  %% "play-frontend-hmrc"         % "3.24.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play-test"              % PlayVersion.current,
    "org.scalatestplus"      %% "scalacheck-1-14"        % "3.2.2.0",
    "org.jsoup"               % "jsoup"                  % "1.15.3",
    "org.pegdown"             % "pegdown"                % "1.6.0",
    "org.scalacheck"         %% "scalacheck"             % "1.16.0",
    "org.mockito"             % "mockito-core"           % "4.7.0",
    "com.github.tomakehurst"  % "wiremock-jre8"          % "2.33.2",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
