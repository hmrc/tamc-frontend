import play.core.PlayVersion
import sbt._

object AppDependencies {
  val compile: Seq[ModuleID] = Seq(
   "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.9.0",
    "com.ibm.icu" % "icu4j" % "54.1.1",
    "uk.gov.hmrc" %% "http-caching-client" % "8.4.0-play-25",
    "uk.gov.hmrc" %% "play-breadcrumb" % "1.0.0",
    "uk.gov.hmrc" %% "local-template-renderer" % "2.4.0",
    "uk.gov.hmrc" %% "time" % "3.6.0",
    "uk.gov.hmrc" %% "domain" % "5.6.0-play-25",
    "uk.gov.hmrc" %% "auth-client" % "2.22.0-play-25",
    "uk.gov.hmrc" %% "url-builder" % "3.1.0",
    "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25",
    "uk.gov.hmrc" %% "tax-year" % "0.5.0",
    "uk.gov.hmrc" %% "play-language" % "3.4.0"
  )

  val test: Seq[ModuleID] = Seq(
        "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25",
        "com.typesafe.play" %% "play-test" % PlayVersion.current,
        "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1",
        "org.jsoup" % "jsoup" % "1.11.3",
        "org.pegdown" % "pegdown" % "1.6.0",
        "org.scalacheck" %% "scalacheck" % "1.14.0",
        "org.mockito" % "mockito-core" % "2.24.5",
        "com.github.tomakehurst" % "wiremock-standalone" % "2.21.0"
      ).map(_ % "test")

  val all: Seq[ModuleID] = compile ++ test
}

