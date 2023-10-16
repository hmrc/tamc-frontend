
import play.core.PlayVersion
import play.sbt.PlayImport.ehcache
import sbt._

object AppDependencies {

  private val bootstrapVersion = "7.19.0"
  private val playVersion = "play-28"
  private val hmrcMongoVersion = "1.3.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                  %% "emailaddress"                       % "3.7.0",
    "uk.gov.hmrc.mongo"            %% s"hmrc-mongo-$playVersion"           % hmrcMongoVersion,
    "uk.gov.hmrc"                  %% s"bootstrap-frontend-$playVersion"   % bootstrapVersion,
    "uk.gov.hmrc"                  %% "http-caching-client"                % s"10.0.0-$playVersion",
    "com.ibm.icu"                   % "icu4j"                              % "71.1",
    "uk.gov.hmrc"                  %% "tax-year"                           % "3.0.0",
    "com.fasterxml.jackson.module" %% "jackson-module-scala"               % "2.13.4",
    "uk.gov.hmrc"                  %% s"internal-auth-client-$playVersion" % "1.2.0",
    "uk.gov.hmrc"                  %% "sca-wrapper"                        % "1.0.43",
    ehcache
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"      %% "play-test"                     % PlayVersion.current,
    "org.scalatestplus"      %% "scalacheck-1-14"               % "3.2.2.0",
    "org.jsoup"               % "jsoup"                         % "1.15.3",
    "org.pegdown"             % "pegdown"                       % "1.6.0",
    "org.scalacheck"         %% "scalacheck"                    % "1.16.0",
    "org.mockito"             % "mockito-core"                  % "4.7.0",
    "com.github.tomakehurst"  % "wiremock-jre8"                 % "2.33.2",
    "uk.gov.hmrc"            %% s"bootstrap-test-$playVersion"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$playVersion" % hmrcMongoVersion
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
