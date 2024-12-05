/*
 * Copyright 2023 HM Revenue & Customs
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

package views

import models.auth.{BaseUserRequest, UserRequest}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.{Lang, MessagesApi, MessagesImpl}
import play.api.test.{FakeRequest, Injecting}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.http.SessionKeys
import utils.BaseTest

import java.util.Locale

class MainViewSpec extends BaseTest with Injecting {

  lazy val main: Main = inject[Main]

  override implicit lazy val messages: MessagesImpl = MessagesImpl(Lang(Locale.getDefault), inject[MessagesApi])

  object CommonValues {

    val pageTitle = "Fake Page Title - Apply for - Marriage Allowance - GOV.UK"
    val pageHeader = "Apply for Marriage Allowance"
    val accountHome = "Account home"
    val messages = "Messages"
    val checkProgress = "Check progress"
    val profileAndSettings = "Profile and settings"
    val signOut = "Sign out"

    val localPersonalAccountLink = "http://localhost:9232/personal-account"
    val localMessagesLink = "http://localhost:9232/personal-account/messages"
    val localTrackProgressLink = "http://localhost:9100/track"
    val signOutUrl = "/marriage-allowance-application/logout"
    val keepAliveUrl = "/keep-alive"

    val urBannerHeader = "Help make GOV.UK better"
    val urBannerLinkText = "Sign up to take part in research (opens in new tab)"
    val urBannerLink = "https://signup.take-part-in-research.service.gov.uk/?utm_campaign=TAMCPTAbanner&utm_source=Other&utm_medium=gov.uk%20survey&t=HMRC&id=133"

    val accessibilityStatementText = "Accessibility statement"
    val accessibilityStatementBaseUrl = "http://localhost:12346/accessibility-statement/marriage-allowance?referrerUrl="

    val reportATechnicalIssueText = "Is this page not working properly? (opens in new tab)"

    val scriptUrl = "http://localhost:9234/marriage-allowance/assets/javascript/tamc.js"
    val tamcBacklinkScriptUrl = "http://localhost:9234/marriage-allowance/pta-frontend/assets/tamc-backlink.js"
    val pollyfillScriptUrl = "http://localhost:9234/marriage-allowance/pta-frontend/assets/pollyfill.js"

    val testName: String = "new style"
    val profileAndSettingsLink: String = "http://localhost:9232/personal-account/profile-and-settings"

    val accessibilityReferrerUrl: String = "%2Fsome-url"
    val reportTechnicalProblemUrl: String = "http://localhost:9250/contact/report-technical-problem?service=TAMC&referrerUrl=%2Fsome-url"

  }

  "if user is authenticated" when {

    implicit val baseUserRequest: BaseUserRequest[_] =
      UserRequest(
        FakeRequest("GET", "/some-url")
          .withSession(SessionKeys.authToken -> "Bearer 1"),
        None,
        isAuthenticated = true,
        Some(""),
        isSA = true
      )

    lazy val pageRender: HtmlFormat.Appendable = main.apply("Fake Page Title - Apply for")(Html("<p>Fake body</p>"))

    lazy val doc: Document = Jsoup.parse(pageRender.toString())

    s"using the ${CommonValues.testName} for the main" which {
      "the page header should" should {
        lazy val pageHeader = doc.select(".govuk-header__service-name")

        "contain the text 'Apply for Marriage Allowance'" in {
          pageHeader.text() shouldBe CommonValues.pageHeader
        }
      }

      "the page title" should {
        s"equal ${CommonValues.pageTitle}" in {
          doc.title() shouldBe CommonValues.pageTitle
        }

      }

      lazy val accountLinks = doc.select(".hmrc-account-menu__link")
      lazy val accountHome = accountLinks.first()
      lazy val messagesLink = accountLinks.get(3)
      lazy val checkProgressLink = accountLinks.get(4)
      lazy val profileAndSettingsLink = accountLinks.get(5)
      lazy val signOutLink = accountLinks.get(6)

      "the account link" should {
        "have the Account Menu visible" in {
          accountLinks.isEmpty shouldBe false
        }

        "have the text Account Menu" in {
          accountHome.text() shouldBe CommonValues.accountHome
        }

        "have the link to the users personal account" in {
          accountHome.attr("href") shouldBe CommonValues.localPersonalAccountLink
        }

      }

      "the messages link" should {
        "have the text Messages" in {
          messagesLink.text() shouldBe CommonValues.messages
        }

        "have the link to the users personal account" in {
          messagesLink.attr("href") shouldBe CommonValues.localMessagesLink
        }

      }

      "the check progress link" should {
        "have the text Check progress" in {
          checkProgressLink.text() shouldBe CommonValues.checkProgress
        }

        "have the link to the users personal account" in {
          checkProgressLink.attr("href") shouldBe CommonValues.localTrackProgressLink
        }

      }

      "the profile and settings link" should {
        "have the text Profile and settings" in {
          profileAndSettingsLink.text() shouldBe CommonValues.profileAndSettings
        }

        "have the link to the users personal account" in {
          profileAndSettingsLink.attr("href") shouldBe CommonValues.profileAndSettingsLink
        }

      }

      "the sign out link" should {
        "have the text Sign out" in {
          signOutLink.text() shouldBe CommonValues.signOut
        }

        "have the link to the sign out controller" in {
          signOutLink.attr("href") shouldBe CommonValues.signOutUrl
        }

      }

      "js scripts" should {
        lazy val scripts = doc.select("script").text()

        "still contain the tamc js script" in {
          scripts.contains(CommonValues.scriptUrl)
        }

        "still contains the tamc backlink scripts" in {
          scripts.contains(CommonValues.tamcBacklinkScriptUrl)
        }

        "still contains the pollyfill scripts" in {
          scripts.contains(CommonValues.pollyfillScriptUrl)
        }

      }

      "the accessibility link" should {
        lazy val accessibilityLink = doc.select("body > footer > div > div > div.govuk-footer__meta-item.govuk-footer__meta-item--grow > ul > li:nth-child(2) > a")

        "contain the text 'Accessibility statement'" in {
          accessibilityLink.text() shouldBe CommonValues.accessibilityStatementText
        }

        "contains the correct URL" in {
          val expectedUrl = CommonValues.accessibilityStatementBaseUrl + CommonValues.accessibilityReferrerUrl

          accessibilityLink.attr("href") shouldBe expectedUrl
        }

      }

      "timeout dialogue" should {
        lazy val timeoutDialogueData = doc.select("[name=\"hmrc-timeout-dialog\"]")

        "have a timeout of 900" in {
          timeoutDialogueData.attr("data-timeout") shouldBe "900"
        }

        "have a countdown of 120" in {
          timeoutDialogueData.attr("data-countdown") shouldBe "120"
        }

        "have the correct keep alive url" in {
          timeoutDialogueData.attr("data-keep-alive-url") shouldBe CommonValues.keepAliveUrl
        }

        "have the correct sign out url" in {
          timeoutDialogueData.attr("data-sign-out-url") shouldBe CommonValues.signOutUrl
        }

      }

      "the link to report a problem" should {
        lazy val isThePageNotWorkingCorrectly = doc.select(".hmrc-report-technical-issue")

        "contain the correct text" in {
          isThePageNotWorkingCorrectly.text() shouldBe CommonValues.reportATechnicalIssueText
        }

        "contains the correct link" in {
          isThePageNotWorkingCorrectly.attr("href") shouldBe CommonValues.reportTechnicalProblemUrl
        }
      }
    }

    "if user is unauthenticated" when {

      implicit val baseUserRequest: BaseUserRequest[_] =
        UserRequest(
          FakeRequest("GET", "/some-url"),
          None,
          isAuthenticated = false,
          Some(""),
          isSA = true
        )

      lazy val pageRender: HtmlFormat.Appendable = main.apply("Fake Page Title - Apply for")(Html("<p>Fake body</p>"))

      lazy val doc: Document = Jsoup.parse(pageRender.toString())

      s"using the ${CommonValues.testName} for the main" which {
        "the page header should" should {
          lazy val pageHeader = doc.select(".govuk-header__service-name")

          "contain the text 'Apply for Marriage Allowance'" in {
            pageHeader.text() shouldBe CommonValues.pageHeader
          }
        }

        "the page title" should {
          s"equal ${CommonValues.pageTitle}" in {
            doc.title() shouldBe CommonValues.pageTitle
          }
        }

        lazy val accountLinks: Elements = doc.select(".hmrc-account-menu__link")

        "the account link" should {
          "not have the Account Menu visible" in {
            accountLinks.isEmpty shouldBe true
          }
        }
      }
    }
  }
}
