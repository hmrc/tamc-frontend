/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys
import utils.IntegrationSpec

import java.util.UUID

class ContentChecksSpec extends IntegrationSpec {

  private val baseUrl = "/marriage-allowance-application"

  case class ExpectedData(content: String)

  def getExpectedData(key: String): ExpectedData =
    key match {
      case "how-it-works"              =>
        ExpectedData(
          "Marriage Allowance automatically renews at the end of each tax year. You can cancel it, but it will not be stopped until the end of the tax year."
        )
      case "date-of-marriage"          => ExpectedData("Date of marriage or civil partnership")
      case "choose-years-to-apply-for" => ExpectedData("Choose the years you want to apply for")
      case "partners-details"          => ExpectedData("Your partner’s details")
      case "apply-by-post"             => ExpectedData("You must apply by post")
      case "confirm-your-email"        => ExpectedData("We will email confirmation of your Marriage Allowance application.")

      case "history"                 => ExpectedData("Your Marriage Allowance summary")
      case "choose"                  => ExpectedData("What do you want to do?")
      case "claims"                  => ExpectedData("Your Marriage Allowance claims")
      case "make-changes"            => ExpectedData("Why do you want to cancel your Marriage Allowance?")
      case "cancel"                  => ExpectedData("We will cancel your Marriage Allowance")
      case "divorce-enter-year"      => ExpectedData("Date of divorce, end of civil partnership or legal separation")
      case "bereavement"             => ExpectedData("We are sorry for your loss")
      case "stop-allowance"          => ExpectedData("Your partner needs to stop the Marriage Allowance claim")
      case "divorce-end-explanation" =>
        ExpectedData("You have told us you divorced, ended your civil partnership or were legally separated")
      case "confirm-email"           =>
        ExpectedData("We will email confirmation that you have cancelled your Marriage Allowance within 24 hours.")
      case "confirm-change"          => ExpectedData("You have asked us to cancel your Marriage Allowance")
      case "finished-change"         => ExpectedData("Marriage Allowance cancelled")

      case "you-cannot-use-this-service" =>
        ExpectedData(
          "Contact the Income Tax Helpline(opens in a new tab) if you need to discuss your Marriage Allowance application."
        )
      case "signed-out"                  => ExpectedData("You have been signed out")
      case key                           => throw new RuntimeException(s"Expected data are missing for `$key`")
    }

  val urls: Map[String, ExpectedData] = Map(
    "/how-it-works"                -> getExpectedData("how-it-works"),
    "/date-of-marriage"            -> getExpectedData("date-of-marriage"),
    "/choose-years-to-apply-for"   -> getExpectedData("choose-years-to-apply-for"),
    "/partners-details"            -> getExpectedData("partners-details"),
    "/apply-by-post"               -> getExpectedData("apply-by-post"),
    "/confirm-your-email"          -> getExpectedData("confirm-your-email"),
    "/history"                     -> getExpectedData("history"),
    "/choose"                      -> getExpectedData("choose"),
    "/claims"                      -> getExpectedData("claims"),
    "/make-changes"                -> getExpectedData("make-changes"),
    "/cancel"                      -> getExpectedData("cancel"),
    "/divorce-enter-year"          -> getExpectedData("divorce-enter-year"),
    "/bereavement"                 -> getExpectedData("bereavement"),
    "/stop-allowance"              -> getExpectedData("stop-allowance"),
    "/divorce-end-explanation"     -> getExpectedData("divorce-end-explanation"),
    "/confirm-email"               -> getExpectedData("confirm-email"),
    "/confirm-change"              -> getExpectedData("confirm-change"),
    "/finished-change"             -> getExpectedData("finished-change"),
    "/you-cannot-use-this-service" -> getExpectedData("you-cannot-use-this-service"),
    "/signed-out"                  -> getExpectedData("signed-out")
  )

  val uuid: String = UUID.randomUUID().toString

  def request(url: String): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, s"$baseUrl$url")
      .withSession(SessionKeys.sessionId -> uuid, SessionKeys.authToken -> "Bearer 123")

  "Content Checks" must {
    urls.foreach { case (url, expectedData) =>
      s"pass content checks at url $url" in {
        val result = route(app, request(url)).value

        status(result) mustBe OK

        val page = Jsoup.parse(contentAsString(result))
        page.text() must include(expectedData.content)
      }
    }
  }
}
