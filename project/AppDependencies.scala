import play.core.PlayVersion
import sbt._

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "emailaddress" % "3.5.0",
    "uk.gov.hmrc" %% "bootstrap-frontend-play-26" % "5.4.0",
    "com.ibm.icu" % "icu4j" % "54.1.1",
    "uk.gov.hmrc" %% "http-caching-client" % "9.5.0-play-26",
    "uk.gov.hmrc" %% "local-template-renderer" % "2.15.0-play-26",
    "uk.gov.hmrc" %% "time" % "3.25.0",
    "uk.gov.hmrc" %% "domain" % "5.11.0-play-26",
    "uk.gov.hmrc" %% "url-builder" % "3.5.0-play-26",
    "uk.gov.hmrc" %% "play-partials" % "8.1.0-play-26",
    "uk.gov.hmrc" %% "tax-year" % "1.3.0",
    "uk.gov.hmrc" %% "play-language" % "5.1.0-play-26",
    "uk.gov.hmrc" %% "govuk-template" % "5.68.0-play-26",
    "uk.gov.hmrc" %% "play-ui" % "9.6.0-play-26"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.10.0-play-26",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3",
    "org.jsoup" % "jsoup" % "1.13.1",
    "org.pegdown" % "pegdown" % "1.6.0",
    "org.scalacheck" %% "scalacheck" % "1.15.4",
    "org.mockito" % "mockito-core" % "3.11.1",
    "com.github.tomakehurst" % "wiremock-jre8" % "2.28.1"
  ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}
