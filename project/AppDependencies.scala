import play.core.PlayVersion
import sbt._

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "emailaddress" % "3.4.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "3.3.0",
    "com.ibm.icu" % "icu4j" % "54.1.1",
    "uk.gov.hmrc" %% "http-caching-client" % "9.2.0-play-26",
    "uk.gov.hmrc" %% "play-breadcrumb" % "1.0.0",
    "uk.gov.hmrc" %% "local-template-renderer" % "2.10.0-play-26",
    "uk.gov.hmrc" %% "time" % "3.19.0",
    "uk.gov.hmrc" %% "domain" % "5.10.0-play-26",
    "uk.gov.hmrc" %% "auth-client" % "3.2.0-play-26",
    "uk.gov.hmrc" %% "url-builder" % "3.4.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "7.1.0-play-26",
    "uk.gov.hmrc" %% "tax-year" % "1.2.0",
    "uk.gov.hmrc" %% "play-language" % "4.7.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.61.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "8.15.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2",
    "org.jsoup" % "jsoup" % "1.11.3",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.scalacheck" %% "scalacheck" % "1.14.0",
    "org.mockito" % "mockito-core" % "2.24.5",
    "com.github.tomakehurst"  % "wiremock-jre8" % "2.26.1"

  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
